package lila
package lobby

import scala.math.max
import play.api.libs.json._
import scalaz.effects._

import memo.Builder

final class History(timeout: Int) {

  private var privateVersion = 0
  private val messages = memo.Builder.expiry[Int, JsObject](timeout)

  def version = privateVersion

  def since(v: Int): List[JsObject] =
    (v + 1 to version).toList map message flatten

  private def message(v: Int) = Option(messages getIfPresent v)

  def +=(msg: JsObject): JsObject = {
    privateVersion = privateVersion + 1
    val vmsg = msg ++ JsObject(Seq("v" -> JsNumber(privateVersion)))
    messages.put(privateVersion, vmsg)
    vmsg
  }
}
