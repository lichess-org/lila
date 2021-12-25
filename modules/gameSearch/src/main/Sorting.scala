package lila.gameSearch

import play.api.i18n.Lang
import lila.i18n.{ I18nKeys => trans }

case class Sorting(f: String, order: String)

object Sorting {

  def fields(implicit lang: Lang) = List(
    Fields.date          -> trans.search.date.txt(),
    Fields.turns         -> trans.search.nbTurns.txt(),
    Fields.averageRating -> trans.rating.txt()
  )

  def orders(implicit lang: Lang) = List(
    "desc" -> trans.search.descending.txt(),
    "asc"  -> trans.search.ascending.txt()
  )

  val default = Sorting(Fields.date, "desc")
}
