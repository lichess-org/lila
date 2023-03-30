package lila.socket

import play.api.libs.json.*

object Socket extends Socket:

  opaque type Sri = String
  object Sri extends OpaqueString[Sri]

  case class Sris(sris: Set[Sri]) // pattern matched

private[socket] trait Socket:

  def makeMessage[A](t: String, data: A)(implicit writes: Writes[A]): JsObject =
    JsObject(new Map.Map2("t", JsString(t), "d", writes.writes(data)))

  def makeMessage(t: String): JsObject = JsObject(new Map.Map1("t", JsString(t)))
