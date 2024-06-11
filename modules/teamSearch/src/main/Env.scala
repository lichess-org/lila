package lila.teamSearch

import akka.actor.*
import com.softwaremill.macwire.*

import scalalib.paginator.Paginator
import lila.core.config.ConfigName
import lila.search.client.SearchClient
import lila.search.spec.Query

final class Env(
    client: SearchClient,
    teamApi: lila.core.team.TeamApi
)(using Executor, akka.stream.Materializer):

  private val maxPerPage = MaxPerPage(15)

  private lazy val paginatorBuilder = lila.search.PaginatorBuilder(api, maxPerPage)

  lazy val api: TeamSearchApi = wire[TeamSearchApi]

  def apply(text: String, page: Int): Fu[Paginator[TeamId]] = paginatorBuilder(Query.team(text), page)

  def cli: lila.common.Cli = new:
    def process = { case "team" :: "search" :: "reset" :: Nil =>
      api.reset.inject("done")
    }

  lila.common.Bus.subscribeFun("team"):
    case lila.core.team.TeamCreate(team)    => api.store(team)
    case lila.core.team.TeamUpdate(team, _) => api.store(team)
    case lila.core.team.TeamDelete(id)      => client.deleteById(index, id.value)
    case lila.core.team.TeamDisable(id)     => client.deleteById(index, id.value)
