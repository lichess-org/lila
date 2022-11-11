package lila.game

import com.github.blemale.scaffeine.LoadingCache
import scala.concurrent.duration.*

import lila.db.dsl.*
import lila.memo.{ CacheApi, MongoCache }
import lila.user.User

final class Cached(
    gameRepo: GameRepo,
    cacheApi: CacheApi,
    mongoCache: MongoCache.Api
)(using ec: scala.concurrent.ExecutionContext):

  def nbImportedBy(userId: User.ID): Fu[Int] = nbImportedCache.get(userId)
  export nbImportedCache.invalidate as clearNbImportedByCache
  export nbImportedCache.get as nbPlaying

  def nbTotal: Fu[Long] = nbTotalCache.get {}

  def lastPlayedPlayingId(userId: User.ID): Fu[Option[Game.ID]] = lastPlayedPlayingIdCache get userId

  private val lastPlayedPlayingIdCache: LoadingCache[User.ID, Fu[Option[Game.ID]]] =
    CacheApi.scaffeineNoScheduler
      .expireAfterWrite(11 seconds)
      .build(gameRepo.lastPlayedPlayingId)

  lila.common.Bus.subscribeFun("startGame") { case lila.game.actorApi.StartGame(game) =>
    game.userIds foreach lastPlayedPlayingIdCache.invalidate
  }

  private val nbPlayingCache = cacheApi[User.ID, Int](256, "game.nbPlaying") {
    _.expireAfterWrite(15 seconds)
      .buildAsyncFuture { userId =>
        gameRepo.coll.countSel(Query nowPlaying userId)
      }
  }

  private val nbImportedCache = mongoCache[User.ID, Int](
    4096,
    "game:imported",
    30 days,
    identity
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
