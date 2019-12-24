package lila.game

import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.MongoCache
import lila.user.User

final class Cached(
    gameRepo: GameRepo,
    cacheApi: lila.memo.CacheApi,
    mongoCache: MongoCache.Api
)(implicit ec: scala.concurrent.ExecutionContext) {

  def nbImportedBy(userId: User.ID): Fu[Int] = nbImportedCache.get(userId)
  def clearNbImportedByCache                 = nbImportedCache invalidate _

  def nbTotal: Fu[Int] = nbTotalCache.get({})

  def nbPlaying = nbPlayingCache.get _

  private val nbPlayingCache = cacheApi[User.ID, Int](256, "game.nbPlaying") {
    _.expireAfterWrite(15 seconds)
      .buildAsyncFuture { userId =>
        gameRepo.coll.countSel(Query nowPlaying userId)
      }
  }

  private val nbImportedCache = mongoCache[User.ID, Int](
    1024,
    "game:imported",
    30 days,
    _.toString
  ) { loader =>
    _.expireAfterAccess(10 minutes)
      .buildAsyncFuture {
        loader { userId =>
          gameRepo.coll countSel Query.imported(userId)
        }
      }
  }

  private val nbTotalCache = mongoCache.unit[Int](
    "game:total",
    29 minutes
  ) { loader =>
    _.refreshAfterWrite(30 minutes)
      .buildAsyncFuture {
        loader { _ =>
          gameRepo.coll.countSel($empty)
        }
      }
  }
}
