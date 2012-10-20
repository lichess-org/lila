package lila
package game

import com.mongodb.casbah.MongoCollection

import core.Settings

final class GameEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection) {

  import settings._

  lazy val gameRepo = new GameRepo(mongodb(GameCollectionGame))

  lazy val pgnRepo = new PgnRepo(mongodb(GameCollectionPgn))

  lazy val cached = new Cached(
    gameRepo = gameRepo,
    nbTtl = GameCachedNbTtl)

  lazy val paginator = new PaginatorBuilder(
    gameRepo = gameRepo,
    cached = cached,
    maxPerPage = GamePaginatorMaxPerPage)

  lazy val featured = new Featured(
    gameRepo = gameRepo,
    lobbyHubName = ActorLobbyHub)

  lazy val export = Export(gameRepo) _

  lazy val listMenu = ListMenu(cached) _

  lazy val rewind = new Rewind

  lazy val gameJs = new GameJs(settings.GameJsPath)
}
