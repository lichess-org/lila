package lila
package i18n

import core.Global.env // OMG

import play.api.i18n.{ MessagesApi, Lang }
import play.api.mvc.RequestHeader
import play.api.templates.Html

final class Translator(api: MessagesApi, pool: I18nPool) {
  
  private val messages = api.messages

  def trans(key: String, args: List[Any])(implicit req: RequestHeader): Html = 
    Html {
      translate(key, args)(pool lang req) getOrElse key
    }

  def transTo(key: String, args: List[Any])(lang: Lang): String = 
    translate(key, args)(lang) getOrElse key

  private def translate(key: String, args: List[Any])(lang: Lang): Option[String] = {
    messages.get(lang.code).flatMap(_.get(key)).orElse(messages.get("default").flatMap(_.get(key))).map { pattern =>
      if (args.isEmpty) pattern else pattern.format(args: _*)
    }
  }
}
