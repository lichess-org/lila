package lila.teamSearch

import akka.actor._
import com.typesafe.config.Config

import lila.db.api.{ $find, $cursor }
import lila.search._
import lila.team.tube.teamTube

final class Env(
    config: Config,
    makeClient: Index => ESClient,
    system: ActorSystem) {

  private val IndexName = config getString "index"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  private val ActorName = config getString "actor.name"

  private val client = makeClient(Index(IndexName))

  def apply(text: String, page: Int) = paginatorBuilder(Query(text), page)

  def cli = new lila.common.Cli {
    import akka.pattern.ask
    private implicit def timeout = makeTimeout minutes 20
    def process = {
      case "team" :: "search" :: "reset" :: Nil => fuccess("done")
      // (indexer ? lila.search.actorApi.Reset) inject "Team search index rebuilt"
    }
  }

  // converter = $find.byOrderedIds[lila.team.Team] _

  private lazy val paginatorBuilder = new lila.search.PaginatorBuilder[lila.team.Team, Query](
    searchApi = ???,
    maxPerPage = PaginatorMaxPerPage)

  system.actorOf(Props(new Actor {
    import lila.team.actorApi._
    def receive = {
      case InsertTeam(team) => // client store store(team)
    }
  }), name = ActorName)
}

object Env {

  lazy val current = "teamSearch" boot new Env(
    config = lila.common.PlayApp loadConfig "teamSearch",
    makeClient = lila.search.Env.current.makeClient,
    system = lila.common.PlayApp.system)
}
