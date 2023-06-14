package lila.tutor

import lila.common.Heapsort.topN
import lila.insight.InsightDimension

case class TutorCompare[D, V](
    dimensionType: InsightDimension[D],
    metric: TutorMetric[V],
    points: List[(D, TutorBothValueOptions[V])],
    color: Option[chess.Color] = None
)(using number: TutorNumber[V]):
  import TutorCompare.*
  import TutorNumber.*

  val totalCountMine = points.map(_._2.mine.so(_.count)).sum

  lazy val dimensionComparisons: List[AnyComparison] =
    val myPoints: List[(D, ValueCount[V])] =
      points.collect { case (dim, TutorBothValueOptions(Some(mine), _)) => dim -> mine }
    for {
      (dim1, met1) <- myPoints.filter(_._2 relevantTo totalCountMine)
      avg = number.mean(myPoints.filter(_._1 != dim1).map(_._2))
    } yield Comparison(dimensionType, dim1, metric, met1, DimAvg(avg), color)

  lazy val peerComparisons: List[AnyComparison] = points.collect {
    case (dim, TutorBothValueOptions(Some(mine), Some(peer)))
        if mine.relevantTo(totalCountMine) && peer.reliableEnough =>
      Comparison(dimensionType, dim, metric, mine, Peers(peer), color)
  }

  def allComparisons: List[AnyComparison] = dimensionComparisons ::: peerComparisons

  def as(color: chess.Color) = copy(color = color.some)

object TutorCompare:

  case class Comparison[D, V](
      dimensionType: InsightDimension[D],
      dimension: D,
      metric: TutorMetric[V],
      value: ValueCount[V],
      reference: Reference[V],
      color: Option[chess.Color] = None
  )(using number: TutorNumber[V]):

    val grade = number.grade(value.value, reference.value.value)

    val importance = grade.value * math.sqrt(value.count)

    def better                          = grade.better
    def worse                           = grade.worse
    def similarTo(other: AnyComparison) = other.dimensionType == dimensionType && other.metric == metric

    override def toString = s"(${grade.value}) $dimensionType $metric ${value.value} vs $reference"

  given compOrder: Ordering[AnyComparison] = Ordering.by(_.importance.abs)

  type AnyComparison = Comparison[?, ?]
  type AnyCompare    = TutorCompare[?, ?]

  def mixedBag(comparisons: List[AnyComparison])(nb: Int): List[AnyComparison] = {
    val half = ~lila.common.Maths.divideRoundUp(nb, 2)
    comparisons.partition(_.better) match
      case (positives, negatives) => positives.topN(half) ::: negatives.topN(half)
  } sorted compOrder.reverse take nb

  def sortAndPreventRepetitions(comparisons: List[AnyComparison])(nb: Int): List[AnyComparison] =
    comparisons
      .sorted(compOrder.reverse)
      .foldLeft(Vector.empty[AnyComparison]) {
        case (Vector(), c)                         => Vector(c)
        case (acc, _) if acc.size >= nb            => acc
        case (acc, c) if acc.exists(_ similarTo c) => acc
        case (acc, c)                              => acc :+ c
      }
      .toList

  sealed trait Reference[V] { val value: ValueCount[V] }
  case class Peers[V](value: ValueCount[V])  extends Reference[V]
  case class DimAvg[V](value: ValueCount[V]) extends Reference[V]
