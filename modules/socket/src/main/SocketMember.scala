package lila.socket

import play.api.libs.json.JsValue

trait SocketMember {

  protected val channel: JsChannel
  val userId: Option[String]

  var ended = false

  def isAuth = userId.isDefined

  def push(msg: JsValue) = channel push msg

  def end = {
    ended = true
    channel.end
  }
}
