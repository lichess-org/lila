package lila.socket

import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import scala.concurrent.duration.FiniteDuration

import play.api.libs.json._

import Socket.SocketVersion

final class History[Metadata](ttl: FiniteDuration) {

  type Message = History.Message[Metadata]

  private var privateVersion = SocketVersion(0)

  private val cache: Cache[Int, Message] = Scaffeine()
    .expireAfterWrite(ttl)
    .build[Int, Message]

  def version = privateVersion

  // none if version asked is > to history version
  // none if an event is missing (asked too old version)
  def since(v: SocketVersion): Option[List[Message]] =
    if (v > version) None
    else if (v == version) Some(Nil)
    else {
      val msgs: List[Message] = (v.inc.value to version.value).flatMap(message)(scala.collection.breakOut)
      (msgs.size == version.value - v.value) option msgs
    }

  def getRecent(maxEvents: Int): List[Message] = {
    val v = version.value
    (v - 5 to v).flatMap(message)(scala.collection.breakOut)
  }

  private def message(v: Int) = cache getIfPresent v

  def +=(payload: JsObject, metadata: Metadata): Message = {
    privateVersion = privateVersion.inc
    val vmsg = History.Message(payload, privateVersion, metadata)
    cache.put(privateVersion.value, vmsg)
    vmsg
  }
}

object History {

  case class Message[Metadata](payload: JsObject, version: SocketVersion, metadata: Metadata) {

    lazy val fullMsg = payload + ("v" -> JsNumber(version.value))

    lazy val skipMsg = Json.obj("v" -> version.value)

    def ++(obj: JsObject) = copy(payload = payload ++ obj)
  }
}
