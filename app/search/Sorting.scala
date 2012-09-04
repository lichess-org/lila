package lila
package search

import org.elasticsearch.search.sort._, SortBuilders._
import com.traackr.scalastic.elasticsearch.SearchParameterTypes.FieldSort

case class Sorting(field: String, order: String) {

  def fieldSort = if (Sorting.fields contains field)
    Sorting.orders find (_.toString == order) map { order ⇒
      FieldSort(field, order = order)
    }
  else none
}

object Sorting {

  val fields = List("date", "turns")

  val orders = List(SortOrder.ASC, SortOrder.DESC)

  val default = Sorting("date", "desc")
}
