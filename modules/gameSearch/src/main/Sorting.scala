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
