package lila.socket

import play.api.libs.json.JsValue

trait SocketMember extends Ordered[SocketMember] {

  protected val channel: JsChannel
  val userId: Option[String]
  val troll: Boolean

  def isAuth = userId.isDefined

  def compare(other: SocketMember) = ~userId compare ~other.userId

  def push(msg: JsValue) {
    channel push msg
  }

  def end {
    channel.end
  }
}

object SocketMember {

  def apply(c: JsChannel): SocketMember = apply(c, none, false)

  def apply(c: JsChannel, uid: Option[String], tr: Boolean): SocketMember = new SocketMember {
    val channel = c
    val userId = uid
    val troll = tr
  }
}
