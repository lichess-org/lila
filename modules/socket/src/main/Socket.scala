package lila.socket

import play.api.libs.json.*
import scala.concurrent.Promise
import alleycats.Zero

object Socket extends Socket:

  opaque type Sri = String
  object Sri:
    def apply(v: String): Sri = v
  extension (s: Sri) def value: String = s

  val sriIso        = lila.common.Iso.isoIdentity[Sri]
  given Format[Sri] = lila.common.Json.stringIsoFormat(using sriIso)

  case class Sris(sris: Set[Sri])

  case class SocketVersion(value: Int) extends AnyVal with IntValue with Ordered[SocketVersion]:
    def compare(other: SocketVersion) = Integer.compare(value, other.value)
    def inc                           = SocketVersion(value + 1)

  val socketVersionIso        = lila.common.Iso.int[SocketVersion](SocketVersion.apply, _.value)
  given Format[SocketVersion] = lila.common.Json.intIsoFormat(using socketVersionIso)
  given Zero[SocketVersion]   = Zero[SocketVersion](SocketVersion(0))

  case class GetVersion(promise: Promise[SocketVersion])

  case class SendToFlag(flag: String, message: JsObject)

private[socket] trait Socket:

  def makeMessage[A](t: String, data: A)(implicit writes: Writes[A]): JsObject =
    JsObject(new Map.Map2("t", JsString(t), "d", writes.writes(data)))

  def makeMessage(t: String): JsObject = JsObject(new Map.Map1("t", JsString(t)))
