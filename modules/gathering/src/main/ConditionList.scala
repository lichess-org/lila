package lila.gathering

import lila.gathering.Condition.*

abstract class ConditionList(options: List[Option[Condition]]):

  lazy val list: List[Condition] = options.flatten

  def relevant = list.nonEmpty

  def accepted = WithVerdicts:
    list.map:
      WithVerdict(_, Accepted)

  def isRatingLimited = list.exists:
    case _: MaxRating => true
    case _: MinRating => true
    case _            => false
