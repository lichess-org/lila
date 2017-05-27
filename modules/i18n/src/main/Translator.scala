package lila.i18n

import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import play.twirl.api.Html

sealed trait Translation extends Any {

  def format(args: Seq[Any]): Option[String]
}

case class Literal(message: String) extends AnyVal with Translation {

  def format(args: Seq[Any]) = Some {
    if (args.isEmpty) message
    else message.format(args: _*)
  }
}

case class Plurals(messages: Map[I18nQuantity, String]) extends AnyVal with Translation {

  def format(args: Seq[Any]) = messages.collectFirst {
    case (quantity, message) => message.format(args: _*)
  }
}

object Translator {

  def html(key: MessageKey, args: List[Any], lang: Lang): Html =
    Html(str(key, args, lang))

  def str(key: MessageKey, args: List[Any], lang: Lang): String =
    translate(key, args, lang) getOrElse key

  def transTo(key: MessageKey, args: Seq[Any], lang: Lang): String =
    translate(key, args, lang) getOrElse key

  private def translate(key: MessageKey, args: Seq[Any], lang: Lang): Option[String] =
    I18nDb.all.get(lang) orElse I18nDb.all.get(defaultLang) flatMap (_ get key) flatMap {
      formatTranslation(key, _, args)
    }

  private def formatTranslation(key: MessageKey, translation: Translation, args: Seq[Any]) = try {
    translation.format(args)
  }
  catch {
    case e: Exception =>
      logger.warn(s"Failed to translate $key -> $translation ($args)", e)
      None
  }
}
