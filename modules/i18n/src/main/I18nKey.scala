package lila.i18n

import play.api.i18n.Lang
import scalatags.Text.RawFrag

final class I18nKey(val key: String, val db: I18nDb.Ref) {

  def apply(args: Any*)(implicit lang: Lang): RawFrag =
    Translator.frag.literal(key, db, args, lang)

  def plural(count: Count, args: Any*)(implicit lang: Lang): RawFrag =
    Translator.frag.plural(key, db, count, args, lang)

  def pluralSame(count: Int)(implicit lang: Lang): RawFrag = plural(count, count)

  def txt(args: Any*)(implicit lang: Lang): String =
    Translator.txt.literal(key, db, args, lang)

  def pluralTxt(count: Count, args: Any*)(implicit lang: Lang): String =
    Translator.txt.plural(key, db, count, args, lang)

  def pluralSameTxt(count: Int)(implicit lang: Lang): String = pluralTxt(count, count)
}

object I18nKey {

  type Select = I18nKeys.type => I18nKey
}
