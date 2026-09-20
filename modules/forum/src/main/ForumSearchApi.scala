package lila.forum

import lila.core.id.ForumPostId
import lila.search.{ SearchClient, SearchApi, PaginatorBuilder }

final class ForumSearchApi(client: SearchClient, config: ForumConfig)(using Executor)
    extends SearchApi[ForumPostId, ForumSearchApi.Query]:

  import ForumSearchApi.*
  import SearchClient.*

  def apply(text: String, page: Int, troll: Boolean) =
    PaginatorBuilder(this, config.searchMaxPerPage)(ForumSearchApi.Query(text.take(100), troll), page)

  def search(query: Query, offset: Long, length: Long) =
    client
      .searchIds(Index.Forum, makeQuery(query), makeSort, offset, length, query)
      .map(_.map(ForumPostId.apply))

  def count(query: Query) =
    client.count(Index.Forum, makeQuery(query), query)

  private def makeQuery(query: Query): SearchQuery =
    val parsed = parse(query.text, List("user"))
    compileFilter(
      parsed.terms.map(multiMatch(_, searchableFields)) ++
        parsed("user").map(term(Fields.author, _)).toList ++
        Option.unless(query.troll)(term(Fields.troll, false)).toList
    )

  private def makeSort: List[SearchSort] = List(fieldSort(Fields.date, "desc"))

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
