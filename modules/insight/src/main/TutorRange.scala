package lila.insight

import cats.data.NonEmptyList

import lila.analyse.{ AccuracyPercent, WinPercent }

sealed abstract class RelativeStrength(val id: Int, val name: String)
object RelativeStrength {
  case object MuchWeaker   extends RelativeStrength(10, "Much weaker")
  case object Weaker       extends RelativeStrength(20, "Weaker")
  case object Similar      extends RelativeStrength(30, "Similar")
  case object Stronger     extends RelativeStrength(40, "Stronger")
  case object MuchStronger extends RelativeStrength(50, "Much stronger")
  val all = List[RelativeStrength](MuchWeaker, Weaker, Similar, Stronger, MuchStronger)
  val byId = all map { p =>
    (p.id, p)
  } toMap
  def apply(diff: Int) =
    diff match {
      case d if d < -200 => MuchWeaker
      case d if d < -100 => Weaker
      case d if d > 200  => MuchStronger
      case d if d > 100  => Stronger
      case _             => Similar
    }
}

sealed abstract class MovetimeRange(val id: Int, val name: String, val tenths: Int)
object MovetimeRange {
  case object MTR1   extends MovetimeRange(1, "0 to 1 second", 10)
  case object MTR3   extends MovetimeRange(3, "1 to 3 seconds", 30)
  case object MTR5   extends MovetimeRange(5, "3 to 5 seconds", 50)
  case object MTR10  extends MovetimeRange(10, "5 to 10 seconds", 100)
  case object MTR30  extends MovetimeRange(30, "10 to 30 seconds", 300)
  case object MTRInf extends MovetimeRange(60, "More than 30 seconds", Int.MaxValue)
  val all: List[MovetimeRange] = List(MTR1, MTR3, MTR5, MTR10, MTR30, MTRInf)
  def reversedNoInf            = all.reverse drop 1
  val byId = all map { p =>
    (p.id, p)
  } toMap
  def toRange(mr: MovetimeRange): (Int, Int) =
    (
      all.indexOption(mr).map(_ - 1).flatMap(all.lift).fold(0)(_.tenths),
      mr.tenths
    )
}

sealed abstract class MaterialRange(val id: Int, val name: String, val imbalance: Int) {
  def negative = imbalance <= 0
}
object MaterialRange {
  case object Down4 extends MaterialRange(1, "Less than -6", -6)
  case object Down3 extends MaterialRange(2, "-3 to -6", -3)
  case object Down2 extends MaterialRange(3, "-1 to -3", -1)
  case object Down1 extends MaterialRange(4, "0 to -1", 0)
  case object Equal extends MaterialRange(5, "Equal", 0)
  case object Up1   extends MaterialRange(6, "0 to +1", 1)
  case object Up2   extends MaterialRange(7, "+1 to +3", 3)
  case object Up3   extends MaterialRange(8, "+3 to +6", 6)
  case object Up4   extends MaterialRange(9, "More than +6", Int.MaxValue)
  val all                     = List[MaterialRange](Down4, Down3, Down2, Down1, Equal, Up1, Up2, Up3, Up4)
  def reversedButEqualAndLast = all.diff(List(Equal, Up4)).reverse
  val byId = all map { p =>
    (p.id, p)
  } toMap
  def toRange(mr: MaterialRange): (Int, Int) =
    if (mr.id == Equal.id) (0, 0)
    else
      (
        byId.get(mr.id - 1).fold(Int.MinValue)(_.imbalance),
        mr.imbalance
      )
}

sealed abstract class TimeVariance(val id: Float, val name: String) {
  lazy val intFactored = (id * TimeVariance.intFactor).toInt
}
object TimeVariance {
  case object VeryConsistent  extends TimeVariance(0.25f, "Very consistent")
  case object QuiteConsistent extends TimeVariance(0.4f, "Quite consistent")
  case object Medium          extends TimeVariance(0.6f, "Medium")
  case object QuiteVariable   extends TimeVariance(0.75f, "Quite variable")
  case object VeryVariable    extends TimeVariance(1f, "Very variable")
  val all = List(VeryConsistent, QuiteConsistent, Medium, QuiteVariable, VeryVariable)
  val byId = all map { p =>
    (p.id, p)
  } toMap
  def apply(v: Float) = all.find(_.id >= v) | VeryVariable
  val intFactor: Int  = 100_000 // multiply variance by that to get an Int for storage
  def toRange(tv: TimeVariance): (Int, Int) =
    if (tv == VeryVariable) (QuiteVariable.intFactored, Int.MaxValue)
    else
      (
        all.indexOf(tv).some.filter(-1 !=).map(_ - 1).flatMap(all.lift).fold(0)(_.intFactored),
        tv.intFactored
      )
}

