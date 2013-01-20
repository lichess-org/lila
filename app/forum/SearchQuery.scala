package lila
package forum

import search.ElasticSearch
import SearchMapping.fields

import org.elasticsearch.index.query._, QueryBuilders._

private[forum] final class SearchQuery private(text: String) {

  def searchRequest(from: Int = 0, size: Int = 10) = ElasticSearch.Request.Search(
    query = makeQuery,
    from = from,
    size = size)

  def countRequest = ElasticSearch.Request.Count(makeQuery)

  private def makeQuery = {
    import SearchMapping.fields._
    boolQuery
    .should(fuzzyQuery(body, text))
    .should(fuzzyQuery(topic, text))
    .should(fuzzyQuery(author, text))
  }
}

object SearchQuery {

  def apply(text: String): SearchQuery = new SearchQuery(text.trim.toLowerCase)
}
