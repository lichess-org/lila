package lila.socket

import play.api.libs.json._

object Socket extends Socket {

  case class Uid(value: String) extends AnyVal
}

private[socket] trait Socket {

  def makeMessage[A](t: String, data: A)(implicit writes: Writes[A]): JsObject =
    JsObject(new Map.Map2("t", JsString(t), "d", writes.writes(data)))

  def makeMessage(t: String): JsObject = JsObject(new Map.Map1("t", JsString(t)))

  val initialPong = makeMessage("n")
}
