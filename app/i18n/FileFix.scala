package lila
package i18n

import play.api.i18n.Lang
import java.io._
import scalaz.effects._

final class FileFix(
  pool: I18nPool,
  path: String,
  keys: I18nKeys) {

  private val pathFile = new File(path)

}
