package lila.i18n

import play.api.i18n.Lang

object I18nDb {

  sealed trait Ref
  case object Site extends Ref
  case object Arena extends Ref

  val site: Messages = lila.i18n.db.site.Registry.load
  val arena: Messages = lila.i18n.db.arena.Registry.load

  def apply(ref: Ref): Messages = ref match {
    case Site => site
    case Arena => arena
  }

  val langs = site.keySet
}
