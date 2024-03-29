package lila.pool

import lila.playban.RageSit
import lila.hub.rating.RatingRange
import lila.hub.pool.{ PoolMember, Joiner }

extension (m: PoolMember)
  def incMisses                     = m.copy(misses = m.misses + 1)
  def ratingDiff(other: PoolMember) = IntRatingDiff(Math.abs(m.rating.value - other.rating.value))
  def withRange(r: Option[RatingRange]) =
    if r == m.ratingRange then m
    else m.copy(ratingRange = r, misses = 0)
  def hasRange = m.ratingRange.isDefined

object PoolMember:

  def apply(joiner: Joiner, rageSit: RageSit): PoolMember =
    lila.hub.pool.PoolMember(
      userId = joiner.me,
      sri = joiner.sri,
      lame = joiner.lame,
      rating = joiner.rating,
      ratingRange = joiner.ratingRange,
      blocking = joiner.blocking,
      rageSitCounter = rageSit.counter / 10
    )
