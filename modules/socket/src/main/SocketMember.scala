package lila.socket

import play.api.libs.json.JsValue

trait SocketMember {

  protected val channel: JsChannel
  val userId: Option[String]
  val troll: Boolean

  def isAuth = userId.isDefined

  def push(msg: JsValue) = try {
    channel push msg
  } catch {
    case _: java.nio.channels.ClosedChannelException => lila.mon.socket.pushChannelClosed()
  }

  def end = channel.end
}
