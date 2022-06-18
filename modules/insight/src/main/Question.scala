package lila.insight

case class Question[X](
    dimension: InsightDimension[X],
    metric: Metric,
    filters: List[Filter[_]]
)

object Question {

  case class Peers(rating: Int) {
    def ratingRange = Range(rating - 30, rating + 30) // TODO: expand at extremities
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
