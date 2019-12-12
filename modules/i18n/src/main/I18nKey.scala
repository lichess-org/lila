package lila.i18n

import scalatags.Text.RawFrag

import lila.common.Lang

sealed trait I18nKey {

  val key: String

  def literalTo(lang: Lang, args: Seq[Any] = Seq.empty): RawFrag
  def pluralTo(lang: Lang, count: Count, args: Seq[Any] = Nil): RawFrag

  def literalTxtTo(lang: Lang, args: Seq[Any] = Seq.empty): String
  def pluralTxtTo(lang: Lang, count: Count, args: Seq[Any] = Nil): String

  /* Implicit context convenience functions */

  // frag
  def apply(args: Any*)(implicit lang: Lang): RawFrag = literalTo(lang, args)
  def plural(count: Count, args: Any*)(implicit lang: Lang): RawFrag = pluralTo(lang, count, args)
  def pluralSame(count: Int)(implicit lang: Lang): RawFrag = plural(count, count)

  // txt
  def txt(args: Any*)(implicit lang: Lang): String = literalTxtTo(lang, args)
  def pluralTxt(count: Count, args: Any*)(implicit lang: Lang): String = pluralTxtTo(lang, count, args)
  def pluralSameTxt(count: Int)(implicit lang: Lang): String = pluralTxt(count, count)
}

final class Translated(val key: String, val db: I18nDb.Ref) extends I18nKey {

  def literalTo(lang: Lang, args: Seq[Any] = Nil): RawFrag =
    Translator.frag.literal(key, db, args, lang)

  def pluralTo(lang: Lang, count: Count, args: Seq[Any] = Nil): RawFrag =
    Translator.frag.plural(key, db, count, args, lang)

  def literalTxtTo(lang: Lang, args: Seq[Any] = Nil): String =
    Translator.txt.literal(key, db, args, lang)

  def pluralTxtTo(lang: Lang, count: Count, args: Seq[Any] = Nil): String =
    Translator.txt.plural(key, db, count, args, lang)
}

final class Untranslated(val key: String) extends I18nKey {

  def literalTo(lang: Lang, args: Seq[Any]) = RawFrag(key)
  def pluralTo(lang: Lang, count: Count, args: Seq[Any]) = RawFrag(key)

  def literalTxtTo(lang: Lang, args: Seq[Any]) = key
  def pluralTxtTo(lang: Lang, count: Count, args: Seq[Any]) = key
}

object I18nKey {

  type Select = I18nKeys.type => I18nKey
}
