package lila.tutor

import alleycats.Zero
import chess.{ Division, Situation }

import lila.analyse.Analysis
import lila.game.Pov
import lila.insight.{ ClockPercent, InsightMetric, InsightPerfStats, MeanRating }
import lila.rating.PerfType
import lila.user.User
import lila.analyse.AccuracyPercent

case class Rating(value: Double) extends AnyVal
object Rating {
  implicit val ordering = Ordering.by[Rating, Double](_.value)
}

case class ValueCount[V](value: V, count: Int) {
  def map[B](f: V => B)      = copy(value = f(value))
  def reliableEnough         = count >= 50
  def relevantTo(total: Int) = reliableEnough && count * 10 > total
}

case class TutorBothValuesAvailable[A](mine: ValueCount[A], peer: ValueCount[A])(implicit
    o: Ordering[A]
) {
  // def map[B: Ordering](f: A => B)                           = TutorBothValuesAvailable(mine map f, peer map f)
  def higher                                        = o.compare(mine.value, peer.value) >= 0
  def grade(implicit number: TutorNumber[A]): Grade = number.grade(mine.value, peer.value)
}
case class TutorBothValues[A](mine: ValueCount[A], peer: Option[ValueCount[A]])(implicit o: Ordering[A]) {
  def map[B: Ordering](f: A => B) = TutorBothValues(mine map f, peer map (_ map f))
  def higher                      = peer.exists(p => o.compare(mine.value, p.value) >= 0)
  def toOption                    = TutorBothValueOptions(mine.some, peer)
}
case class TutorBothValueOptions[A](mine: Option[ValueCount[A]], peer: Option[ValueCount[A]])(implicit
    o: Ordering[A]
) {
  def map[B: Ordering](f: A => B) = TutorBothValueOptions(mine map (_ map f), peer map (_ map f))
  def higher                      = mine.exists(m => peer.exists(p => o.compare(m.value, p.value) >= 0))
  def asAvailable                 = for { m <- mine; p <- peer } yield TutorBothValuesAvailable(m, p)
  def grade(implicit number: TutorNumber[A]): Option[Grade] = asAvailable.map(_.grade)
}

sealed abstract class TutorMetric[V](val metric: InsightMetric)

object TutorMetric {
  case object GlobalClock extends TutorMetric[ClockPercent](InsightMetric.ClockPercent)
  // time used when losing ((100 - clockPercent) on last move)
  case object ClockUsage  extends TutorMetric[ClockPercent](InsightMetric.ClockPercent)
  case object Accuracy    extends TutorMetric[AccuracyPercent](InsightMetric.MeanAccuracy)
  case object Awareness   extends TutorMetric[TutorRatio](InsightMetric.Awareness)
  case object Performance extends TutorMetric[Rating](InsightMetric.Performance)
}

case class TutorRatio(value: Double) extends AnyVal {
  def percent = value * 100
}

object TutorRatio {

  def apply(a: Int, b: Int): TutorRatio       = TutorRatio(a.toDouble / b)
  def apply(a: Double, b: Double): TutorRatio = TutorRatio(a / b)
  def fromPercent(p: Double): TutorRatio      = TutorRatio(p / 100)

  implicit val zero     = Zero(TutorRatio(0))
  implicit val ordering = Ordering.by[TutorRatio, Double](_.value)
}

// value from -1 (worse) to +1 (best)
case class Grade private (value: Double) {

  import Grade.Wording

  def abs    = math.abs(value)
  def better = wording >= Wording.SlightlyBetter
  def worse  = wording <= Wording.SlightlyWorse
  def negate = copy(value = -value)

  val wording: Wording = Wording.list.find(_.top > value) | Wording.MuchBetter
}

object Grade {
  def apply(a: Double, b: Double): Grade = apply((a / b) - 1)
  def apply(value: Double): Grade        = new Grade(value atLeast -1 atMost 1)

  sealed abstract class Wording(val value: String, val top: Double) extends Ordered[Wording] {
    def compare(other: Wording) = top compare other.top
  }
  object Wording {
    case object MuchWorse      extends Wording("much worse than", -0.5)
    case object Worse          extends Wording("worse than", -0.2)
    case object SlightlyWorse  extends Wording("slightly worse than", -0.05)
    case object Similar        extends Wording("similar to", 0.05)
    case object SlightlyBetter extends Wording("slightly better than", 0.2)
    case object Better         extends Wording("better than", 0.5)
    case object MuchBetter     extends Wording("much better than", 1)
    val list = List[Wording](MuchWorse, Worse, SlightlyWorse, Similar, SlightlyBetter, Better, MuchBetter)
  }
}

case class TutorUser(user: User, perfType: PerfType, perfStats: InsightPerfStats)
