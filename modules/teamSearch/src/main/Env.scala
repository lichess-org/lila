package lila.teamSearch

import akka.actor.*
import com.softwaremill.macwire.*
import lila.common.autoconfig.*
import play.api.Configuration

import lila.common.config.*
import lila.search.*
import lila.team.Team
import lila.common.paginator.Paginator

@Module
private class TeamSearchConfig(
    @ConfigName("index") val indexName: String,
    @ConfigName("actor.name") val actorName: String
)

final class Env(
    appConfig: Configuration,
    makeClient: Index => ESClient,
    teamRepo: lila.team.TeamRepo
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

  def apply(text: String, page: Int): Fu[Paginator[Team]] = paginatorBuilder(Query(text), page)

  def cli: lila.common.Cli = new:
    def process = { case "team" :: "search" :: "reset" :: Nil =>
      api.reset inject "done"
    }

  system.actorOf(
    Props(new Actor:
      import lila.team.{ InsertTeam, RemoveTeam }
      def receive =
        case InsertTeam(team) => api.store(team)
        case RemoveTeam(id)   => client.deleteById(id into Id)
    ),
    name = config.actorName
  )
