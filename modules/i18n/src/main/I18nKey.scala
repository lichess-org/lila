package lila.i18n

import play.api.i18n.Lang
import scalatags.Text.RawFrag

opaque type I18nKey = String

object I18nKey extends OpaqueString[I18nKey]:

  extension (key: I18nKey)

    def apply(args: Matchable*)(using lang: Lang): RawFrag =
      Translator.frag.literal(key, args, lang)

    def plural(count: Count, args: Matchable*)(using lang: Lang): RawFrag =
      Translator.frag.plural(key, count, args, lang)

    def pluralSame(count: Int)(using Lang): RawFrag = plural(count, count)

    def txt(args: Any*)(using lang: Lang): String =
      Translator.txt.literal(key, args, lang)

    def pluralTxt(count: Count, args: Any*)(using lang: Lang): String =
      Translator.txt.plural(key, count, args, lang)

    def pluralSameTxt(count: Int)(using Lang): String = pluralTxt(count, count)

  end extension
