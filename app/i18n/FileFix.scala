package lila
package i18n

import play.api.i18n.{ MessagesApi, Lang }
import java.io._
import scalaz.effects._

private[i18n] final class FileFix(
    pool: I18nPool,
    path: String,
    keys: I18nKeys,
    api: MessagesApi) {

  val apply: IO[Unit] =
    (pool.nonDefaultLangs.toList map fix).sequence map (_ ⇒ Unit)

  private def fix(lang: Lang): IO[Unit] = {
    val messages = sanitize((api.messages get lang.language) | Map.empty)
    write(lang, messages)
  }

  private def sanitize(messages: Map[String, String]) =
    keys.keys map { key ⇒
      messages get key.key filter { message ⇒
        hasVariable(key.to(pool.default)()).fold(hasVariable(message), true)
      } map (key.key -> _)
    } flatten

  private def hasVariable(message: String) = message contains "%s"

  private def write(lang: Lang, messages: List[(String, String)]) = {
    val file = "%s/messages.%s".format(path, lang.language)
    printToFile(file) { writer ⇒
      messages foreach {
        case (name, trans) ⇒ writer.println(name + "=" + trans)
      }
    }
  }
}
