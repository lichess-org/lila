package lila
package team

import search.ElasticSearch
import SearchMapping.fields

import org.elasticsearch.index.query._, FilterBuilders._, QueryBuilders._
import org.elasticsearch.search.sort._, SortBuilders._
import scalastic.elasticsearch.SearchParameterTypes.FieldSort
import org.joda.time.DateTime
import org.scala_tools.time.Imports._

case class SearchQuery(text: String) {

  def nonEmpty = text.nonEmpty

  def searchRequest(from: Int = 0, size: Int = 10) = ElasticSearch.Request.Search(
    query = matchAllQuery,
    filter = filters,
    sortings = FieldSort(SearchMapping.fields.nbMembers, order = SortOrder.DESC) :: Nil,
    from = from,
    size = size)

  def countRequest = ElasticSearch.Request.Count(matchAllQuery, filters)

  private def filters = None
}
