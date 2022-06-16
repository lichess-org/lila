package lila.insight

case class Question[X](
    dimension: InsightDimension[X],
    metric: Metric,
    filters: List[Filter[_]]
)

case class Filter[A](
    dimension: InsightDimension[A],
    selected: List[A]
) {

  def isEmpty = selected.isEmpty || selected.sizeIs == InsightDimension.valuesOf(dimension).size

  import reactivemongo.api.bson._

  def matcher: BSONDocument = InsightDimension.filtersOf(dimension, selected)
}
