package lila.pool

import org.joda.time.DateTime

import lila.user.User

case class PoolMember(
  userId: User.ID,
  socketId: lila.socket.Socket.Uid,
  rating: Int,
  createdAt: DateTime)

object PoolMember {

  def apply(joiner: PoolApi.Joiner, config: PoolConfig): PoolMember =
    PoolMember(
      joiner.userId,
      joiner.socketId,
      joiner.ratingMap.getOrElse(config.perfType.key, 1500),
      DateTime.now)
}
