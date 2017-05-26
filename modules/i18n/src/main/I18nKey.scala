package lila.i18n

import play.api.i18n.Lang
import play.twirl.api.Html
import lila.user.UserContext

sealed trait I18nKey {

  val key: String

  def apply(args: Any*)(implicit ctx: UserContext): Html

  def str(args: Any*)(implicit ctx: UserContext): String

  def to(lang: Lang)(args: Any*): String

  def en(args: Any*): String = to(enLang)(args: _*)
}

final class Translated(val key: String) extends I18nKey {

  def apply(args: Any*)(implicit ctx: UserContext): Html =
    Translator.html(key, args.toList, ctx.lang)

  def str(args: Any*)(implicit ctx: UserContext): String =
    Translator.str(key, args.toList, ctx.lang)

  def to(lang: Lang)(args: Any*): String =
    Translator.transTo(key, args.toList, lang)
}

final class Untranslated(val key: String) extends I18nKey {

  def apply(args: Any*)(implicit ctx: UserContext) = Html(key)

  def str(args: Any*)(implicit ctx: UserContext) = key

  def to(lang: Lang)(args: Any*) = key
}

object I18nKey {

  type Select = I18nKeys.type => I18nKey
}
