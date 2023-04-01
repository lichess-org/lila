package lila.teamSearch

import akka.actor.*
import com.softwaremill.macwire.*
import lila.common.autoconfig.*
import play.api.Configuration

import lila.common.config.*
import lila.search.*

@Module
private class TeamSearchConfig(
    @ConfigName("index") val indexName: String,
    @ConfigName("actor.name") val actorName: String
)

@annotation.nowarn("msg=unused")
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

  private lazy val paginatorBuilder = new lila.search.PaginatorBuilder(api, maxPerPage)

  lazy val api: TeamSearchApi = wire[TeamSearchApi]

  def apply(text: String, page: Int) = paginatorBuilder(Query(text), page)

  def cli =
    new lila.common.Cli:
      def process = { case "team" :: "search" :: "reset" :: Nil =>
        api.reset inject "done"
      }

  system.actorOf(
    Props(new Actor {
      import lila.team.{ InsertTeam, RemoveTeam }
      def receive = {
        case InsertTeam(team) => api.store(team).unit
        case RemoveTeam(id)   => client.deleteById(id into Id).unit
      }
    }),
    name = config.actorName
  )
