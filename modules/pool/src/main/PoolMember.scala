package lila.pool

import org.joda.time.DateTime

import lila.user.User

case class PoolMember(
    userId: User.ID,
    socketId: lila.socket.Socket.Uid,
    rating: Int,
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
      joiner.userId,
      joiner.socketId,
      joiner.ratingMap.getOrElse(config.perfType.key, 1500),
      since = DateTime.now)
}
