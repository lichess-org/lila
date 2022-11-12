package lila.teamSearch

import akka.actor.*
import com.softwaremill.macwire.*
import io.methvin.play.autoconfig.*
import play.api.Configuration

import lila.common.config.*
import lila.search.*

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
    ec: scala.concurrent.ExecutionContext,
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
      import lila.team.actorApi.*
      def receive = {
        case InsertTeam(team) => api.store(team).unit
        case RemoveTeam(id)   => client.deleteById(Id(id)).unit
      }
    }),
    name = config.actorName
  )
