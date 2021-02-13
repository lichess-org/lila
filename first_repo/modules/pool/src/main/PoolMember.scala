package lila.pool

import lila.rating.RatingRange
import lila.user.User
import lila.playban.RageSit

case class PoolMember(
    userId: User.ID,
    sri: lila.socket.Socket.Sri,
    rating: Int,
    ratingRange: Option[RatingRange],
    lame: Boolean,
    blocking: PoolMember.BlockedUsers,
    rageSitCounter: Int,
    misses: Int = 0 // how many waves they missed
) {

  def incMisses = copy(misses = misses + 1)

  def ratingDiff(other: PoolMember) = Math.abs(rating - other.rating)

  def withRange(r: Option[RatingRange]) =
    if (r == ratingRange) this
    else copy(ratingRange = r, misses = 0)

  def hasRange = ratingRange.isDefined
}

object PoolMember {

  case class BlockedUsers(ids: Set[User.ID]) extends AnyVal

  def apply(joiner: PoolApi.Joiner, config: PoolConfig, rageSit: RageSit): PoolMember =
    PoolMember(
      userId = joiner.userId,
      sri = joiner.sri,
      lame = joiner.lame,
      rating = joiner.rating,
      ratingRange = joiner.ratingRange,
      blocking = BlockedUsers(joiner.blocking),
      rageSitCounter = rageSit.counter / 10
    )
}
