package lila.socket

import play.api.libs.json.*
import alleycats.Zero

object Socket extends Socket:

  opaque type Sri = String
  object Sri:
    def apply(v: String): Sri = v
  extension (s: Sri) def value: String = s

  val sriIso        = lila.common.Iso.isoIdentity[Sri]
  given Format[Sri] = lila.common.Json.stringIsoFormat(using sriIso)

  case class Sris(sris: Set[Sri])

private[socket] trait Socket:

  def makeMessage[A](t: String, data: A)(implicit writes: Writes[A]): JsObject =
    JsObject(new Map.Map2("t", JsString(t), "d", writes.writes(data)))

  def makeMessage(t: String): JsObject = JsObject(new Map.Map1("t", JsString(t)))
