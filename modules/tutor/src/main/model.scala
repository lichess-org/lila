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

case class TutorMetric[A](mine: A, peer: Option[A])(implicit o: Ordering[A]) {
  def higher = peer.exists(p => o.compare(mine, p) >= 0)
}
case class TutorMetricOption[A](mine: Option[A], peer: Option[A])(implicit o: Ordering[A]) {
  def higher                      = mine.exists(m => peer.exists(p => o.compare(m, p) >= 0))
  def map[B: Ordering](f: A => B) = TutorMetricOption(mine map f, peer map f)
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

  def abs = math.abs(value)

  def wording: Wording = value match {
    case v if v < -0.4 => Wording.MuchWorse
    case v if v < -0.2 => Wording.QuiteWorse
    case v if v < -0.1 => Wording.SlightlyWorse
    case v if v < 0.1  => Wording.Equal
    case v if v < 0.2  => Wording.SlightlyBetter
    case v if v < 0.4  => Wording.QuiteBetter
    case _             => Wording.MuchBetter
  }
}

object RelativeQuality {
  def apply(a: Double, b: Double): RelativeQuality = apply((a / b) - 1)
  def apply(value: Double): RelativeQuality        = new RelativeQuality(value atLeast -1 atMost 1)

  sealed trait Wording
  object Wording {
    case object MuchWorse      extends Wording
    case object QuiteWorse     extends Wording
    case object SlightlyWorse  extends Wording
    case object Equal          extends Wording
    case object SlightlyBetter extends Wording
    case object QuiteBetter    extends Wording
    case object MuchBetter     extends Wording
  }
}

case class TutorUser(user: User, perfType: PerfType, perfStats: InsightPerfStats)
