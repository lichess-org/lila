package lila.gameSearch

import play.api.i18n.Lang

import lila.core.i18n.{ Translate, I18nKey as trans }

case class Sorting(f: String, order: String)

object Sorting:

  def fields(using Translate) = List(
    Fields.date          -> trans.search.date.txt(),
    Fields.turns         -> trans.search.nbTurns.txt(),
    Fields.averageRating -> trans.site.rating.txt()
  )

  def orders(using Translate) = List(
    "desc" -> trans.search.descending.txt(),
    "asc"  -> trans.search.ascending.txt()
  )

  val default = Sorting(Fields.date, "desc")
