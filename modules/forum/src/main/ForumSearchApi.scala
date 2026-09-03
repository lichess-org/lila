package lila.forum

import play.api.libs.json.*

import lila.core.id.ForumPostId
import lila.search.{ SearchClient, SearchApi, PaginatorBuilder }

final class ForumSearchApi(elastic: SearchClient, config: ForumConfig)(using Executor)
    extends SearchApi[ForumPostId, ForumSearchApi.Query]:

  import ForumSearchApi.*
  import SearchClient.*

  def apply(text: String, page: Int, troll: Boolean) =
    PaginatorBuilder(this, config.searchMaxPerPage)(ForumSearchApi.Query(text.take(100), troll), page)

  def search(query: Query, offset: Long, length: Long) =
    elastic
      .searchIds(Index.Forum, makeQuery(query), makeSort, offset, length, query)
      .map(_.map(ForumPostId.apply))

  def count(query: Query) =
    elastic.count(Index.Forum, makeQuery(query), query)

  private def makeQuery(query: Query): JsObject =
    val parsed = parse(query.text, List("user"))
    compileFilter(
      parsed.terms.map(multiMatch(_, searchableFields)) ++
        parsed("user").map(term(Fields.author, _)).toList ++
        Option.unless(query.troll)(term(Fields.troll, false)).toList
    )

  private def makeSort: JsArray =
    Json.arr(fieldSort(Fields.date, "desc"))

object ForumSearchApi:
  // see file://./../../../../bin/elastic/forum.ts

  case class Query(text: String, troll: Boolean)

  private val searchableFields = List(Fields.body, Fields.topic, Fields.author)

  private object Fields:
    val body = "bo"
    val topic = "to"
    val author = "au"
    val troll = "tr"
    val date = "da"
