package lila.i18n

import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import play.twirl.api.Html

private[i18n] final class Translator(messages: Messages, pool: I18nPool) {

  private val defaultMessages = messages.get("default") err "No default messages"

  def html(key: String, args: List[Any])(implicit req: RequestHeader): Html =
    Html(str(key, args)(req))

  def str(key: String, args: List[Any])(implicit req: RequestHeader): String =
    translate(key, args)(pool lang req) getOrElse key

  def transTo(key: String, args: Seq[Any])(lang: Lang): String =
    translate(key, args)(lang) getOrElse key

  def rawTranslation(lang: Lang)(key: String): Option[String] =
    messages get lang.language flatMap (_ get key)

  private def defaultTranslation(key: String, args: Seq[Any]): Option[String] =
    defaultMessages get key flatMap { pattern =>
      formatTranslation(key, pattern, args)
    }

  private def translate(key: String, args: Seq[Any])(lang: Lang): Option[String] =
    if (lang.language == pool.default.language) defaultTranslation(key, args)
    else messages get lang.language flatMap (_ get key) flatMap { pattern =>
      formatTranslation(key, pattern, args)
    } orElse defaultTranslation(key, args)

  private def formatTranslation(key: String, pattern: String, args: Seq[Any]) = try {
    Some(if (args.isEmpty) pattern else pattern.format(args: _*))
  }
  catch {
    case e: Exception =>
      logger.warn(s"Failed to translate $key -> $pattern ($args)", e)
      None
  }
}
