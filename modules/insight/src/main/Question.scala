package lila.insight

case class Question[X](
    dimension: InsightDimension[X],
    metric: Metric,
    filters: List[Filter[_]]
) {
  def add(filter: Filter[_]) = copy(filters = filter :: filters)
}

object Question {

  case class Peers(rating: MeanRating) {
    def ratingRange = Range(rating.value - 30, rating.value + 30) // TODO: expand at extremities
    // 0.0002647 x ^(2) - 0.80735 x +635.4411
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
