package lila
package search

import org.elasticsearch.search.sort._, SortBuilders._
import com.traackr.scalastic.elasticsearch.SearchParameterTypes.FieldSort

case class Sorting(field: String, order: String) {

  def fieldSort = FieldSort(
    field = (Sorting.fieldKeys contains field).fold(field, Sorting.default.field),
    order = (order.toLowerCase == "asc").fold(SortOrder.ASC, SortOrder.DESC)
  )
}

object Sorting {

  val fields = List(
    Game.fields.date -> "Date",
    Game.fields.turns -> "Turns",
    Game.fields.averageElo -> "Average ELO")

  def fieldKeys = fields map (_._1)

  val orders = List(SortOrder.ASC, SortOrder.DESC) map { s ⇒ s.toString -> s.toString }

  val default = Sorting(Game.fields.date, "desc")
}
