package lila.insight

case class Question[X](
    dimension: Dimension[X],
    metric: Metric,
    filters: List[Filter[_]]
)

case class Filter[A](
    dimension: Dimension[A],
    selected: List[A]
) {

  def isEmpty = selected.isEmpty || selected.size == Dimension.valuesOf(dimension).size

  import reactivemongo.bson._

  def matcher: BSONDocument = Dimension.filtersOf(dimension, selected)
}
