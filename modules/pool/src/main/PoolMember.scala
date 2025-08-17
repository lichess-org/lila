package lila.pool

import chess.rating.IntRatingDiff

import lila.core.pool.PoolMember
import lila.core.rating.RatingRange

extension (m: PoolMember)
  def incMisses = m.copy(misses = m.misses + 1)
  def ratingDiff(other: PoolMember) = IntRatingDiff(Math.abs(m.rating.value - other.rating.value))
  def withRange(r: Option[RatingRange]) =
    if r == m.ratingRange then m
    else m.copy(ratingRange = r, misses = 0)
  def hasRange = m.ratingRange.isDefined
