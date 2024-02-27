package lila.game

import com.github.blemale.scaffeine.LoadingCache
import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.{ CacheApi, MongoCache }
import lila.user.User

final class Cached(
    gameRepo: GameRepo,
    cacheApi: CacheApi,
    mongoCache: MongoCache.Api
)(implicit ec: scala.concurrent.ExecutionContext) {

  def nbImportedBy(userId: User.ID): Fu[Int] = nbImportedCache.get(userId)
  def clearNbImportedByCache                 = nbImportedCache invalidate _

  def nbTotal: Fu[Long] = nbTotalCache.get {}

  def nbPlaying = nbPlayingCache.get _

  def nbPaused = nbPausedCache.get _

  def lastPlayedPlayingId(userId: User.ID): Fu[Option[Game.ID]] = lastPlayedPlayingIdCache get userId

  private val lastPlayedPlayingIdCache: LoadingCache[User.ID, Fu[Option[Game.ID]]] =
    CacheApi.scaffeineNoScheduler
      .expireAfterWrite(5 seconds)
      .build(gameRepo.lastPlayedPlayingId _)

  lila.common.Bus.subscribeFun("startGame") { case lila.game.actorApi.StartGame(game) =>
    game.userIds foreach { lastPlayedPlayingIdCache.invalidate(_) }
  }

  private val nbPlayingCache = cacheApi[User.ID, Int](256, "game.nbPlaying") {
    _.expireAfterWrite(15 seconds)
      .buildAsyncFuture { userId =>
        gameRepo.coll.countSel(Query nowPlaying userId)
      }
  }

  private val nbPausedCache = cacheApi[User.ID, Int](256, "game.nbPaused") {
    _.expireAfterWrite(2 minute)
      .buildAsyncFuture { userId =>
        gameRepo.coll.countSel(Query paused userId)
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

  private val nbTotalCache = mongoCache.unit[Long](
    "game:total",
    29 minutes
  ) { loader =>
    _.refreshAfterWrite(30 minutes)
      .buildAsyncFuture {
        loader { _ =>
          gameRepo.coll.countAll
        }
      }
  }
}
