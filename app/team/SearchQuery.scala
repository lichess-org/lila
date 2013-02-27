package lila
package team

import search.ElasticSearch
import SearchMapping.fields

import org.elasticsearch.index.query._, QueryBuilders._

private[team] final class SearchQuery private (terms: List[String]) {

  def searchRequest(from: Int = 0, size: Int = 10) = ElasticSearch.Request.Search(
    query = makeQuery,
    from = from,
    size = size)

  def countRequest = ElasticSearch.Request.Count(makeQuery)

  private def makeQuery = terms.foldLeft(boolQuery()) {
    case (query, term) ⇒ query must {
      multiMatchQuery(term, fields.name, fields.description, fields.location)
    }
  }
}

object SearchQuery {

  def apply(text: String): SearchQuery = new SearchQuery(
    ElasticSearch.Request decomposeTextQuery text
  )
}
