package lila.team

import play.api.libs.json.*

import lila.search.{ SearchClient, SearchApi, PaginatorBuilder }

final class TeamSearchApi(elastic: SearchClient)(using Executor)
    extends SearchApi[TeamId, TeamSearchApi.Query]:

  import TeamSearchApi.*
  import SearchClient.*

  private lazy val paginatorBuilder = lila.search.PaginatorBuilder(this, maxPerPage)

  def apply(text: String, page: Int) =
    paginatorBuilder(Query(text), page)

  def search(query: Query, offset: Long, length: Long) =
    elastic
      .searchIds(Index.Team, makeQuery(query), makeSort, offset, length, query)
      .map(_.map(TeamId.apply))

  def count(query: Query) = elastic.count(Index.Team, makeQuery(query), query)

  private def makeQuery(query: Query): JsObject =
    compileFilter(parse(query.text, Nil).terms.map(multiMatch(_, searchableFields)))

  private def makeSort: JsArray =
    Json.arr(fieldSort(Fields.nbMembers, "desc"))

object TeamSearchApi:
  // see file://./../../../../bin/elastic/team.ts

  case class Query(text: String)

  private val maxPerPage = MaxPerPage(15)
  private val searchableFields = List(Fields.name, Fields.description)

  private object Fields:
    val name = "na"
    val description = "de"
    val nbMembers = "nbm"