final class CplRange(val name: String, val cpl: Int)
object CplRange {
  val all = List(0, 10, 25, 50, 100, 200, 500, 99999).map { cpl =>
    new CplRange(
      name =
        if (cpl == 0) "Perfect"
        else if (cpl == 99999) "> 500 CPL"
        else s"≤ $cpl CPL",
      cpl = cpl
    )
  }
  val byId = all.map { p =>
    (p.cpl, p)
  }.toMap
  val worse = all.last
}

sealed abstract class EvalRange(val id: Int, val name: String, val eval: Int)
object EvalRange {
  case object Down5 extends EvalRange(1, "Less than -600", -600)
  case object Down4 extends EvalRange(2, "-350 to -600", -350)
  case object Down3 extends EvalRange(3, "-175 to -350", -175)
  case object Down2 extends EvalRange(4, "-80 to -175", -80)
  case object Down1 extends EvalRange(5, "-25 to -80", -25)
  case object Equal extends EvalRange(6, "Equality", 25)
  case object Up1   extends EvalRange(7, "+25 to +80", 80)
  case object Up2   extends EvalRange(8, "+80 to +175", 175)
  case object Up3   extends EvalRange(9, "+175 to +350", 350)
  case object Up4   extends EvalRange(10, "+350 to +600", 600)
  case object Up5   extends EvalRange(11, "More than +600", Int.MaxValue)
  val all             = List[EvalRange](Down5, Down4, Down3, Down2, Down1, Equal, Up1, Up2, Up3, Up4, Up5)
  def reversedButLast = all.init.reverse
  val byId = all map { p =>
    (p.id, p)
  } toMap
  def toRange(er: EvalRange): (Int, Int) = (
    byId.get(er.id - 1).fold(Int.MinValue)(_.eval),
    er.eval
  )
}

final class AccuracyPercentRange(val name: String, val bottom: AccuracyPercent)
object AccuracyPercentRange {
  val all: NonEmptyList[AccuracyPercentRange] = (0 to 90 by 10).toList.toNel.get.map { pc =>
    new AccuracyPercentRange(s"$pc% to ${pc + 10}%", AccuracyPercent(pc))
  }
  val byPercent                             = all.toList map { p => (p.bottom.toInt, p) } toMap
  def toRange(bottom: AccuracyPercentRange) = (bottom.bottom, AccuracyPercent(bottom.bottom.value + 10))
}

final class WinPercentRange(val name: String, val bottom: WinPercent)
object WinPercentRange {
  val all: NonEmptyList[WinPercentRange] = (0 to 90 by 10).toList.toNel.get.map { pc =>
    new WinPercentRange(s"$pc% to ${pc + 10}%", WinPercent(pc))
  }
  val byPercent                        = all.toList map { p => (p.bottom.toInt, p) } toMap
  def toRange(bottom: WinPercentRange) = (bottom.bottom, WinPercent(bottom.bottom.value + 10))
}

sealed class ClockPercentRange(val name: String, val bottom: ClockPercent)
object ClockPercentRange {
  val all = NonEmptyList.of[ClockPercentRange](
    new ClockPercentRange("≤3% time left", ClockPercent fromPercent 0),
    new ClockPercentRange("3% to 10% time left", ClockPercent fromPercent 3),
    new ClockPercentRange("10% to 25% time left", ClockPercent fromPercent 10),
    new ClockPercentRange("25% to 50% time left", ClockPercent fromPercent 25),
    new ClockPercentRange("≥50% time left", ClockPercent fromPercent 50)
  )
  val byPercent = all.toList map { p => (p.bottom.toInt, p) } toMap
  def toRange(x: ClockPercentRange): (ClockPercent, ClockPercent) = (
    x.bottom,
    all.toList.next(x).fold(ClockPercent(100))(_.bottom)
  )
}
