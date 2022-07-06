package lila.insight

case class Question[X](
    dimension: InsightDimension[X],
    metric: InsightMetric,
    filters: List[Filter[_]] = Nil
) {
  def filter(f: Filter[_]) = copy(filters = f :: filters)

  def withMetric(m: InsightMetric) = copy(metric = m)

  def monKey = s"${dimension.key}_${metric.key}"
}

object Question {

  case class Peers(rating: MeanRating) {
    def ratingRange = {
      // based on https://lichess.org/stat/rating/distribution/blitz
      val diff = Math.ceil(0.0002647 * Math.pow(rating.value, 2) - 0.80735 * rating.value + 635.4411).toInt
      Range(rating.value - diff, rating.value + diff)
    }
  }
}

case class Filter[A](
    dimension: InsightDimension[A],
    selected: List[A]
) {

  def isEmpty = selected.isEmpty || selected.sizeIs == InsightDimension.valuesOf(dimension).size

  import reactivemongo.api.bson._

  def matcher: BSONDocument = InsightDimension.filtersOf(dimension, selected)
}
