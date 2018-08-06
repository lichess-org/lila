package lidraughts.socket

import play.api.libs.json._

object Socket extends Socket {

  case class Uid(value: String) extends AnyVal

  val uidIso = lidraughts.common.Iso.string[Uid](Uid.apply, _.value)
  implicit val uidFormat = lidraughts.common.PimpedJson.stringIsoFormat(uidIso)

  case class SocketVersion(value: Int) extends AnyVal with IntValue with Ordered[SocketVersion] {
    def compare(other: SocketVersion) = value compare other.value
    def inc = SocketVersion(value + 1)
  }

  val socketVersionIso = lidraughts.common.Iso.int[SocketVersion](SocketVersion.apply, _.value)
  implicit val socketVersionFormat = lidraughts.common.PimpedJson.intIsoFormat(socketVersionIso)
}

private[socket] trait Socket {

  def makeMessage[A](t: String, data: A)(implicit writes: Writes[A]): JsObject =
    JsObject(new Map.Map2("t", JsString(t), "d", writes.writes(data)))

  def makeMessage(t: String): JsObject = JsObject(new Map.Map1("t", JsString(t)))

  val initialPong = makeMessage("n")
}
