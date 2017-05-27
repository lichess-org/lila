package lila.i18n

import play.api.i18n.Lang
import play.twirl.api.Html
import lila.user.UserContext

sealed trait I18nKey {

  val key: String

  def literalTo(lang: Lang, args: Any*): String

  def pluralTo(lang: Lang, count: Count, args: Any*): String

  /* Implicit context convenience functions */

  def literalStr(args: Any*)(implicit ctx: UserContext): String = literalTo(ctx.lang, args)

  def pluralStr(count: Count, args: Any*)(implicit ctx: UserContext): String = pluralTo(ctx.lang, count, args)

  def literal(args: Any*)(implicit ctx: UserContext): Html = Html(literalTo(ctx.lang, args))

  def plural(count: Count, args: Any*)(implicit ctx: UserContext): Html = Html(pluralTo(ctx.lang, count, args))

  /* Shortcuts */

  def apply()(implicit ctx: UserContext): Html = literal()

  def str()(implicit ctx: UserContext): String = literalStr()

  // reuses the count as the single argument
  // allows `plural(nb)` instead of `plural(nb, nb)`
  def pluralSame(count: Int)(implicit ctx: UserContext): Html = plural(count, count)
  def pluralSameStr(count: Int)(implicit ctx: UserContext): String = pluralStr(count, count)

  /* English translations */

  def literalEn(args: Any*): String = literalTo(enLang, args: _*)
  def pluralEn(count: Count, args: Any*): String = pluralTo(enLang, count, args: _*)
}

final class Translated(val key: String) extends I18nKey {

  def literalTo(lang: Lang, args: Any*): String =
    Translator.literal(key, args, lang)

  def pluralTo(lang: Lang, count: Count, args: Any*): String =
    Translator.plural(key, count, args, lang)
}

final class Untranslated(val key: String) extends I18nKey {

  def literalTo(lang: Lang, args: Any*) = key

  def pluralTo(lang: Lang, count: Count, args: Any*) = key
}

object I18nKey {

  type Select = I18nKeys.type => I18nKey
}
