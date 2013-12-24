package lila.i18n

import play.api.i18n.Lang
import play.api.templates.Html

trait I18nKey {

  val key: String

  def apply(args: Any*)(implicit ctx: lila.user.Context): Html

  def str(args: Any*)(implicit ctx: lila.user.Context): String

  def to(lang: Lang)(args: Any*): String

  def en(args: Any*): String = to(I18nKey.en)(args:_ *)
}

case class Untranslated(key: String) extends I18nKey {

  def apply(args: Any*)(implicit ctx: lila.user.Context) = Html(key)

  def str(args: Any*)(implicit ctx: lila.user.Context) = key

  def to(lang: Lang)(args: Any*) = key
}

object I18nKey {

  val en = Lang("en")

  type Select = I18nKeys ⇒ I18nKey

  def untranslated(key: String) = Untranslated(key)
}
