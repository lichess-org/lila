package lila.tutor

import lila.common.Heapsort
import lila.insight.{ InsightDimension, Metric }

case class TutorCompare[D, V](
    dimensionType: InsightDimension[D],
    metric: Metric,
    points: List[(D, TutorMetricOption[V])]
)(implicit number: TutorNumber[V]) {
  import TutorCompare._
  import TutorNumber._

  val totalCountMine = points.map(_._2.mine.??(_.count)).sum

  val dimComparisons = {
    val myPoints: List[(D, ValueCount[V])] =
      points.collect { case (dim, TutorMetricOption(Some(mine), _)) => dim -> mine }

    for {
      (dim1, met1) <- myPoints.filter(_._2 relevantTo totalCountMine)
      avg = number.average(myPoints.filter(_._1 != dim1).map(_._2))
    } yield Comparison(dimensionType, dim1, metric, met1, DimAvg(avg))
  }

  val peerComparisons = points.collect {
    case (dim, TutorMetricOption(Some(mine), Some(peer)))
        if mine.relevantTo(totalCountMine) && peer.reliableEnough =>
      Comparison(dimensionType, dim, metric, mine, Peers(peer))
  }

  def comparisons: List[Comparison[D, V]] = dimComparisons ::: peerComparisons
  def highlights(nb: Int)                 = Heapsort.topNToList(comparisons, nb, comparisonOrdering)

  def dimensionHighlights(nb: Int) = Heapsort.topNToList(dimComparisons, nb, comparisonOrdering)
  def peerHighlights(nb: Int)      = Heapsort.topNToList(peerComparisons, nb, comparisonOrdering)
}

object TutorCompare {

  case class Comparison[D, V](
      dimensionType: InsightDimension[D],
      dimension: D,
      metric: Metric,
      value: ValueCount[V],
      reference: Reference[V]
  )(implicit number: TutorNumber[V]) {

    val comparison = number.compare(value.value, reference.value.value)

    override def toString = s"(${comparison.value}) $dimension $metric $value vs $reference"
  }

  private[tutor] val comparisonOrdering = Ordering.by[Comparison[_, _], Double](_.comparison.abs)

  sealed trait Reference[V] { val value: ValueCount[V] }
  case class Peers[V](value: ValueCount[V])  extends Reference[V]
  case class DimAvg[V](value: ValueCount[V]) extends Reference[V]
}
