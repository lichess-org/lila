package lila.i18n

import play.twirl.api.Html
import scalatags.Text.RawFrag

import lila.common.Lang
import lila.common.String.html.escapeHtml
import lila.common.String.frag.{ escapeHtml => escapeFrag }

object Translator {

  object html {

    def literal(key: MessageKey, db: I18nDb.Ref, args: Seq[Any], lang: Lang): Html =
      translate(key, db, lang, I18nQuantity.Other /* grmbl */ , args)

    def plural(key: MessageKey, db: I18nDb.Ref, count: Count, args: Seq[Any], lang: Lang): Html =
      translate(key, db, lang, I18nQuantity(lang, count), args)

    private def translate(key: MessageKey, db: I18nDb.Ref, lang: Lang, quantity: I18nQuantity, args: Seq[Any]): Html =
      findTranslation(key, db, lang) flatMap { translation =>
        val htmlArgs = escapeArgs(args)
        try {
          translation match {
            case literal: Simple => Some(literal.formatHtml(htmlArgs))
            case literal: Escaped => Some(literal.formatHtml(htmlArgs))
            case plurals: Plurals => plurals.formatHtml(quantity, htmlArgs)
          }
        } catch {
          case e: Exception =>
            logger.warn(s"Failed to format html $db/$lang/$key -> $translation (${args.toList})", e)
            Some(Html(key))
        }
      } getOrElse {
        logger.info(s"No translation found for $quantity $key in $lang")
        Html(key)
      }

    private def escapeArgs(args: Seq[Any]): Seq[Html] = args.map {
      case s: String => escapeHtml(s)
      case h: Html => h
      case r: RawFrag => Html(r.v)
      case a => Html(a.toString)
    }
  }

  object frag {
    def literal(key: MessageKey, db: I18nDb.Ref, args: Seq[Any], lang: Lang): RawFrag =
      translate(key, db, lang, I18nQuantity.Other /* grmbl */ , args)

    def plural(key: MessageKey, db: I18nDb.Ref, count: Count, args: Seq[Any], lang: Lang): RawFrag =
      translate(key, db, lang, I18nQuantity(lang, count), args)

    private def translate(key: MessageKey, db: I18nDb.Ref, lang: Lang, quantity: I18nQuantity, args: Seq[Any]): RawFrag =
      findTranslation(key, db, lang) flatMap { translation =>
        val htmlArgs = escapeArgs(args)
        try {
          translation match {
            case literal: Simple => Some(literal.formatFrag(htmlArgs))
            case literal: Escaped => Some(literal.formatFrag(htmlArgs))
            case plurals: Plurals => plurals.formatFrag(quantity, htmlArgs)
          }
        } catch {
          case e: Exception =>
            logger.warn(s"Failed to format html $db/$lang/$key -> $translation (${args.toList})", e)
            Some(RawFrag(key))
        }
      } getOrElse {
        logger.info(s"No translation found for $quantity $key in $lang")
        RawFrag(key)
      }

    private def escapeArgs(args: Seq[Any]): Seq[RawFrag] = args.map {
      case s: String => escapeFrag(s)
      case h: Html => RawFrag(h.body)
      case r: RawFrag => r
      case a => RawFrag(a.toString)
    }
  }

  object txt {

    def literal(key: MessageKey, db: I18nDb.Ref, args: Seq[Any], lang: Lang): String =
      translate(key, db, lang, I18nQuantity.Other /* grmbl */ , args)

    def plural(key: MessageKey, db: I18nDb.Ref, count: Count, args: Seq[Any], lang: Lang): String =
      translate(key, db, lang, I18nQuantity(lang, count), args)

    private def translate(key: MessageKey, db: I18nDb.Ref, lang: Lang, quantity: I18nQuantity, args: Seq[Any]): String =
      findTranslation(key, db, lang) flatMap { translation =>
        try {
          translation match {
            case literal: Simple => Some(literal.formatTxt(args))
            case literal: Escaped => Some(literal.formatTxt(args))
            case plurals: Plurals => plurals.formatTxt(quantity, args)
          }
        } catch {
          case e: Exception =>
            logger.warn(s"Failed to format txt $db/$lang/$key -> $translation (${args.toList})", e)
            Some(key)
        }
      } getOrElse {
        logger.info(s"No translation found for $quantity $db/$lang/$key in $lang")
        key
      }
  }

  private[i18n] def findTranslation(key: MessageKey, db: I18nDb.Ref, lang: Lang): Option[Translation] =
    I18nDb(db).get(lang.value).flatMap(t => Option(t get key)) orElse
      I18nDb(db).get(defaultLang.value).flatMap(t => Option(t get key))
}
