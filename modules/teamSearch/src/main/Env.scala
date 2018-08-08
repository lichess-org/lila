package lidraughts.teamSearch

import akka.actor._
import com.typesafe.config.Config

import lidraughts.search._

final class Env(
    config: Config,
    makeClient: Index => ESClient,
    system: ActorSystem
) {

  private val IndexName = config getString "index"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  private val ActorName = config getString "actor.name"

  private val client = makeClient(Index(IndexName))

  val api = new TeamSearchApi(client)

  def apply(text: String, page: Int) = paginatorBuilder(Query(text), page)

  def cli = new lidraughts.common.Cli {
    def process = {
      case "team" :: "search" :: "reset" :: Nil => api.reset inject "done"
    }
  }

  private lazy val paginatorBuilder = new lidraughts.search.PaginatorBuilder[lidraughts.team.Team, Query](
    searchApi = api,
    maxPerPage = lidraughts.common.MaxPerPage(PaginatorMaxPerPage)
  )

  system.actorOf(Props(new Actor {
    import lidraughts.team.actorApi._
    def receive = {
      case InsertTeam(team) => api store team
      case RemoveTeam(id) => client deleteById Id(id)
    }
  }), name = ActorName)
}

object Env {

  lazy val current = "teamSearch" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "teamSearch",
    makeClient = lidraughts.search.Env.current.makeClient,
    system = lidraughts.common.PlayApp.system
  )
}
