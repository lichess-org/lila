package lila.socket

import play.api.libs.json.{ JsObject, JsValue }

trait SocketMember {

  val userId: Option[String]
  def isAuth = userId.isDefined

  val push: SocketMember.Push

  def end: Unit
}

trait DirectSocketMember extends SocketMember {

  protected val channel: JsChannel

  val push: SocketMember.Push = channel.push _

  def end = channel.end
}

trait RemoteSocketMember extends SocketMember {

  def end = () // meh
}

object SocketMember {
  type Push = JsValue => Unit
}
