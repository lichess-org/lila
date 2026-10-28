package lila.game

import lila.db.dsl.*
import lila.memo.MongoCache

final class Cached(gameRepo: GameRepo, mongoCache: MongoCache.Api)(using Executor):

  def nbImportedBy(userId: UserId): Fu[Int] = nbImportedCache.get(userId)
  export nbImportedCache.invalidate as clearNbImportedByCache

  def nbTotal: Fu[Long] = nbTotalCache.get {}

  private val nbImportedCache = mongoCache[UserId, Int](4096, "game:imported", 30.days, _.value): loader =>
    _.expireAfterAccess(10.minutes).buildAsyncFuture:
      loader: userId =>
        gameRepo.coll.secondary.countSel(Query.imported(userId))

  private val nbTotalCache = mongoCache.unit[Long]("game:total", 29.minutes): loader =>
    _.refreshAfterWrite(30.minutes).buildAsyncFuture:
      loader: _ =>
        gameRepo.coll.secondary.countAll
