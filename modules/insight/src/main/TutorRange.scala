package lila.insight

import chess.IntRating
import chess.rating.IntRatingDiff
import chess.eval.WinPercent

import lila.analyse.AccuracyPercent

enum RelativeStrength(val id: Int, val name: String):
  case MuchWeaker   extends RelativeStrength(10, "Much weaker")
  case Weaker       extends RelativeStrength(20, "Weaker")
  case Similar      extends RelativeStrength(30, "Similar")
  case Stronger     extends RelativeStrength(40, "Stronger")
  case MuchStronger extends RelativeStrength(50, "Much stronger")

object RelativeStrength:
  val byId = values.mapBy(_.id)
  def apply(myRating: IntRating, opRating: IntRating): RelativeStrength = apply(
    (opRating - myRating).into(IntRatingDiff)
  )
  def apply(diff: IntRatingDiff): RelativeStrength = diff.value match
    case d if d < -200 => MuchWeaker
    case d if d < -100 => Weaker
    case d if d > 200  => MuchStronger
    case d if d > 100  => Stronger
    case _             => Similar

enum MovetimeRange(val id: Int, val name: String, val tenths: Int):
  case MTR1   extends MovetimeRange(1, "0 to 1 second", 10)
  case MTR3   extends MovetimeRange(3, "1 to 3 seconds", 30)
  case MTR5   extends MovetimeRange(5, "3 to 5 seconds", 50)
  case MTR10  extends MovetimeRange(10, "5 to 10 seconds", 100)
  case MTR30  extends MovetimeRange(30, "10 to 30 seconds", 300)
  case MTRInf extends MovetimeRange(60, "More than 30 seconds", Int.MaxValue)
object MovetimeRange:
  def reversedNoInf = values.reverse.drop(1)
  val byId          = values.mapBy(_.id)
  def toRange(mr: MovetimeRange): (Int, Int) = (
    values.toIndexedSeq.indexOption(mr).map(_ - 1).flatMap(values.lift).fold(0)(_.tenths),
    mr.tenths
  )

enum MaterialRange(val id: Int, val name: String, val imbalance: Int):
  case Down4 extends MaterialRange(1, "Less than -6", -6)
  case Down3 extends MaterialRange(2, "-3 to -6", -3)
  case Down2 extends MaterialRange(3, "-1 to -3", -1)
  case Down1 extends MaterialRange(4, "0 to -1", 0)
  case Equal extends MaterialRange(5, "Equal", 0)
  case Up1   extends MaterialRange(6, "0 to +1", 1)
  case Up2   extends MaterialRange(7, "+1 to +3", 3)
  case Up3   extends MaterialRange(8, "+3 to +6", 6)
  case Up4   extends MaterialRange(9, "More than +6", Int.MaxValue)
  def negative = imbalance <= 0
object MaterialRange:
  def reversedButEqualAndLast = values.diff(List(Equal, Up4)).reverse
  val byId                    = values.mapBy(_.id)
  def toRange(mr: MaterialRange): (Int, Int) =
    if mr.id == Equal.id then (0, 0)
    else
      (
        byId.get(mr.id - 1).fold(Int.MinValue)(_.imbalance),
        mr.imbalance
      )

enum TimeVariance(val id: Float, val name: String):
  case VeryConsistent  extends TimeVariance(0.25f, "Very consistent")
  case QuiteConsistent extends TimeVariance(0.4f, "Quite consistent")
  case Medium          extends TimeVariance(0.6f, "Medium")
  case QuiteVariable   extends TimeVariance(0.75f, "Quite variable")
  case VeryVariable    extends TimeVariance(1f, "Very variable")
  def intFactored = (id * TimeVariance.intFactor).toInt
