## ES API usage

 * Scrolling: org.graylog2.indexer.results.ScrollResult
   * SearchScroll
   * ClearScroll
 * Search
   * index ranges: aggs + filter, indexRangeStatsOfIndex
   * MultiSearch: always one search to work around URL limit
 * Index management: org.graylog2.indexer.indices.Indices
   * Settings
     * GetSettings: index + alias
     * UpdateSettings
   * Index
     * CreateIndex: settings
     * CloseIndex
     * DeleteIndex
     * ForceMerge
     * Flush
     * OpenIndex
   * Aliases
     * GetAliases
     * ModifyAliases (Add, Remove)
   * Cat APIs
     * CatIndices
   * Templates
     * PutTemplate
     * DeleteTemplate
   * Stats
     * Index stats: "level", "shards"
 * Node
   * Ping
 * Cluster
   * Cluster State
     * State: Index mappings via /\_cluster/state
   * Health
     * Health waitForStatus
 * Document
   * Get
 * Indexing
   * Bulk
 * Misc
   * Analyze
   * NodesInfo: via jest nodechecker/autodiscovery
