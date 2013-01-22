package lila
package forum

import search.ElasticSearch
import SearchMapping.fields

import org.elasticsearch.index.query._, QueryBuilders._, FilterBuilders._

private[forum] final class SearchQuery private(text: String, staff: Boolean) {

  def searchRequest(from: Int = 0, size: Int = 10) = ElasticSearch.Request.Search(
    query = makeQuery,
    filter = makeFilters,
    from = from,
    size = size)

  def countRequest = ElasticSearch.Request.Count(makeQuery)

  private def makeQueryFuzzy = {
    boolQuery
    .should(fuzzyQuery(fields.body, text))
    .should(fuzzyQuery(fields.topic, text))
    .should(fuzzyQuery(fields.author, text))
  }

  private def makeQuery = multiMatchQuery(text, fields.body, fields.topic, fields.author)

  private def makeFilters = !staff option termFilter(fields.staff, false)

}

object SearchQuery {

  def apply(text: String, staff: Boolean): SearchQuery = 
    new SearchQuery(text.trim.toLowerCase, staff)
}
