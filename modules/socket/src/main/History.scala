package lidraughts.socket

import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import scala.concurrent.duration.FiniteDuration

import play.api.libs.json._

final class History[Metadata](ttl: FiniteDuration) {

  type Message = History.Message[Metadata]

  private var privateVersion = 0

  private val cache: Cache[Int, Message] = Scaffeine()
    .expireAfterWrite(ttl)
    .build[Int, Message]

  def version = privateVersion

  // none if version asked is > to history version
  // none if an event is missing (asked too old version)
  def since(v: Int): Option[List[Message]] =
    if (v > version) None
    else if (v == version) Some(Nil)
    else {
      val msgs: List[Message] = (v + 1 to version).flatMap(message)(scala.collection.breakOut)
      (msgs.size == version - v) option msgs
    }

  private def message(v: Int) = cache getIfPresent v

  def +=(payload: JsObject, metadata: Metadata): Message = {
    privateVersion = privateVersion + 1
    val vmsg = History.Message(payload, privateVersion, metadata)
    cache.put(privateVersion, vmsg)
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
