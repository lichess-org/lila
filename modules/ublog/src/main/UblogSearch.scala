package lila.ublog

import lila.core.id.UblogPostId
import lila.core.ublog.{ BlogsBy, Quality }
import lila.search.*
import lila.search.client.SearchClient
import lila.search.spec.{ Query, SortBlogsBy }

final class UblogSearch(client: SearchClient, config: UblogConfig)(using Executor)
    extends SearchReadApi[UblogPostId, Query.Ublog]:

  lazy val builder = PaginatorBuilder(this, config.searchPageSize)

  def fetchResults(text: String, by: BlogsBy, minQualityOpt: Option[Quality], page: Int) =
    builder(Query.Ublog(text, toSearchSort(by), minQualityOpt.map(_.ordinal), none), page)

  private def toSearchSort(by: BlogsBy): SortBlogsBy = by match
    case BlogsBy.newest => SortBlogsBy.newest
    case BlogsBy.oldest => SortBlogsBy.oldest
    case BlogsBy.score  => SortBlogsBy.score
    case BlogsBy.likes  => SortBlogsBy.likes

  def search(query: Query.Ublog, from: From, size: Size): Fu[List[UblogPostId]] =
    client
      .search(query, from, size)
      .map(res => res.hitIds.map(id => UblogPostId.apply(id.value)))

  def count(query: Query.Ublog) =
    client.count(query).dmap(_.count)
