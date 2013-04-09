package lila.search

trait Query {

  def searchRequest(from: Int = 0, size: Int = 10): ElasticSearch.Request.Search

  def countRequest: ElasticSearch.Request.Count
}
