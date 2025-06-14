package lila.ublog

import lila.core.id.UblogPostId
import lila.search.*
import lila.search.client.SearchClient
import lila.search.spec.{ Query, SortBlogsBy }
import UblogAutomod.Quality

final class UblogSearch(client: SearchClient, config: UblogConfig)(using Executor)
    extends SearchReadApi[UblogPostId, Query.Ublog]:
  lazy val builder = PaginatorBuilder(this, config.searchPageSize)

  def fetchResults(text: String, by: lila.core.ublog.BlogsBy, minQualityOpt: Option[Quality], page: Int) =
    val sortBy =
      SortBlogsBy.values.find(_.toString == by.toString).getOrElse(SortBlogsBy.Score)
    builder(Query.Ublog(text, sortBy, minQualityOpt.map(_.ordinal), none), page)

  def search(query: Query.Ublog, from: From, size: Size): Fu[List[UblogPostId]] =
    client
      .search(query, from, size)
      .map(res => res.hitIds.map(id => UblogPostId.apply(id.value)))

  def count(query: Query.Ublog) =
    client.count(query).dmap(_.count)
