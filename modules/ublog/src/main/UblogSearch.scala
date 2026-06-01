package lila.ublog

import play.api.libs.json.*

import lila.core.id.UblogPostId
import lila.core.ublog.{ BlogsBy, Quality }
import lila.search.{ SearchClient, SearchApi, SearchChunk, SearchCursor }
import lila.search.SearchClient.*

final class UblogSearch(elastic: SearchClient, config: UblogConfig)(using Executor)
    extends SearchApi[UblogPostId, UblogSearch.Query]:

  import UblogSearch.*

  def fetchByCursor[A](
      text: String,
      by: BlogsBy,
      minQualityOpt: Option[Quality],
      cursor: Option[Long]
  )(filterFetch: Seq[UblogPostId] => Fu[Map[UblogPostId, A]]): Fu[SearchChunk[A]] =
    val query = Query(text, by, minQualityOpt.map(_.ordinal), none)
    SearchCursor(
      countUnfiltered = count(query),
      search = (offset, length) => search(query, offset, length),
      maxPerPage = config.searchPageSize
    )(cursor, filterFetch)

  def search(query: Query, offset: Long, length: Long): Fu[List[UblogPostId]] =
    elastic
      .searchIds(Index.Ublog, makeQuery(query), makeSort(query.by), offset, length, query)
      .map(_.map(UblogPostId.apply))

  def count(query: Query): Fu[Long] =
    elastic.count(Index.Ublog, makeQuery(query), query)

object UblogSearch:
  // see file://./../../../../bin/elastic/ublog.ts

  case class Query(text: String, by: BlogsBy, minQuality: Option[Int], language: Option[String])

  private def makeQuery(query: Query): JsObject =
    bool(
      must = List(queryString(sanitize(query.text), "text")),
      filter = List(
        query.minQuality.map(rangeGte("quality", _)),
        query.language.map(term("language", _))
      ).flatten
    )

  private def makeSort(by: BlogsBy): JsArray =
    JsArray(
      (by match
        case BlogsBy.score => List(fieldSort("_score", "desc"))
        case BlogsBy.likes => List(fieldSort("likes", "desc"))
        case _ => Nil
      ) ++ List(
        fieldSort("quality", "desc", Some("_last")),
        fieldSort("date", if by == BlogsBy.oldest then "asc" else "desc", Some("_last"))
      )
    )

  private def sanitize(text: String): String =
    sanitizeQueryString(text, Set("language:[a-z]{2}", "quality:[1-3]"))
