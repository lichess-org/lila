package lila.i18n

import java.io._
import scala.concurrent.Future

import play.api.i18n.Lang

private[i18n] final class FileFix(
    pool: I18nPool,
    path: String,
    keys: I18nKeys,
    messages: Messages) {

  val apply: Funit =
    Future.traverse(pool.nonDefaultLangs.toList)(fix).void

  private def fix(lang: Lang): Funit =
    write(lang, sanitize((messages get lang.language) | Map.empty))

  private def sanitize(messages: Map[String, String]) =
    keys.keys map { key =>
      messages get key.key filter { message =>
        hasVariable(key.to(pool.default)()).fold(hasVariable(message), true)
      } map (key.key -> _)
    } flatten

  private def hasVariable(message: String) = message contains "%s"

  private def write(lang: Lang, messages: List[(String, String)]) = {
    val file = "%s/messages.%s".format(path, lang.language)
    printToFile(file) { writer =>
      messages foreach {
        case (name, trans) => writer.println(name + "=" + trans)
      }
    }
  }
}
