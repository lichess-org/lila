package lila.i18n

import scalatags.Text.all.*
import play.api.i18n.Lang

import lila.common.String.html.escapeHtml

object Translator:

  object frag:
    def literal(key: I18nKey, args: Seq[Matchable], lang: Lang): RawFrag =
      translate(key, lang, I18nQuantity.Other /* grmbl */, args)

    def plural(key: I18nKey, count: Count, args: Seq[Matchable], lang: Lang): RawFrag =
      translate(key, lang, I18nQuantity(lang, count), args)

    private def translate(
        key: I18nKey,
        lang: Lang,
        quantity: I18nQuantity,
        args: Seq[Matchable]
    ): RawFrag =
      findTranslation(key, lang) flatMap { translation =>
        val htmlArgs = escapeArgs(args)
        try
          translation match
            case literal: Simple  => Some(literal.format(htmlArgs))
            case literal: Escaped => Some(literal.format(htmlArgs))
            case plurals: Plurals => plurals.format(quantity, htmlArgs)
        catch
          case e: Exception =>
            logger.warn(s"Failed to format html $lang/$key -> $translation (${args.toList})", e)
            Some(RawFrag(key.value))
      } getOrElse RawFrag(key.value)

    private def escapeArgs(args: Seq[Matchable]): Seq[RawFrag] = args.map:
      case s: String     => escapeHtml(Html(s))
      case r: RawFrag    => r
      case f: StringFrag => RawFrag(f.render)
      case a             => RawFrag(a.toString)

  object txt:

    def literal(key: I18nKey, args: Seq[Any], lang: Lang): String =
      translate(key, lang, I18nQuantity.Other /* grmbl */, args)

    def plural(key: I18nKey, count: Count, args: Seq[Any], lang: Lang): String =
      translate(key, lang, I18nQuantity(lang, count), args)

    private def translate(
        key: I18nKey,
        lang: Lang,
        quantity: I18nQuantity,
        args: Seq[Any]
    ): String =
      findTranslation(key, lang) flatMap { translation =>
        try
          translation match
            case literal: Simple  => Some(literal.formatTxt(args))
            case literal: Escaped => Some(literal.formatTxt(args))
            case plurals: Plurals => plurals.formatTxt(quantity, args)
        catch
          case e: Exception =>
            logger.warn(s"Failed to format txt $lang/$key -> $translation (${args.toList})", e)
            Some(key.value)
      } getOrElse key.value

  private[i18n] def findTranslation(key: I18nKey, lang: Lang): Option[Translation] =
    Registry.all.get(lang).flatMap(t => Option(t get key)) orElse
      Option(Registry.default.get(key))
