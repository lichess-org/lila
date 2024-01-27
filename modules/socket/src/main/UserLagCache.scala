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

  export cache.{ getIfPresent as get }

  def getLagRating(userId: UserId): Option[Int] =
    Centis
      .raw(get(userId))
      .map: c =>
        if c <= 15 then 4
        else if c <= 30 then 3
        else if c <= (50) then 2
        else 1
