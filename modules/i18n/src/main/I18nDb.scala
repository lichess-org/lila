package lila.i18n

import play.api.i18n.Lang

object I18nDb {

  sealed trait Ref
  case object Site extends Ref
  case object Arena extends Ref
  case object Emails extends Ref

  val site: Messages = lila.i18n.db.site.Registry.load
  val arena: Messages = lila.i18n.db.arena.Registry.load
  val emails: Messages = lila.i18n.db.emails.Registry.load

  def apply(ref: Ref): Messages = ref match {
    case Site => site
    case Arena => arena
    case Emails => emails
  }

  val langs = site.keySet
}
