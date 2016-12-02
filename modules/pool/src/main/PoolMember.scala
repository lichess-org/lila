package lila.pool

import org.joda.time.DateTime

import lila.user.User
import lila.rating.RatingRange

case class PoolMember(
    userId: User.ID,
    socketId: lila.socket.Socket.Uid,
    rating: Int,
    range: Option[RatingRange],
    engine: Boolean,
    since: DateTime,
    misses: Int = 0 // how many waves they missed
) {

  def incMisses = copy(misses = misses + 1)

  def waitMillis: Int = (DateTime.now.getMillis - since.getMillis).toInt

  def ratingDiff(other: PoolMember) = Math.abs(rating - other.rating)
}

object PoolMember {

  def apply(joiner: PoolApi.Joiner, config: PoolConfig): PoolMember =
    PoolMember(
      userId = joiner.userId,
      socketId = joiner.socketId,
      engine = joiner.engine,
      rating = joiner.ratingMap.getOrElse(config.perfType.key, 1500),
      range = joiner.ratingRange,
      since = DateTime.now)
}
