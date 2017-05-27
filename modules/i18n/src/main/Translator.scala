package lila.i18n

import play.api.i18n.Lang
import play.twirl.api.Html

import lila.common.String.html.{ escape => escapeHtml }

object Translator {

  object html {

    def literal(key: MessageKey, args: Seq[Any], lang: Lang): Html =
      translate(key, lang, I18nQuantity.Other /* grmbl */ , args)

    def plural(key: MessageKey, count: Count, args: Seq[Any], lang: Lang): Html =
      translate(key, lang, I18nQuantity(lang, count), args)

    private def translate(key: MessageKey, lang: Lang, quantity: I18nQuantity, args: Seq[Any]): Html =
      findTranslation(key, lang) flatMap { translation =>
        val htmlArgs = escapeArgs(args)
        try {
          translation match {
            case literal: Literal => Some(literal.formatHtml(htmlArgs))
            case plurals: Plurals => plurals.formatHtml(quantity, htmlArgs)
          }
        }
        catch {
          case e: Exception =>
            logger.warn(s"Failed to format html $key -> $translation (${args.toList})", e)
            Some(Html(key))
        }
      } getOrElse {
        logger.warn(s"No translation found for $quantity $key in $lang")
        Html(key)
      }

    private def escapeArgs(args: Seq[Any]): Seq[Html] = args.map {
      case s: String => escapeHtml(s)
      case h: Html => h
      case a => Html(a.toString)
    }
  }

  object txt {

    def literal(key: MessageKey, args: Seq[Any], lang: Lang): String =
      translate(key, lang, I18nQuantity.Other /* grmbl */ , args)

    def plural(key: MessageKey, count: Count, args: Seq[Any], lang: Lang): String =
      translate(key, lang, I18nQuantity(lang, count), args)

    private def translate(key: MessageKey, lang: Lang, quantity: I18nQuantity, args: Seq[Any]): String =
      findTranslation(key, lang) flatMap { translation =>
        try {
          translation match {
            case literal: Literal => Some(literal.formatTxt(args))
            case plurals: Plurals => plurals.formatTxt(quantity, args)
          }
        }
        catch {
          case e: Exception =>
            logger.warn(s"Failed to format txt $key -> $translation (${args.toList})", e)
            Some(key)
        }
      } getOrElse {
        logger.warn(s"No translation found for $quantity $key in $lang")
        key
      }
  }

  private def findTranslation(key: MessageKey, lang: Lang): Option[Translation] =
    I18nDb.all.get(lang).flatMap(_ get key) orElse
      I18nDb.all.get(defaultLang).flatMap(_ get key)
}
