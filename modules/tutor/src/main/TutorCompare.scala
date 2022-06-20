package lila.tutor

import lila.common.Heapsort
import lila.insight.{ InsightDimension, Metric }

case class TutorCompare[D, V: TutorCompare.Comparable](
    metric: Metric,
    points: List[(D, TutorMetricOption[V])]
) {
  import TutorCompare._

  val totalCountMine = points.map(_._2.mine.??(_.count)).sum

  val dimComparisons = {
    val myPoints: List[((D, ValueCount[V]), Int)] = points.collect {
      case (dim, TutorMetricOption(Some(mine), _)) if mine.relevantTo(totalCountMine) =>
        dim -> mine
    }.zipWithIndex

    for {
      ((dim1, met1), i1) <- myPoints
      ((dim2, met2), i2) <- myPoints.filter(_._2 > i1)
    } yield Comparison(dim1, metric, met1, OtherDim(dim2, met2))
  }

  val peerComparisons = points.collect {
    case (dim, TutorMetricOption(Some(mine), Some(peer)))
        if mine.relevantTo(totalCountMine) && peer.reliableEnough =>
      Comparison(dim, metric, mine, Peers[D, V](peer))
  }

  def comparisons: List[Comparison[D, V]] = dimComparisons ::: peerComparisons
  def highlights(nb: Int)                 = Heapsort.topNToList(comparisons, nb, comparisonOrdering)

  def dimensionHighlights(nb: Int) = Heapsort.topNToList(dimComparisons, nb, comparisonOrdering)
  def peerHighlights(nb: Int)      = Heapsort.topNToList(peerComparisons, nb, comparisonOrdering)
}

object TutorCompare {

  case class Comparison[D, V: Comparable](
      dimension: D,
      metric: Metric,
      value: ValueCount[V],
      reference: Reference[D, V]
  ) {
    val comparison = valueCountIsComparable[V].compare(value, reference.value)

    override def toString = s"(${comparison.value}) $dimension $metric $value vs $reference"
  }

  private[tutor] val comparisonOrdering = Ordering.by[Comparison[_, _], Double](_.comparison.abs)

  sealed trait Reference[D, V] { val value: ValueCount[V] }
  case class Peers[D, V](value: ValueCount[V])                  extends Reference[D, V]
  case class OtherDim[D, V](dimension: D, value: ValueCount[V]) extends Reference[D, V]

  trait Comparable[V] {
    def compare(a: V, b: V): ValueComparison
  }

  implicit val ratioIsComparable = new Comparable[TutorRatio] {
    def compare(a: TutorRatio, b: TutorRatio) = ValueComparison(a.value, b.value)
  }
  implicit val doubleIsComparable = new Comparable[Double] {
    def compare(a: Double, b: Double) = ValueComparison(a, b)
  }
  implicit val acplIsComparable = new Comparable[Acpl] {
    def compare(a: Acpl, b: Acpl) = ValueComparison(-a.value, -b.value)
  }
  implicit val ratingIsComparable = new Comparable[Rating] {
    def compare(a: Rating, b: Rating) = ValueComparison((a.value - b.value) / 300)
  }
  implicit def valueCountIsComparable[V](implicit by: Comparable[V]) = new Comparable[ValueCount[V]] {
    def compare(a: ValueCount[V], b: ValueCount[V]) = by.compare(a.value, b.value)
  }

  def higherIsBetter[M](metric: Metric) = metric match {
    case Metric.MeanCpl => false
    case _              => true
  }
}
