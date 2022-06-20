package lila.tutor

import lila.common.Heapsort
import lila.insight.{ InsightDimension, Metric }

case class TutorCompare[D, V: TutorCompare.Qualifiable](
    dimensionType: InsightDimension[D],
    metricType: Metric,
    points: List[(D, TutorMetricOption[V])]
) {
  import TutorCompare._

  val dimComparisons = {
    val myPoints = points.collect { case (dim, TutorMetricOption(Some(mine), _)) =>
      dim -> mine
    }.zipWithIndex
    for {
      ((dim1, met1), i1) <- myPoints
      ((dim2, met2), i2) <- myPoints.filter(_._2 > i1)
    } yield Comparison(dimensionType, dim1, metricType, met1, OtherDim(dim2, met2))
  }

  val peerComparisons = points.collect { case (dim, TutorMetricOption(Some(mine), Some(peer))) =>
    Comparison(dimensionType, dim, metricType, mine, Peers[D, V](peer))
  }

  def comparisons: List[Comparison[D, V]] = dimComparisons ::: peerComparisons
  def highlights(nb: Int)                 = Heapsort.topNToList(comparisons, nb, comparisonOrdering)

  def dimensionHighlights(nb: Int) = Heapsort.topNToList(dimComparisons, nb, comparisonOrdering)
  def peerHighlights(nb: Int)      = Heapsort.topNToList(peerComparisons, nb, comparisonOrdering)
}

object TutorCompare {

  case class Comparison[D, V: Qualifiable](
      dimensionType: InsightDimension[D],
      dimension: D,
      metricType: Metric,
      value: V,
      reference: Reference[D, V]
  ) {
    val quality = implicitly[Qualifiable[V]].quality(value, reference.value)

    override def toString = s"(${quality.value}) $dimension $metricType $value vs $reference"
  }

  private[tutor] val comparisonOrdering = Ordering.by[Comparison[_, _], Double](_.quality.abs)

  sealed trait Reference[D, V] { val value: V }
  case class Peers[D, V](value: V)                  extends Reference[D, V]
  case class OtherDim[D, V](dimension: D, value: V) extends Reference[D, V]

  trait Qualifiable[V] {
    def quality(a: V, b: V): RelativeQuality
  }

  implicit val ratioIsQualifiable = new Qualifiable[TutorRatio] {
    def quality(a: TutorRatio, b: TutorRatio) = RelativeQuality(a.value, b.value)
  }
  implicit val doubleIsQualifiable = new Qualifiable[Double] {
    def quality(a: Double, b: Double) = RelativeQuality(a, b)
  }
  implicit val acplIsQualifiable = new Qualifiable[Acpl] {
    def quality(a: Acpl, b: Acpl) = RelativeQuality(-a.value, -b.value)
  }

  def higherIsBetter[M](metric: Metric) = metric match {
    case Metric.MeanCpl => false
    case _              => true
  }
}
