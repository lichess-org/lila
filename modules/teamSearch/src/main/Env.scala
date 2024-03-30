package lila.teamSearch

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.autoconfig.*
import lila.common.config.*
import lila.common.paginator.Paginator
import lila.search.*

@Module
private class TeamSearchConfig(
    @ConfigName("index") val indexName: String,
    @ConfigName("actor.name") val actorName: String
)

final class Env(
    appConfig: Configuration,
    makeClient: Index => ESClient,
    teamApi: lila.hub.team.TeamApi
)(using
    ec: Executor,
    system: ActorSystem,
    materializer: akka.stream.Materializer
):

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

  system.actorOf(
    Props(new Actor:
      import lila.hub.team.{ InsertTeam, RemoveTeam }
      def receive =
        case InsertTeam(team) => api.store(team)
        case RemoveTeam(id)   => client.deleteById(id.into(Id))
    ),
    name = config.actorName
  )
