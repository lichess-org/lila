package lila
package i18n

import play.api.i18n.{ MessagesApi, Lang }
import java.io._
import scalaz.effects._

final class FileFix(
    pool: I18nPool,
    path: String,
    keys: I18nKeys,
    api: MessagesApi) {

  private val pathFile = new File(path)
  private val keyNames = keys.keys map (_.key)

  val apply: IO[Unit] = 
    (pool.nonDefaultLangs.toList map fix).sequence map (_ ⇒ Unit)

  private def fix(lang: Lang): IO[Unit] = {
    val messages = sanitize((api.messages get lang.language) | Map.empty)
    write(lang, messages)
  }

  private def sanitize(messages: Map[String, String]) =
    keyNames map { name ⇒
      messages get name map (name -> _)
    } flatten

  private def write(lang: Lang, messages: List[(String, String)]) = io {
    val file = "%s/messages.%s".format(pathFile.getCanonicalPath, lang.language)
    printToFile(new File(file)) { writer ⇒
      messages foreach {
        case (name, trans) ⇒ writer.println(name + "=" + trans)
      }
    }
  }

  private def printToFile(f: File)(op: PrintWriter ⇒ Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }
}
