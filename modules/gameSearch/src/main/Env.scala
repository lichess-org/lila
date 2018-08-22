package lidraughts.gameSearch

import akka.actor._
import com.typesafe.config.Config

import lidraughts.game.actorApi.{ InsertGame, FinishGame }
import lidraughts.search._

final class Env(
    config: Config,
    system: ActorSystem,
    makeClient: Index => ESClient
) {

  private val IndexName = config getString "index"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  private val ActorName = config getString "actor.name"

  private lazy val client = makeClient(Index(IndexName))

  lazy val api = new GameSearchApi(client, system)

  def cli = new lidraughts.common.Cli {
    def process = {
      case "game" :: "search" :: "reset" :: Nil => api.reset inject "done"
    }
  }

  lazy val paginator = new PaginatorBuilder[lidraughts.game.Game, Query](
    searchApi = api,
    maxPerPage = lidraughts.common.MaxPerPage(PaginatorMaxPerPage)
  )

  lazy val forms = new DataForm

  lazy val userGameSearch = new UserGameSearch(
    forms = forms,
    paginator = paginator
  )

  system.lidraughtsBus.subscribeFun('finishGame) {
    case FinishGame(game, _, _) if !game.aborted => api store game
    case InsertGame(game) => api store game
  }
}

object Env {

  lazy val current = "gameSearch" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "gameSearch",
    system = lidraughts.common.PlayApp.system,
    makeClient = lidraughts.search.Env.current.makeClient
  )
}
