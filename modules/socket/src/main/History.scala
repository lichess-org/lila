package lila.socket

import scala.concurrent.duration.Duration

import play.api.libs.json._

import actorApi._
import lila.memo

final class History(ttl: Duration) {

  import History._

  private var privateVersion = 0
  private val messages = memo.Builder.expiry[Int, Message](ttl)

  def version = privateVersion

  // none if version asked is > to history version
  // none if an event is missing (asked too old version)
  def since(v: Int): Option[List[Message]] =
    if (v > version) None
    else if (v == version) Some(Nil)
    else ((v + 1 to version).toList map message).sequence

  private def message(v: Int) = Option(messages getIfPresent v)

  def +=(payload: JsObject, troll: Boolean): Message = {
    privateVersion = privateVersion + 1
    val vmsg = Message(payload, privateVersion, troll)
    messages.put(privateVersion, vmsg)
    vmsg
  }
}

object History {

  case class Message(payload: JsObject, version: Int, troll: Boolean = false) {

    lazy val fullMsg = payload + ("v" -> JsNumber(version))

    lazy val skipMsg = Json.obj("v" -> version)

    def ++(obj: JsObject) = copy(payload = payload ++ obj)
  }
}
