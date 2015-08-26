package lila.gameSearch

import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.search.sort.SortOrder

case class Sorting(f: String, order: String) {

  def definition =
    field sort (Sorting.fieldKeys contains f).fold(f, Sorting.default.f) order
      (order.toLowerCase == "asc").fold(SortOrder.ASC, SortOrder.DESC)
}

object Sorting {

  val fields = List(
    Fields.date -> "Date",
    Fields.turns -> "Moves",
    Fields.averageRating -> "Rating")

  def fieldKeys = fields map (_._1)

  val orders = List(SortOrder.DESC, SortOrder.ASC) map { s => s.toString -> s.toString }

  val default = Sorting(Fields.date, "desc")
}
