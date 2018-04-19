## Elasticsearch REST Client compatiblity tests for Graylog

This tool exists to make sure that [Graylog's](https://github.com/Graylog2/graylog2-server) elasticsearch API usage is compatible across versions of nodes and client libraries.

The high level REST client is officially not backwards compatible, because later versions might change fields and/or URLs in a way that an earlier server does not accept or send.
In order to avoid having to use multiple client library versions, this tool performs all types of requests that Graylog does in a more concise way against a list of elasticsearch servers.

If any of those tests fail for a certain version we can quickly see what broken and hopefully why, too, while using the latest possible version of the high level client library.

## Building

The project uses Maven 3, the project can be build by standard means:

```
mvn clean compile package
```

## Running

After the project has been built and packaged, the jar file in `target/esclient-test.jar` is directly executable, as long as its `dependency-jars` directory is available.

Simply run the jar directly:

```
java -jar target/esclient-test.jar help
```

This will print:

```
usage: esclient-test <command> [ <args> ]

Commands are:
    help   Display help information
    run    Runs the elasticsearch requests

See 'esclient-test help <command>' for more information on a specific command.
```


To run the entire test suite against one or more Elasticsearch servers, use the run command:

```
$ java -jar target/esclient-test.jar help run
NAME
        esclient-test run - Runs the elasticsearch requests

SYNOPSIS
        esclient-test run {-h | --host} <elasticsearch host>...
                [ {-i | --index} <index name> ]

OPTIONS
        -h <elasticsearch host>, --host <elasticsearch host>


        -i <index name>, --index <index name>
            The index name to use in requests, picks a random name by default

```