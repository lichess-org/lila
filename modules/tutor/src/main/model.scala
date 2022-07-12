package lila.tutor

import alleycats.Zero
import chess.{ Division, Situation }

import lila.analyse.AccuracyPercent
import lila.analyse.Analysis
import lila.game.Pov
import lila.insight.{ ClockPercent, InsightMetric, InsightPerfStats, MeanRating }
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

  def double(implicit number: TutorNumber[V]) = ValueCount[Double](number double value, count)
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

  def mix(other: TutorBothValueOptions[A])(implicit number: TutorNumber[A]): TutorBothValueOptions[A] =
    TutorBothValueOptions(
      mine = number.mean(mine, other.mine).some.filter(_.count > 0),
      peer = number.mean(peer, other.peer).some.filter(_.count > 0)
    )

}

sealed abstract class TutorMetric[V](val metric: InsightMetric)

object TutorMetric {
  case object GlobalClock extends TutorMetric[ClockPercent](InsightMetric.ClockPercent)
  // time used when losing ((100 - clockPercent) on last move)
  case object ClockUsage  extends TutorMetric[ClockPercent](InsightMetric.ClockPercent)
  case object Accuracy    extends TutorMetric[AccuracyPercent](InsightMetric.MeanAccuracy)
  case object Awareness   extends TutorMetric[GoodPercent](InsightMetric.Awareness)
  case object Performance extends TutorMetric[Rating](InsightMetric.Performance)
}

// higher is better
case class GoodPercent(value: Double) extends AnyVal with Percent

object GoodPercent {
  def apply(a: Double, b: Double): GoodPercent = GoodPercent(100 * a / b)
  implicit val ordering                        = Ordering.by[GoodPercent, Double](_.value)
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
  def percent(a: Percent, b: Percent): Grade = apply((a.value - b.value) / 25)
  def apply(value: Double): Grade            = new Grade(value atLeast -1 atMost 1)

  sealed abstract class Wording(val id: Int, val value: String, val top: Double) extends Ordered[Wording] {
    def compare(other: Wording) = top compare other.top
  }
  object Wording {
    case object MuchBetter     extends Wording(7, "much better than", 1)
    case object Better         extends Wording(6, "better than", 0.6)
    case object SlightlyBetter extends Wording(5, "slightly better than", 0.3)
    case object Similar        extends Wording(4, "similar to", 0.1)
    case object SlightlyWorse  extends Wording(3, "slightly worse than", -Similar.top)
    case object Worse          extends Wording(2, "worse than", -SlightlyBetter.top)
    case object MuchWorse      extends Wording(1, "much worse than", -Better.top)
    val list = List[Wording](MuchWorse, Worse, SlightlyWorse, Similar, SlightlyBetter, Better, MuchBetter)
  }
}

case class TutorUser(user: User, perfType: PerfType, perfStats: InsightPerfStats)
