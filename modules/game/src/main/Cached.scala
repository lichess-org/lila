package lila.game

import com.github.blemale.scaffeine.LoadingCache

import lila.db.dsl.*
import lila.memo.{ CacheApi, MongoCache }

final class Cached(gameRepo: GameRepo, mongoCache: MongoCache.Api)(using Executor):

  def nbImportedBy(userId: UserId): Fu[Int] = nbImportedCache.get(userId)
  export nbImportedCache.invalidate as clearNbImportedByCache
  export gameRepo.countNowPlaying as nbPlaying

  def nbTotal: Fu[Long] = nbTotalCache.get {}

  def lastPlayedPlayingId(userId: UserId): Fu[Option[GameId]] = lastPlayedPlayingIdCache.get(userId)

  private val lastPlayedPlayingIdCache: LoadingCache[UserId, Fu[Option[GameId]]] =
    CacheApi.scaffeineNoScheduler
      .expireAfterWrite(11.seconds)
      .build(gameRepo.lastPlayedPlayingId)

  lila.common.Bus.sub[lila.core.game.StartGame]: start =>
    start.game.userIds.foreach(lastPlayedPlayingIdCache.invalidate)

  private val nbImportedCache = mongoCache[UserId, Int](4096, "game:imported", 30.days, _.value): loader =>
    _.expireAfterAccess(10.minutes).buildAsyncFuture:
      loader: userId =>
        gameRepo.coll.secondary.countSel(Query.imported(userId))

  private val nbTotalCache = mongoCache.unit[Long]("game:total", 29.minutes): loader =>
    _.refreshAfterWrite(30.minutes).buildAsyncFuture:
      loader: _ =>
        gameRepo.coll.secondary.countAll
