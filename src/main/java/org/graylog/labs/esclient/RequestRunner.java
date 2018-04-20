package org.graylog.labs.esclient;

import static java.util.Collections.emptyMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Required;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;
import com.google.common.net.HostAndPort;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.Version;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog2.indexer.IndexMapping5;

@Command(name = "run", description = "Runs the elasticsearch requests")
public class RequestRunner implements Runnable {

  @Option(name = {"-i", "--index"},
      title = "index name",
      description = "The index name to use in requests, picks a random name by default")
  private String indexName;

  @Required
  @Option(name = {"-h", "--host"}, title = "elasticsearch host")
  private List<HostAndPort> hosts;

  @Option(name = {"-k", "--keep"}, title = "do not run cleanup tasks")
  private boolean skipCleanup = false;

  private RestHighLevelClient high;
  private RestClient low;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private List<Runnable> cleanupTasks = Lists.newArrayList();

  public void run() {
    System.out.println("Running elasticsearch client " + Version.CURRENT);
    if (Strings.isNullOrEmpty(indexName)) {
      indexName = RandomStringUtils.random(16, true, true);
    }
    indexName = indexName.toLowerCase(Locale.ENGLISH);
    System.out.println("Using randomized index name: " + indexName);

    hosts.forEach(hostAndPort -> runOnHost(hostAndPort.withDefaultPort(9200)));
  }

  private void runOnHost(HostAndPort address) {
    System.out.println("Running against elasticsearch on " + address);
    high = new RestHighLevelClient(
        RestClient.builder(new HttpHost(address.getHost(), address.getPort()))
    );
    low = high.getLowLevelClient();

    try {
      printClusterInfo();
      prepareIndex();
      bulkIndex();
      performSearches();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (!skipCleanup) {
          cleanupTasks.forEach(Runnable::run);
        }
        high.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      System.out.println("\nDone with host " + address +".\n");
    }
  }

  private void printClusterInfo() throws IOException {
    final MainResponse info = high.info();
    System.out.println("\nConnected to Elasticsearch cluster:");
    System.out.println("     Name: " + info.getClusterName().value());
    System.out.println("     UUID: " + info.getClusterUuid());
    System.out.println("     Node: " + info.getNodeName());
    System.out.println("  Version: " + info.getVersion());
    System.out.println("    Build: " + info.getBuild());
  }

  private void prepareIndex() throws IOException {
    final Map<String, Object> messageTemplate = new IndexMapping5()
        .messageTemplate(indexName + "_*", "standard");

    HttpEntity entity = entity(messageTemplate);
    final Response templateCreateResponse =
        low.performRequest(
            "PUT",
            "_template/" + indexName + "_template",
            emptyMap(),
            entity);
    if (templateCreateResponse.getStatusLine().getStatusCode() != 200) {
      throw new IllegalStateException("Could not create template: " + templateCreateResponse.getStatusLine().getReasonPhrase());
    }
    cleanupTasks.add(() -> {
      try {
        low.performRequest("DELETE", "_template/" + indexName + "_template");
      } catch (IOException e) {
        e.printStackTrace();
      }
    });

    final Settings settings = Settings.builder()
        .put("index.number_of_shards", 2)
        .put("index.number_of_replicas", 0)
        .build();
    final CreateIndexResponse createIndexResponse = high.indices()
        .create(new CreateIndexRequest(indexName + "_0", settings));
    if (!createIndexResponse.isShardsAcknowledged()) {
      throw new IllegalStateException("Create index failed");
    }
    cleanupTasks.add(() -> {
      try {
        high.indices().delete(new DeleteIndexRequest(indexName + "_0"));
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }

  private void bulkIndex() throws IOException {
    final URL firstBatch = Resources.getResource("bulk/batch_1.txt");
    final BulkRequest br = Resources.readLines(firstBatch, StandardCharsets.UTF_8, new LineProcessor<BulkRequest>() {
      private final BulkRequest br = new BulkRequest();
      @Override
      public boolean processLine(@Nonnull String line) {
        // skip comments and empty lines
        if (line.isEmpty() || line.trim().startsWith("#")) {
          return true;
        }
        br.add(new IndexRequest(indexName + "_0", "message").source(line, XContentType.JSON));
        return true;
      }

      @Override
      public BulkRequest getResult() {
        return br;
      }
    });
    if (br.numberOfActions() > 0) {
      high.bulk(br);
    } else {
      System.err.println("Bulk index request contains no actions, is this correct?");
    }
  }

  private void performSearches() throws IOException {
    final SearchSourceBuilder timestampExists = new SearchSourceBuilder()
        .query(QueryBuilders.existsQuery("timestamp"));

    final MultiSearchRequest msr = new MultiSearchRequest().add(
        new SearchRequest()
            .indices(indexName)
            .source(timestampExists)
    );
    final MultiSearchResponse multiSearchResponse = high.multiSearch(msr);
    multiSearchResponse.getResponses();
  }

  private NStringEntity entity(Object payload) {
    try {
      return new NStringEntity(objectMapper.writeValueAsString(payload), ContentType.APPLICATION_JSON);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return null;
  }
}