object TimeVariance:
  val byId            = values.mapBy(_.id)
  def apply(v: Float) = values.find(_.id >= v) | VeryVariable
  val intFactor: Int  = 100_000 // multiply variance by that to get an Int for storage
  def toRange(tv: TimeVariance): (Int, Int) =
    if tv == VeryVariable then (QuiteVariable.intFactored, Int.MaxValue)
    else
      (
        values.toIndexedSeq.indexOption(tv).map(_ - 1).flatMap(values.lift).fold(0)(_.intFactored),
        tv.intFactored
      )

final class CplRange(val name: String, val cpl: Int)
object CplRange:
  val all = List(0, 10, 25, 50, 100, 200, 500, 99999).map { cpl =>
    CplRange(
      name = if cpl == 0 then "Perfect" else if cpl == 99999 then "> 500 CPL" else s"≤ $cpl CPL",
      cpl = cpl
    )
  }
  val byId  = all.mapBy(_.cpl)
  val worse = all.last

enum EvalRange(val id: Int, val name: String, val eval: Int):
  case Down5 extends EvalRange(1, "Less than -600", -600)
  case Down4 extends EvalRange(2, "-350 to -600", -350)
  case Down3 extends EvalRange(3, "-175 to -350", -175)
  case Down2 extends EvalRange(4, "-80 to -175", -80)
  case Down1 extends EvalRange(5, "-25 to -80", -25)
  case Equal extends EvalRange(6, "Equality", 25)
  case Up1   extends EvalRange(7, "+25 to +80", 80)
  case Up2   extends EvalRange(8, "+80 to +175", 175)
  case Up3   extends EvalRange(9, "+175 to +350", 350)
  case Up4   extends EvalRange(10, "+350 to +600", 600)
  case Up5   extends EvalRange(11, "More than +600", Int.MaxValue)
object EvalRange:
  def reversedButLast = values.init.reverse
  val byId            = values.mapBy(_.id)
  def toRange(er: EvalRange): (Int, Int) = (
    byId.get(er.id - 1).fold(Int.MinValue)(_.eval),
    er.eval
  )

final class AccuracyPercentRange(val name: String, val bottom: AccuracyPercent)
object AccuracyPercentRange:
  val all: NonEmptyList[AccuracyPercentRange] = (0 to 90 by 10).toList.toNel.get.map { pc =>
    AccuracyPercentRange(s"$pc% to ${pc + 10}%", AccuracyPercent.fromPercent(pc))
  }
  val byPercent                             = all.toList.mapBy(_.bottom.toInt)
  def toRange(bottom: AccuracyPercentRange) = (bottom.bottom, bottom.bottom + 10)

final class WinPercentRange(val name: String, val bottom: WinPercent)
object WinPercentRange:
  val all: NonEmptyList[WinPercentRange] = (0 to 90 by 10).toList.toNel.get.map { pc =>
    WinPercentRange(s"$pc% to ${pc + 10}%", WinPercent(pc))
  }
  val byPercent                        = all.toList.mapBy(_.bottom.toInt)
  def toRange(bottom: WinPercentRange) = (bottom.bottom, WinPercent(bottom.bottom.value + 10))

final class ClockPercentRange(val name: String, val bottom: ClockPercent)
object ClockPercentRange:
  val all = NonEmptyList.of[ClockPercentRange](
    ClockPercentRange("≤3% time left", ClockPercent.fromPercent(0)),
    ClockPercentRange("3% to 10% time left", ClockPercent.fromPercent(3)),
    ClockPercentRange("10% to 25% time left", ClockPercent.fromPercent(10)),
    ClockPercentRange("25% to 50% time left", ClockPercent.fromPercent(25)),
    ClockPercentRange("≥50% time left", ClockPercent.fromPercent(50))
  )
  val byPercent = all.toList.mapBy(_.bottom.toInt)
  def toRange(x: ClockPercentRange): (ClockPercent, ClockPercent) = (
    x.bottom,
    all.toList.next(x).fold(ClockPercent.fromPercent(100))(_.bottom)
  )
