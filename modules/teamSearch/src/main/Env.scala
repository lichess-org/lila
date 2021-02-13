package lila.teamSearch

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration

import lila.common.config._
import lila.search._

@Module
private class TeamSearchConfig(
    @ConfigName("index") val indexName: String,
    @ConfigName("actor.name") val actorName: String
)

final class Env(
    appConfig: Configuration,
    makeClient: Index => ESClient,
    teamRepo: lila.team.TeamRepo
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  private val config = appConfig.get[TeamSearchConfig]("teamSearch")(AutoConfig.loader)

  private val maxPerPage = MaxPerPage(15)

  private lazy val client = makeClient(Index(config.indexName))

  private lazy val paginatorBuilder = wire[lila.search.PaginatorBuilder[lila.team.Team, Query]]

  lazy val api: TeamSearchApi = wire[TeamSearchApi]

  def apply(text: String, page: Int) = paginatorBuilder(Query(text), page)

  def cli =
    new lila.common.Cli {
      def process = {
        case "team" :: "search" :: "reset" :: Nil => api.reset inject "done"
      }
    }

  system.actorOf(
    Props(new Actor {
      import lila.team.actorApi._
      def receive = {
        case InsertTeam(team) => api store team
        case RemoveTeam(id)   => client deleteById Id(id)
      }
    }),
    name = config.actorName
  )
}
