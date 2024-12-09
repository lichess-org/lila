package lila.teamSearch

import com.softwaremill.macwire.*
import scalalib.paginator.Paginator

import lila.search.client.SearchClient
import lila.search.spec.Query

final class Env(client: SearchClient)(using Executor):

  private val maxPerPage = MaxPerPage(15)

  private lazy val paginatorBuilder = lila.search.PaginatorBuilder(api, maxPerPage)

  lazy val api: TeamSearchApi = wire[TeamSearchApi]

  def apply(text: String, page: Int): Fu[Paginator[TeamId]] = paginatorBuilder(Query.team(text), page)
