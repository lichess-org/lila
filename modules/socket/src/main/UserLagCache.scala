package lila.socket

import chess.Centis
import com.github.blemale.scaffeine.Cache

object UserLagCache:

  private val cache: Cache[UserId, Centis] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterWrite(15 minutes)
    .build[UserId, Centis]()

  def put(userId: UserId, lag: Centis): Unit =
    if lag.centis >= 0
    then
      cache.put(
        userId,
        cache
          .getIfPresent(userId)
          .fold(lag):
            _ avg lag
      )

  def get(userId: UserId): Option[Centis] = cache.getIfPresent(userId)

  def getLagRating(userId: UserId): Option[Int] =
    get(userId).map:
      case i if i <= Centis(15) => 4
      case i if i <= Centis(30) => 3
      case i if i <= Centis(50) => 2
      case _                    => 1
