package lila
package team

import search.ElasticSearch
import SearchMapping.fields

import org.elasticsearch.index.query._, QueryBuilders._
import org.elasticsearch.search.sort._, SortBuilders._
import scalastic.elasticsearch.SearchParameterTypes.FieldSort
import org.joda.time.DateTime
import org.scala_tools.time.Imports._

private[team] final class SearchQuery private(text: String) {

  def nonEmpty = text.nonEmpty

  def searchRequest(from: Int = 0, size: Int = 10) = ElasticSearch.Request.Search(
    query = makeQuery,
    from = from,
    size = size)

  def countRequest = ElasticSearch.Request.Count(makeQuery)

  private def makeQuery = {
    import SearchMapping.fields._
    boolQuery
    .should(fuzzyQuery(name, text))
    .should(fuzzyQuery(description, text))
    .should(fuzzyQuery(location, text))
  }
}

object SearchQuery {

  def apply(text: String): SearchQuery = new SearchQuery(text.trim.toLowerCase)
}
