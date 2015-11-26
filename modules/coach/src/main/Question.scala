package lila.coach

case class Question[X](
  dimension: Dimension[X],
  metric: Metric,
  filters: List[Filter[_]])

case class Filter[A](
    dimension: Dimension[A],
    selected: List[A]) {

  import reactivemongo.bson._

  def matcher: BSONDocument = selected map dimension.bson.write match {
    case Nil     => BSONDocument()
    case List(x) => BSONDocument(dimension.dbKey -> x)
    case xs      => BSONDocument(dimension.dbKey -> BSONDocument("$or" -> BSONArray(xs)))
  }
}

case class Answer[X](
  question: Question[X],
  clusters: List[Cluster[X]])

case class Cluster[X](
    x: X,
    data: Point,
    size: Point) {

  def points = List(data, size)
}

sealed trait Point {
  val name: String
  val y: Double
  val isSize: Boolean
  lazy val key = s"$name$isSize"
}
object Point {
  case class Data(name: String, y: Double) extends Point {
    val isSize = false
  }
  case class Size(name: String, y: Double) extends Point {
    val isSize = true
  }
}
