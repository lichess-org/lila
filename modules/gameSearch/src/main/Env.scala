package lila.gameSearch

import akka.actor._
import com.typesafe.config.Config

import lila.search._

final class Env(
    config: Config,
    system: ActorSystem,
    makeClient: Index => ESClient) {

  private val IndexName = config getString "index"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  private val ActorName = config getString "actor.name"

  private lazy val client = makeClient(Index(IndexName))

  lazy val api = new GameSearchApi(client)

  lazy val paginator = new PaginatorBuilder[lila.game.Game, Query](
    searchApi = api,
    maxPerPage = PaginatorMaxPerPage)

  lazy val forms = new DataForm

  lazy val userGameSearch = new UserGameSearch(
    forms = forms,
    paginator = paginator)

  system.actorOf(Props(new Actor {
    import lila.game.actorApi.{ InsertGame, FinishGame }
    context.system.lilaBus.subscribe(self, 'finishGame)
    def receive = {
      case FinishGame(game, _, _) => self ! InsertGame(game)
      case InsertGame(game)       => api store game
    }
  }), name = ActorName)

  def cli = new lila.common.Cli {
    import akka.pattern.ask
    private implicit def timeout = makeTimeout minutes 60
    def process = {
      case "game" :: "search" :: "index" :: "all" :: Nil => api.indexAll inject "done"
      case "game" :: "search" :: "index" :: since :: Nil => api.indexSince(since) inject "done"
    }
  }
}

object Env {

  lazy val current = "gameSearch" boot new Env(
    config = lila.common.PlayApp loadConfig "gameSearch",
    system = lila.common.PlayApp.system,
    makeClient = lila.search.Env.current.makeClient)
}
