package lila.gameSearch

case class Sorting(f: String, order: String)

object Sorting {

  val fields = List(
    Fields.date -> "Date",
    Fields.turns -> "Moves",
    Fields.averageRating -> "Rating")

  val orders = List("desc", "asc") map { s => s -> s }

  val default = Sorting(Fields.date, "desc")
}

case class Clocking(
    initMin: Option[Int] = None,
    initMax: Option[Int] = None,
    incMin: Option[Int] = None,
    incMax: Option[Int] = None) {

  def nonEmpty = initMin.isDefined || initMax.isDefined || incMin.isDefined || incMax.isDefined
}
