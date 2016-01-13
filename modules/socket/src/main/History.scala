package lila.socket

import scala.concurrent.duration.Duration

import play.api.libs.json._

import actorApi._

final class History[Metadata](ttl: Duration) {

  type Message = History.Message[Metadata]

  private var privateVersion = 0
  private val messages = lila.memo.Builder.expiry[Int, Message](ttl)

  def version = privateVersion

  // none if version asked is > to history version
  // none if an event is missing (asked too old version)
  def since(v: Int): Option[List[Message]] =
    if (v > version) None
    else if (v == version) Some(Nil)
    else {
      val msgs = (v + 1 to version).toList flatMap message
      (msgs.size == version - v) option msgs
    }

  private def message(v: Int) = Option(messages getIfPresent v)

  def +=(payload: JsObject, metadata: Metadata): Message = {
    privateVersion = privateVersion + 1
    val vmsg = History.Message(payload, privateVersion, metadata)
    messages.put(privateVersion, vmsg)
    vmsg
  }
}

object History {

  case class Message[Metadata](payload: JsObject, version: Int, metadata: Metadata) {

    lazy val fullMsg = payload + ("v" -> JsNumber(version))

    lazy val skipMsg = Json.obj("v" -> version)

    def ++(obj: JsObject) = copy(payload = payload ++ obj)
  }
}
