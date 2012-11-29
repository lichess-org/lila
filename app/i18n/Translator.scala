package lila
package i18n

import play.api.i18n.{ MessagesApi, Lang }
import play.api.mvc.RequestHeader
import play.api.templates.Html

final class Translator(api: MessagesApi, pool: I18nPool) {

  private val messages = api.messages
  private val defaultMessages = messages.get("default") err "No default messages"

  def html(key: String, args: List[Any])(implicit req: RequestHeader): Html =
    Html(str(key, args)(req))

  def str(key: String, args: List[Any])(implicit req: RequestHeader): String =
    translate(key, args)(pool lang req) getOrElse key

  def transTo(key: String, args: List[Any])(lang: Lang): String =
    translate(key, args)(lang) getOrElse key

  def rawTranslation(lang: Lang)(key: String): Option[String] =
    messages get lang.code flatMap (_ get key)

  def defaultTranslation(key: String, args: List[Any]): Option[String] =
    defaultMessages get key flatMap { pattern ⇒
      formatTranslation(key, pattern, args) 
    }

  private def translate(key: String, args: List[Any])(lang: Lang): Option[String] =
    if (lang == pool.default) defaultTranslation(key, args)
    else messages get lang.code flatMap (_ get key) flatMap { pattern ⇒
      formatTranslation(key, pattern, args) 
    } orElse defaultTranslation(key, args)

  def formatTranslation(key: String, pattern: String, args: List[Any]) = try {
    Some(if (args.isEmpty) pattern else pattern.format(args: _*))
  }
  catch {
    case e: Exception ⇒ {
      println("Failed to translate %s -> %s (%s) - %s".format(
        key, pattern, args, e.getMessage))
      None
    }
  }
}
