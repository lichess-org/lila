package lila.app
package socket

import scala.math.max
import play.api.libs.json._
import scalaz.effects._

import lila.common.memo.Builder

final class History(timeout: Int) {

  private var privateVersion = 0
  private val messages = memo.Builder.expiry[Int, JsObject](timeout)

  def version = privateVersion

  // none if version asked is > to history version
  // none if an event is missing (asked too old version)
  def since(v: Int): Option[List[JsObject]] =
    if (v > version) None
    else if (v == version) Some(Nil)
    else ((v + 1 to version).toList map message).sequence

  private def message(v: Int) = Option(messages getIfPresent v)

  def +=(msg: JsObject): JsObject = {
    privateVersion = privateVersion + 1
    val vmsg = msg ++ JsObject(Seq("v" -> JsNumber(privateVersion)))
    messages.put(privateVersion, vmsg)
    vmsg
  }
}
