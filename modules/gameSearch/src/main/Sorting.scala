package lila.gameSearch

import lila.db.api.SortOrder

case class Sorting(f: String, order: String) {

  // def definition =
  //   field sort (Sorting.fieldKeys contains f).fold(f, Sorting.default.f) order
  //     (order.toLowerCase == "asc").fold(SortOrder.Ascending, SortOrder.Descending)
}

object Sorting {

  val fields = List(
    Fields.date -> "Date",
    Fields.turns -> "Moves",
    Fields.averageRating -> "Average Rating")

  def fieldKeys = fields map (_._1)

  val orders = List(SortOrder.Descending, SortOrder.Ascending) map { s => s.toString -> s.toString }

  val default = Sorting(Fields.date, "desc")
}
