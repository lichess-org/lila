package lila.gameSearch

import akka.actor._
import com.typesafe.config.Config

import lila.search._

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

  lazy val paginator = new PaginatorBuilder[lila.game.Game, Query](
    searchApi = api,
    maxPerPage = lila.common.MaxPerPage(PaginatorMaxPerPage)
  )

  lazy val forms = new DataForm

  lazy val userGameSearch = new UserGameSearch(
    forms = forms,
    paginator = paginator
  )

  system.lilaBus.subscribe(system.actorOf(Props(new Actor {
    import lila.game.actorApi.{ InsertGame, FinishGame }
    def receive = {
      case FinishGame(game, _, _) if !game.aborted => self ! InsertGame(game)
      case InsertGame(game) => api store game
    }
  }), name = ActorName), 'finishGame)
}

object Env {

  lazy val current = "gameSearch" boot new Env(
    config = lila.common.PlayApp loadConfig "gameSearch",
    system = old.play.Env.actorSystem,
    makeClient = lila.search.Env.current.makeClient
  )
}
