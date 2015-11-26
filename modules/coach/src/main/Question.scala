package lila.coach

case class Question[X](
  dimension: Dimension[X],
  metric: Metric,
  filters: List[Filter[_]])

case class Filter[A](
    dimension: Dimension[A],
    selected: List[A]) {

  def isEmpty = selected.isEmpty || selected.size == Dimension.valuesOf(dimension).size

  import reactivemongo.bson._

  def matcher: BSONDocument = selected map dimension.bson.write match {
    case Nil     => BSONDocument()
    case List(x) => BSONDocument(dimension.dbKey -> x)
    case xs      => BSONDocument(dimension.dbKey -> BSONDocument("$in" -> BSONArray(xs)))
  }
}
