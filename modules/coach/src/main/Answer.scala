package lila.coach

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
