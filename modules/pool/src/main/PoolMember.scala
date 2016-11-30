package lila.pool

import org.joda.time.DateTime

import lila.user.User

case class PoolMember(
  userId: String,
  rating: Int,
  createdAt: DateTime)

object PoolMember {

  def apply(user: User, config: PoolConfig): PoolMember = PoolMember(
    user.id,
    user.perfs(config.perfType).intRating,
    DateTime.now)
}
