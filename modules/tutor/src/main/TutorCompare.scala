package lila.tutor

import lila.common.Heapsort.implicits._
import lila.insight.{ InsightDimension, InsightMetric }

case class TutorCompare[D, V](
    dimensionType: InsightDimension[D],
    metric: TutorMetric[V],
    points: List[(D, TutorBothValueOptions[V])]
)(implicit number: TutorNumber[V]) {
  import TutorCompare._
  import TutorNumber._

  val totalCountMine = points.map(_._2.mine.??(_.count)).sum

  lazy val dimensionComparisons: List[AnyComparison] = {
    val myPoints: List[(D, ValueCount[V])] =
      points.collect { case (dim, TutorBothValueOptions(Some(mine), _)) => dim -> mine }
    for {
      (dim1, met1) <- myPoints.filter(_._2 relevantTo totalCountMine)
      avg = number.mean(myPoints.filter(_._1 != dim1).map(_._2))
    } yield Comparison(dimensionType, dim1, metric, met1, DimAvg(avg))
  }

  lazy val peerComparisons: List[AnyComparison] = points.collect {
    case (dim, TutorBothValueOptions(Some(mine), Some(peer)))
        if mine.relevantTo(totalCountMine) && peer.reliableEnough =>
      Comparison(dimensionType, dim, metric, mine, Peers(peer))
  }

  def allComparisons: List[AnyComparison] = dimensionComparisons ::: peerComparisons
}

object TutorCompare {

  case class Comparison[D, V](
      dimensionType: InsightDimension[D],
      dimension: D,
      metric: TutorMetric[V],
      value: ValueCount[V],
      reference: Reference[V]
  )(implicit number: TutorNumber[V]) {

    val grade = number.grade(value.value, reference.value.value)

    val importance = grade.value * math.sqrt(value.count)

    def better                          = grade.better
    def worse                           = grade.worse
    def similarTo(other: AnyComparison) = other.dimensionType == dimensionType && other.metric == metric

    override def toString = s"(${grade.value}) $dimensionType $metric ${value.value} vs $reference"
  }

  implicit val comparisonOrdering: Ordering[AnyComparison] =
    Ordering.by[AnyComparison, Double](_.importance.abs)

  type AnyComparison = Comparison[_, _]
  type AnyCompare    = TutorCompare[_, _]

  def mixedBag(comparisons: List[AnyComparison])(nb: Int): List[AnyComparison] = {
    val half = ~lila.common.Maths.divideRoundUp(nb, 2)
    comparisons.partition(_.better) match {
      case (positives, negatives) => positives.topN(half) ::: negatives.topN(half)
    }
  } sorted comparisonOrdering.reverse take nb

  def sortAndPreventRepetitions(comparisons: List[AnyComparison])(nb: Int): List[AnyComparison] =
    comparisons
      .sorted(comparisonOrdering.reverse)
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
}
