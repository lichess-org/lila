package lila.i18n

import play.api.i18n.Lang
import play.twirl.api.Html
import lila.user.UserContext

sealed trait I18nKey {

  val key: String

  def literalHtmlTo(lang: Lang, args: Seq[Any]): Html

  def pluralHtmlTo(lang: Lang, count: Count, args: Seq[Any]): Html

  def literalTxtTo(lang: Lang, args: Seq[Any]): String

  def pluralTxtTo(lang: Lang, count: Count, args: Seq[Any]): String

  /* Implicit context convenience functions */

  def literal(args: Any*)(implicit ctx: UserContext): Html = literalHtmlTo(ctx.lang, args)

  def plural(count: Count, args: Any*)(implicit ctx: UserContext): Html = pluralHtmlTo(ctx.lang, count, args)

  def literalTxt(args: Any*)(implicit ctx: UserContext): String = literalTxtTo(ctx.lang, args)

  def pluralTxt(count: Count, args: Any*)(implicit ctx: UserContext): String = pluralTxtTo(ctx.lang, count, args)

  /* Shortcuts */

  def apply()(implicit ctx: UserContext): Html = literal()

  def txt()(implicit ctx: UserContext): String = literalTxt()

  // reuses the count as the single argument
  // allows `plural(nb)` instead of `plural(nb, nb)`
  def pluralSame(count: Int)(implicit ctx: UserContext): Html = plural(count, count)
  def pluralSameTxt(count: Int)(implicit ctx: UserContext): String = pluralTxt(count, count)

  /* English translations */

  // def literalEn(args: Any*): Html = literalHtmlTo(enLang, args)
  // def pluralEn(count: Count, args: Any*): Html = pluralHtmlTo(enLang, count, args)
}

final class Translated(val key: String) extends I18nKey {

  def literalHtmlTo(lang: Lang, args: Seq[Any]): Html =
    Translator.html.literal(key, args, lang)

  def pluralHtmlTo(lang: Lang, count: Count, args: Seq[Any]): Html =
    Translator.html.plural(key, count, args, lang)

  def literalTxtTo(lang: Lang, args: Seq[Any]): String =
    Translator.txt.literal(key, args, lang)

  def pluralTxtTo(lang: Lang, count: Count, args: Seq[Any]): String =
    Translator.txt.plural(key, count, args, lang)
}

final class Untranslated(val key: String) extends I18nKey {

  def literalHtmlTo(lang: Lang, args: Seq[Any]) = Html(key)

  def pluralHtmlTo(lang: Lang, count: Count, args: Seq[Any]) = Html(key)

  def literalTxtTo(lang: Lang, args: Seq[Any]) = key

  def pluralTxtTo(lang: Lang, count: Count, args: Seq[Any]) = key
}

object I18nKey {

  type Select = I18nKeys.type => I18nKey
}
