package lila.gameSearch

import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.search.sort.SortOrder

case class Sorting(field: String, order: String) {

  def definition =
    by field (Sorting.fieldKeys contains field).fold(field, Sorting.default.field) order
      (order.toLowerCase == "asc").fold(SortOrder.ASC, SortOrder.DESC)
}

object Sorting {

  val fields = List(
    Fields.date -> "Date",
    Fields.turns -> "Moves",
    Fields.averageRating -> "Average Rating")

  def fieldKeys = fields map (_._1)

  val orders = List(SortOrder.DESC, SortOrder.ASC) map { s => s.toString -> s.toString }

  val default = Sorting(Fields.date, "desc")
}
