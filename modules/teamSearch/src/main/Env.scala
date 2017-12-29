package lila.teamSearch

import akka.actor._
import com.typesafe.config.Config

import lila.search._

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

  def cli = new lila.common.Cli {
    def process = {
      case "team" :: "search" :: "reset" :: Nil => api.reset inject "done"
    }
  }

  private lazy val paginatorBuilder = new lila.search.PaginatorBuilder[lila.team.Team, Query](
    searchApi = api,
    maxPerPage = lila.common.MaxPerPage(PaginatorMaxPerPage)
  )

  system.actorOf(Props(new Actor {
    import lila.team.actorApi._
    def receive = {
      case InsertTeam(team) => api store team
      case RemoveTeam(id) => client deleteById Id(id)
    }
  }), name = ActorName)
}

object Env {

  lazy val current = "teamSearch" boot new Env(
    config = lila.common.PlayApp loadConfig "teamSearch",
    makeClient = lila.search.Env.current.makeClient,
    system = lila.common.PlayApp.system
  )
}
