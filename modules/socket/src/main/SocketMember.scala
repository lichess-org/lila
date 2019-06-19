package lila.socket

import play.api.libs.json.JsValue

trait SocketMember {

  protected val channel: JsChannel
  val userId: Option[String]

  def isAuth = userId.isDefined

  def push(msg: JsValue) = channel push msg

  def end = channel.end
}
