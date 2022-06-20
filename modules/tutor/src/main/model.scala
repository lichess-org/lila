package lila.tutor

import alleycats.Zero
import chess.{ Division, Situation }

import lila.analyse.Analysis
import lila.game.Pov
import lila.insight.InsightPerfStats
import lila.rating.PerfType
import lila.user.User

case class Acpl(value: Double) extends AnyVal {
  def winningChances    = RelativeQuality(2 / (1 + Math.exp(-0.004 * value)) - 1)
  override def toString = value.toInt.toString
}
object Acpl {
  implicit val ordering = Ordering.by[Acpl, Double](_.value)
}
case class Rating(value: Double) extends AnyVal
object Rating {
  implicit val ordering = Ordering.by[Rating, Double](_.value)
}

case class ValueCount[V](value: V, count: Int) {
  def map[B](f: V => B) = copy(value = f(value))
}

case class TutorMetric[A](mine: ValueCount[A], peer: Option[ValueCount[A]])(implicit o: Ordering[A]) {
  def map[B: Ordering](f: A => B) = TutorMetric(mine map f, peer map (_ map f))
  def higher                      = peer.exists(p => o.compare(mine.value, p.value) >= 0)
  def toOption                    = TutorMetricOption(mine.some, peer)
}
case class TutorMetricOption[A](mine: Option[ValueCount[A]], peer: Option[ValueCount[A]])(implicit
    o: Ordering[A]
) {
  def map[B: Ordering](f: A => B) = TutorMetricOption(mine map (_ map f), peer map (_ map f))
  def higher                      = mine.exists(m => peer.exists(p => o.compare(m.value, p.value) >= 0))
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
case class RelativeQuality private (value: Double) extends AnyVal {

  import RelativeQuality.Wording

  def abs      = math.abs(value)
  def positive = value > 0

  def wording: Wording = value match {
    case v if v < -0.5  => Wording.MuchWorse
    case v if v < -0.2  => Wording.Worse
    case v if v < -0.05 => Wording.SlightlyWorse
    case v if v < 0.05  => Wording.Similar
    case v if v < 0.2   => Wording.SlightlyBetter
    case v if v < 0.5   => Wording.Better
    case _              => Wording.MuchBetter
  }
}

object RelativeQuality {
  def apply(a: Double, b: Double): RelativeQuality = apply((a / b) - 1)
  def apply(value: Double): RelativeQuality        = new RelativeQuality(value atLeast -1 atMost 1)

  sealed abstract class Wording(val value: String)
  object Wording {
    case object MuchWorse      extends Wording("much worse")
    case object Worse          extends Wording("worse")
    case object SlightlyWorse  extends Wording("slightly worse")
    case object Similar        extends Wording("similar")
    case object SlightlyBetter extends Wording("slightly better")
    case object Better         extends Wording("better")
    case object MuchBetter     extends Wording("much better")
  }
}

case class TutorUser(user: User, perfType: PerfType, perfStats: InsightPerfStats)
