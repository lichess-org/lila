package lila.socket

import chess.Centis
import com.github.blemale.scaffeine.Cache

import lila.core.socket.userLag.*

final class UserLagCache(using Executor):

  private val cache: Cache[UserId, Centis] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterWrite(15.minutes)
    .build[UserId, Centis]()

  val put: Put = (userId, lag) =>
    if lag.centis >= 0
    then
      cache.put(
        userId,
        cache
          .getIfPresent(userId)
          .fold(lag):
            _.avg(lag)
      )

  export cache.getIfPresent as get

  val getLagRating: GetLagRating = userId =>
    Centis
      .raw(get(userId))
      .map: c =>
        if c <= 15 then 4
        else if c <= 30 then 3
        else if c <= (50) then 2
        else 1
