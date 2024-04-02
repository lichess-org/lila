package lila.pool

import lila.playban.RageSit
import lila.core.rating.RatingRange
import lila.core.pool.{ PoolMember, Joiner }

extension (m: PoolMember)
  def incMisses                     = m.copy(misses = m.misses + 1)
  def ratingDiff(other: PoolMember) = IntRatingDiff(Math.abs(m.rating.value - other.rating.value))
  def withRange(r: Option[RatingRange]) =
    if r == m.ratingRange then m
    else m.copy(ratingRange = r, misses = 0)
  def hasRange = m.ratingRange.isDefined

object PoolMember:

  def apply(joiner: Joiner, rageSit: RageSit): PoolMember =
    lila.core.pool.PoolMember(
      userId = joiner.me,
      sri = joiner.sri,
      lame = joiner.lame,
      rating = joiner.rating,
      ratingRange = joiner.ratingRange,
      blocking = joiner.blocking,
      rageSitCounter = rageSit.counter / 10
    )
