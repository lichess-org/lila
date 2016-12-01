package lila.pool

import org.joda.time.DateTime

import lila.user.User

case class PoolMember(
    userId: User.ID,
    socketId: lila.socket.Socket.Uid,
    rating: Int,
    misses: Int = 0 // how many waves they missed
) {

  def incMisses = copy(misses = misses + 1)
}

object PoolMember {

  def apply(joiner: PoolApi.Joiner, config: PoolConfig): PoolMember =
    PoolMember(
      joiner.userId,
      joiner.socketId,
      joiner.ratingMap.getOrElse(config.perfType.key, 1500))
}
