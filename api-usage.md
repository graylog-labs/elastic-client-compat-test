## ES API usage

 * Scrolling: org.graylog2.indexer.results.ScrollResult
   * SearchScroll
   * ClearScroll
 * Search
   * index ranges: aggs + filter, indexRangeStatsOfIndex
   * MultiSearch
 * Index management: org.graylog2.indexer.indices.Indices
   * Settings
     * GetSettings: index + alias
     * UpdateSettings
   * Index
     * CreateIndex: settings (done, high)
     * CloseIndex
     * DeleteIndex (done, high)
     * ForceMerge
     * Flush
     * OpenIndex
   * Aliases
     * GetAliases
     * ModifyAliases (Add, Remove)
   * Cat APIs
     * CatIndices
   * Templates
     * PutTemplate (done, low)
     * DeleteTemplate (done, low)
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
   * Bulk (done, high)
 * Misc
   * Analyze
   * NodesInfo: via jest nodechecker/autodiscovery
