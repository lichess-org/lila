package lila.tutor

import alleycats.Zero
import chess.{ Division, Situation }

import lila.analyse.Analysis
import lila.game.Pov
import lila.insight.{ InsightPerfStats, Metric, TimePressure }
import lila.rating.PerfType
import lila.user.User

case class Rating(value: Double) extends AnyVal
object Rating {
  implicit val ordering = Ordering.by[Rating, Double](_.value)
}

case class ValueCount[V](value: V, count: Int) {
  def map[B](f: V => B)      = copy(value = f(value))
  def reliableEnough         = count >= 50
  def relevantTo(total: Int) = reliableEnough && count * 10 > total
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
}

sealed abstract class TutorMetric(val metric: Metric)

object TutorMetric {
  case object GlobalTimePressure extends TutorMetric(Metric.TimePressure)
  case object DefeatTimePressure extends TutorMetric(Metric.TimePressure)
  case object Accuracy           extends TutorMetric(Metric.MeanAccuracy)
  case object Awareness          extends TutorMetric(Metric.Awareness)
  case object Performance        extends TutorMetric(Metric.Performance)
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
case class ValueComparison private (value: Double) {

  import ValueComparison.Wording

  def abs    = math.abs(value)
  def better = wording == Wording.MuchBetter || wording == Wording.Better || wording == Wording.SlightlyBetter
  def worse  = wording == Wording.MuchWorse || wording == Wording.Worse || wording == Wording.SlightlyWorse
  def negate = copy(value = -value)

  val wording: Wording = value match {
    case v if v < -0.5  => Wording.MuchWorse
    case v if v < -0.2  => Wording.Worse
    case v if v < -0.05 => Wording.SlightlyWorse
    case v if v < 0.05  => Wording.Similar
    case v if v < 0.2   => Wording.SlightlyBetter
    case v if v < 0.5   => Wording.Better
    case _              => Wording.MuchBetter
  }
}

object ValueComparison {
  def apply(a: Double, b: Double): ValueComparison = apply((a / b) - 1)
  def apply(value: Double): ValueComparison        = new ValueComparison(value atLeast -1 atMost 1)

  sealed abstract class Wording(val value: String)
  object Wording {
    case object MuchWorse      extends Wording("much worse than")
    case object Worse          extends Wording("worse than")
    case object SlightlyWorse  extends Wording("slightly worse than")
    case object Similar        extends Wording("similar to")
    case object SlightlyBetter extends Wording("slightly better than")
    case object Better         extends Wording("better than")
    case object MuchBetter     extends Wording("much better than")
  }
}

case class TutorUser(user: User, perfType: PerfType, perfStats: InsightPerfStats)
