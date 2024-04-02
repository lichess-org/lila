package lila.teamSearch

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.autoconfig.*
import lila.common.config.*
import scalalib.paginator.Paginator
import lila.search.*
import lila.core.config.ConfigName

@Module
private class TeamSearchConfig(
    @ConfigName("index") val indexName: String
)

final class Env(
    appConfig: Configuration,
    makeClient: Index => ESClient,
    teamApi: lila.core.team.TeamApi
)(using Executor, akka.stream.Materializer):

  private val config = appConfig.get[TeamSearchConfig]("teamSearch")(AutoConfig.loader)

  private val maxPerPage = MaxPerPage(15)

  private lazy val client = makeClient(Index(config.indexName))

  private lazy val paginatorBuilder = lila.search.PaginatorBuilder(api, maxPerPage)

  lazy val api: TeamSearchApi = wire[TeamSearchApi]

  def apply(text: String, page: Int): Fu[Paginator[TeamId]] = paginatorBuilder(Query(text), page)

  def cli: lila.common.Cli = new:
    def process = { case "team" :: "search" :: "reset" :: Nil =>
      api.reset.inject("done")
    }

  lila.common.Bus.subscribeFun("team"):
    case lila.core.team.TeamCreate(team) => api.store(team)
    case lila.core.team.TeamUpdate(team) => api.store(team)
    case lila.core.team.TeamDelete(id)   => client.deleteById(id.into(Id))
    case lila.core.team.TeamDisable(id)  => client.deleteById(id.into(Id))
