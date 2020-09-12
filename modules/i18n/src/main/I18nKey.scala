package lila.i18n

import play.api.i18n.Lang
import scalatags.Text.RawFrag

final class I18nKey(val key: String) {

  def apply(args: Any*)(implicit lang: Lang): RawFrag =
    Translator.frag.literal(key, args, lang)

  def plural(count: Count, args: Any*)(implicit lang: Lang): RawFrag =
    Translator.frag.plural(key, count, args, lang)

  def pluralSame(count: Int)(implicit lang: Lang): RawFrag = plural(count, count)

  def txt(args: Any*)(implicit lang: Lang): String =
    Translator.txt.literal(key, args, lang)

  def pluralTxt(count: Count, args: Any*)(implicit lang: Lang): String =
    Translator.txt.plural(key, count, args, lang)

  def pluralSameTxt(count: Int)(implicit lang: Lang): String = pluralTxt(count, count)
}

object I18nKey {

  type Select = I18nKeys.type => I18nKey
}
