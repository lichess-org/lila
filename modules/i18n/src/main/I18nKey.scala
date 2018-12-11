package lila.i18n

import play.twirl.api.Html

import lila.common.Lang

sealed trait I18nKey {

  val key: String

  def literalHtmlTo(lang: Lang, args: Seq[Any] = Seq.empty): Html

  def pluralHtmlTo(lang: Lang, count: Count, args: Seq[Any] = Nil): Html

  def literalTxtTo(lang: Lang, args: Seq[Any] = Seq.empty): String

  def pluralTxtTo(lang: Lang, count: Count, args: Seq[Any] = Nil): String

  /* Implicit context convenience functions */

  // literal
  def apply(args: Any*)(implicit lang: Lang): Html = literalHtmlTo(lang, args)

  def plural(count: Count, args: Any*)(implicit lang: Lang): Html = pluralHtmlTo(lang, count, args)

  // literalTxt
  def txt(args: Any*)(implicit lang: Lang): String = literalTxtTo(lang, args)

  def pluralTxt(count: Count, args: Any*)(implicit lang: Lang): String = pluralTxtTo(lang, count, args)

  // reuses the count as the single argument
  // allows `plural(nb)` instead of `plural(nb, nb)`
  def pluralSame(count: Int)(implicit lang: Lang): Html = plural(count, count)
  def pluralSameTxt(count: Int)(implicit lang: Lang): String = pluralTxt(count, count)
}

final class Translated(val key: String, val db: I18nDb.Ref) extends I18nKey {

  def literalHtmlTo(lang: Lang, args: Seq[Any] = Nil): Html =
    Translator.html.literal(key, db, args, lang)

  def pluralHtmlTo(lang: Lang, count: Count, args: Seq[Any] = Nil): Html =
    Translator.html.plural(key, db, count, args, lang)

  def literalTxtTo(lang: Lang, args: Seq[Any] = Nil): String =
    Translator.txt.literal(key, db, args, lang)

  def pluralTxtTo(lang: Lang, count: Count, args: Seq[Any] = Nil): String =
    Translator.txt.plural(key, db, count, args, lang)
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
