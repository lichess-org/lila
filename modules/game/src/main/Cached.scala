package lila.game

import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.MongoCache
import lila.user.User

final class Cached(
    gameRepo: GameRepo,
    cacheApi: lila.memo.CacheApi,
    mongoCache: MongoCache.Builder
)(implicit ec: scala.concurrent.ExecutionContext) {

  def nbImportedBy(userId: User.ID): Fu[Int] = nbImportedCache(userId)
  def clearNbImportedByCache                 = nbImportedCache remove _

  def nbTotal: Fu[Int] = countCache($empty)

  def nbPlaying = nbPlayingCache.get _

  private val nbPlayingCache = cacheApi[User.ID, Int](256, "game.nbPlaying") {
    _.expireAfterAccess(15 seconds)
      .buildAsyncFuture { userId =>
        gameRepo.coll.countSel(Query nowPlaying userId)
      }
  }

  private val nbImportedCache = mongoCache[User.ID, Int](
    prefix = "game:imported",
    f = userId => gameRepo.coll countSel Query.imported(userId),
    timeToLive = 1 hour,
    timeToLiveMongo = 30.days.some,
    keyToString = identity
  )

  private val countCache = mongoCache[Bdoc, Int](
    prefix = "game:count",
    f = gameRepo.coll.countSel(_),
    timeToLive = 1 hour,
    keyToString = lila.db.BSON.hashDoc
  )
}
