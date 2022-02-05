package lila.gameSearch

case class Sorting(f: String, order: String)

object Sorting {

  val fields = List(
    Fields.date          -> "Date",
    Fields.plies         -> "Moves",
    Fields.averageRating -> "Rating"
  )

  val orders = List(
    "desc" -> "Descending",
    "asc"  -> "Ascending"
  )

  val default = Sorting(Fields.date, "desc")
}
