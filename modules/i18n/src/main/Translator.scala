package lila.i18n

import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import play.twirl.api.Html

private sealed trait Translation extends Any

private case class Literal(message: String) extends AnyVal with Translation {

  def format(args: Seq[Any]): String =
    if (args.isEmpty) message
    else message.format(args: _*)
}

private case class Plurals(messages: Map[I18nQuantity, String]) extends AnyVal with Translation {

  def format(quantity: I18nQuantity, args: Seq[Any]): Option[String] =
    messages.get(quantity)
      .orElse(messages.get(I18nQuantity.Other))
      .orElse(messages.headOption.map(_._2))
      .map(_.format(args: _*))
}

object Translator {

  def literal(key: MessageKey, args: Seq[Any], lang: Lang): String =
    findTranslation(key, lang) flatMap {
      formatTranslation(key, _, I18nQuantity.Other /* grmbl */ , args)
    } getOrElse key

  def plural(key: MessageKey, count: Count, args: Seq[Any], lang: Lang): String =
    findTranslation(key, lang) flatMap {
      formatTranslation(key, _, I18nQuantity(lang, count), args)
    } getOrElse key

  private def findTranslation(key: MessageKey, lang: Lang): Option[Translation] =
    I18nDb.all.get(lang) orElse I18nDb.all.get(defaultLang) flatMap (_ get key)

  private def formatTranslation(key: MessageKey, translation: Translation, quantity: I18nQuantity, args: Seq[Any]): Option[String] = try {
    translation match {
      case literal: Literal => Some(literal.format(args))
      case plurals: Plurals => plurals.format(quantity, args)
    }
  }
  catch {
    case e: Exception =>
      logger.warn(s"Failed to translate $key -> $translation (${args.toList})", e)
      None
  }
}
