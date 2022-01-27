package lila.i18n

import scalatags.Text.all._
import play.api.i18n.Lang

import lila.common.String.html.escapeHtml

object Translator {

  object frag {
    def literal(key: MessageKey, args: Seq[Any], lang: Lang): RawFrag =
      translate(key, lang, I18nQuantity.Other /* grmbl */, args)

    def plural(key: MessageKey, count: Count, args: Seq[Any], lang: Lang): RawFrag =
      translate(key, lang, I18nQuantity(lang, count), args)

    private def translate(
        key: MessageKey,
        lang: Lang,
        quantity: I18nQuantity,
        args: Seq[Any]
    ): RawFrag =
      findTranslation(key, lang) flatMap { translation =>
        val htmlArgs = escapeArgs(args)
        try {
          translation match {
            case literal: Simple  => Some(literal.format(htmlArgs))
            case literal: Escaped => Some(literal.format(htmlArgs))
            case plurals: Plurals => plurals.format(quantity, htmlArgs)
          }
        } catch {
          case e: Exception =>
            logger.warn(s"Failed to format html $lang/$key -> $translation (${args.toList})", e)
            Some(RawFrag(key))
        }
      } getOrElse RawFrag(key)

    private def escapeArgs(args: Seq[Any]): Seq[RawFrag] =
      args.map {
        case s: String     => escapeHtml(s)
        case r: RawFrag    => r
        case f: StringFrag => RawFrag(f.render)
        case a             => RawFrag(a.toString)
      }
  }

  object txt {

    def literal(key: MessageKey, args: Seq[Any], lang: Lang): String =
      translate(key, lang, I18nQuantity.Other /* grmbl */, args)

    def plural(key: MessageKey, count: Count, args: Seq[Any], lang: Lang): String =
      translate(key, lang, I18nQuantity(lang, count), args)

    private def translate(
        key: MessageKey,
        lang: Lang,
        quantity: I18nQuantity,
        args: Seq[Any]
    ): String =
      findTranslation(key, lang) flatMap { translation =>
        try {
          translation match {
            case literal: Simple  => Some(literal.formatTxt(args))
            case literal: Escaped => Some(literal.formatTxt(args))
            case plurals: Plurals => plurals.formatTxt(quantity, args)
          }
        } catch {
          case e: Exception =>
            logger.warn(s"Failed to format txt $lang/$key -> $translation (${args.toList})", e)
            Some(key)
        }
      } getOrElse key
  }

  private[i18n] def findTranslation(key: MessageKey, lang: Lang): Option[Translation] =
    Registry.all.get(lang).flatMap(t => Option(t get key)) orElse
      Option(Registry.default.get(key))
}
