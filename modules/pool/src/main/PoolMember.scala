package lila.pool

import lila.rating.RatingRange
import lila.playban.RageSit

case class PoolMember(
    userId: UserId,
    sri: lila.socket.Socket.Sri,
    rating: IntRating,
    ratingRange: Option[RatingRange],
    lame: Boolean,
    blocking: Blocking,
    rageSitCounter: Int,
    misses: Int = 0 // how many waves they missed
):

  def incMisses = copy(misses = misses + 1)

  def ratingDiff(other: PoolMember) = IntRatingDiff(Math.abs(rating.value - other.rating.value))

  def withRange(r: Option[RatingRange]) =
    if r == ratingRange then this
    else copy(ratingRange = r, misses = 0)

  def hasRange = ratingRange.isDefined

object PoolMember:

  given UserIdOf[PoolMember] = _.userId

  def apply(joiner: PoolApi.Joiner, rageSit: RageSit): PoolMember =
    PoolMember(
      userId = joiner.me,
      sri = joiner.sri,
      lame = joiner.lame,
      rating = joiner.rating,
      ratingRange = joiner.ratingRange,
      blocking = joiner.blocking,
      rageSitCounter = rageSit.counter / 10
    )
