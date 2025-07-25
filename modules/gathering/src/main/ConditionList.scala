package lila.gathering

import lila.gathering.Condition.*

abstract class ConditionList(options: List[Option[Condition]]):

  def maxRating: Option[MaxRating]
  def minRating: Option[MinRating]

  final lazy val list: List[Condition] = options.flatten

  export list.nonEmpty

  def sameMaxRating(other: ConditionList) = maxRating.map(_.rating) == other.maxRating.map(_.rating)
  def sameMinRating(other: ConditionList) = minRating.map(_.rating) == other.minRating.map(_.rating)
  def sameRatings(other: ConditionList) = sameMaxRating(other) && sameMinRating(other)

  def accepted = WithVerdicts:
    list.map:
      WithVerdict(_, Accepted)

  def isRatingLimited = list.exists:
    case _: MaxRating => true
    case _: MinRating => true
    case _ => false

  def validRatings = (minRating, maxRating) match
    case (Some(min), Some(max)) => min.rating < max.rating
    case _ => true

  override def toString() = s"Conditions(${list.mkString(",")})"
