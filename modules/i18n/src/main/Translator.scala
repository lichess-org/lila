package lila.i18n

import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import play.twirl.api.Html

sealed trait Translation extends Any

case class Literal(message: String) extends AnyVal with Translation {

  def format(args: Seq[Any]): String =
    if (args.isEmpty) message
    else message.format(args: _*)
}

case class Plurals(messages: Map[I18nQuantity, String]) extends AnyVal with Translation {

  def format(count: Count, args: Seq[Any]): Option[String] =
    // TODO real quantity selection
    messages.collectFirst {
      case (quantity, message) => message.format(args: _*)
    }
}

object Translator {

  def literal(key: MessageKey, args: Seq[Any], lang: Lang): String =
    findTranslation(key, lang) flatMap {
      formatTranslation(key, _, 1 /* grmbl */ , args)
    } getOrElse key

  def plural(key: MessageKey, count: Count, args: Seq[Any], lang: Lang): String =
    findTranslation(key, lang) flatMap {
      formatTranslation(key, _, count, args)
    } getOrElse key

  private def findTranslation(key: MessageKey, lang: Lang): Option[Translation] =
    I18nDb.all.get(lang) orElse I18nDb.all.get(defaultLang) flatMap (_ get key)

  private def formatTranslation(key: MessageKey, translation: Translation, count: Count, args: Seq[Any]): Option[String] = try {
    translation match {
      case literal: Literal => Some(literal.format(args))
      case plurals: Plurals => plurals.format(count, args)
    }
  }
  catch {
    case e: Exception =>
      logger.warn(s"Failed to translate $key -> $translation ($args)", e)
      None
  }
}
