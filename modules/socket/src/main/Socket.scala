package lila.socket

import play.api.libs.json._

object Socket extends Socket {

  case class Uid(value: String) extends AnyVal
}

private[socket] trait Socket {

  def makeMessage[A](t: String, data: A)(implicit writes: Writes[A]): JsObject =
    JsObject(List("t" -> JsString(t), "d" -> writes.writes(data)))

  def makeMessage(t: String): JsObject = JsObject(List("t" -> JsString(t)))

  val initialPong = makeMessage("n")
}
