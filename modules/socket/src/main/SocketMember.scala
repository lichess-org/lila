package lila.socket

import play.api.libs.json.{ JsObject, JsValue }

trait SocketMember {

  val userId: Option[String]
  def isAuth = userId.isDefined

  def push(msg: JsValue): Unit

  def end: Unit
}

trait DirectSocketMember extends SocketMember {

  protected val channel: JsChannel

  def push(msg: JsValue) = channel push msg

  def end = channel.end
}

trait RemoteSocketMember extends SocketMember {

  protected def bus: lila.common.Bus
  protected def sri: Socket.Sri

  def push(msg: JsValue) = msg.asOpt[JsObject] foreach { obj =>
    bus.publish(lila.hub.actorApi.socket.RemoteSocketTellSriOut(sri.value, obj), 'remoteSocketOut)
  }

  def end = () // meh
}
