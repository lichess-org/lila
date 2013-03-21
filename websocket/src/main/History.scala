package lila.websocket

import scala.math.max
import scala.concurrent.duration.Duration
import play.api.libs.json._

import lila.memo

final class History(timeout: Duration) {

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
