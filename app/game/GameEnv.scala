package lila
package game

import com.mongodb.casbah.MongoCollection

import core.Settings

final class GameEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection) {

  import settings._

  lazy val gameRepo = new GameRepo(mongodb(MongoCollectionGame))

  lazy val cached = new Cached(
    gameRepo = gameRepo,
    nbTtl = GameCachedNbTtl)

  lazy val paginator = new PaginatorBuilder(
    gameRepo = gameRepo,
    maxPerPage = GamePaginatorMaxPerPage)
}
