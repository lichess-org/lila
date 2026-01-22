package lila.tutor

import alleycats.Zero
import scalalib.model.Percent
import chess.IntRating

import lila.analyse.AccuracyPercent
import lila.insight.{ ClockPercent, InsightMetric, InsightPerfStats }
import lila.rating.PerfType

case class ValueCount[V](value: V, count: Int):
  def map[B](f: V => B) = copy(value = f(value))
  def reliableEnough = count >= 50
  def relevantTo(total: Int) = reliableEnough && count * 10 > total

  def double(using number: TutorNumber[V]) = ValueCount[Double](number.double(value), count)

case class TutorBothValuesAvailable[A](mine: ValueCount[A], peer: ValueCount[A])(using
    o: Ordering[A]
):
  // def map[B: Ordering](f: A => B)                           = TutorBothValuesAvailable(mine map f, peer map f)
  def higher = o.compare(mine.value, peer.value) >= 0
  def grade(using number: TutorNumber[A]): Grade = number.grade(mine.value, peer.value)

case class TutorBothValues[A](mine: ValueCount[A], peer: Option[ValueCount[A]])(using o: Ordering[A]):
  def map[B: Ordering](f: A => B) = TutorBothValues(mine.map(f), peer.map(_.map(f)))
  def higher = peer.exists(p => o.compare(mine.value, p.value) >= 0)
  def toOption = TutorBothValueOptions(mine.some, peer)

case class TutorBothValueOptions[A](mine: Option[ValueCount[A]], peer: Option[ValueCount[A]])(using
    o: Ordering[A]
):
  def map[B: Ordering](f: A => B) = TutorBothValueOptions(mine.map(_.map(f)), peer.map(_.map(f)))
  def higher = mine.exists(m => peer.exists(p => o.compare(m.value, p.value) >= 0))
  def asAvailable = for m <- mine; p <- peer yield TutorBothValuesAvailable(m, p)
  def grade(using TutorNumber[A]): Option[Grade] = asAvailable.map(_.grade)

  def mix(other: TutorBothValueOptions[A])(using number: TutorNumber[A]): TutorBothValueOptions[A] =
    TutorBothValueOptions(
      mine = number.mean(mine, other.mine).some.filter(_.count > 0),
      peer = number.mean(peer, other.peer).some.filter(_.count > 0)
    )
object TutorBothValueOptions:
  given zero[A: Ordering]: Zero[TutorBothValueOptions[A]] = Zero(TutorBothValueOptions[A](none, none))

enum TutorMetric[V](val metric: InsightMetric):
  case GlobalClock extends TutorMetric[ClockPercent](InsightMetric.ClockPercent)
  // time used when losing ((100 - clockPercent) on last move)
  case ClockUsage extends TutorMetric[ClockPercent](InsightMetric.ClockPercent)
  case Flagging extends TutorMetric[ClockPercent](InsightMetric.Termination)
  case Accuracy extends TutorMetric[AccuracyPercent](InsightMetric.MeanAccuracy)
  case Awareness extends TutorMetric[GoodPercent](InsightMetric.Awareness)
  case Performance extends TutorMetric[IntRating](InsightMetric.Performance)
  case Resourcefulness extends TutorMetric[GoodPercent](InsightMetric.Result)
  case Conversion extends TutorMetric[GoodPercent](InsightMetric.Result)

// higher is better
opaque type GoodPercent = Double
object GoodPercent extends OpaqueDouble[GoodPercent]:
  given Percent[GoodPercent] = Percent.of(GoodPercent)
  given lila.db.NoDbHandler[GoodPercent] with {}
  extension (a: GoodPercent) def toInt = Percent.toInt(a)
  def apply(a: Double, b: Double): GoodPercent = GoodPercent(100 * a / b)

// value from -1 (worse) to +1 (best)
case class Grade private (value: Double):
  import Grade.Wording
  def abs = math.abs(value)
  def better = wording >= Wording.SlightlyBetter
  def worse = wording <= Wording.SlightlyWorse
  def negate = copy(value = -value)
  val wording: Wording = Wording.list.find(_.top > value) | Wording.MuchBetter

object Grade:
  def percent[P](a: P, b: P)(using p: Percent[P]): Grade = apply((p.value(a) - p.value(b)) / 25)
  def apply(value: Double): Grade = new Grade(value.atLeast(-1).atMost(1))

  enum Wording(val id: Int, val value: String, val top: Double) extends Ordered[Wording]:
    def compare(other: Wording) = top.compare(other.top)
    case MuchBetter extends Wording(7, "much better than", 1)
    case Better extends Wording(6, "better than", 0.4)
    case SlightlyBetter extends Wording(5, "slightly better than", 0.2)
    case Similar extends Wording(4, "similar to", 0.07)
    case SlightlyWorse extends Wording(3, "slightly worse than", -Similar.top)
    case Worse extends Wording(2, "worse than", -SlightlyBetter.top)
    case MuchWorse extends Wording(1, "much worse than", -Better.top)
  object Wording:
    val list = values.reverse.toList

case class TutorPlayer(
    user: User,
    perfType: PerfType,
    perfStats: InsightPerfStats,
    peerMatch: Option[TutorPerfReport.PeerMatch]
)
