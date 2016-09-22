package lila.socket

import play.api.libs.json.JsValue

trait SocketMember extends Ordered[SocketMember] {

  protected val channel: JsChannel
  val sameOrigin: Boolean
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

  def apply(c: JsChannel, sameOrigin: Boolean): SocketMember = apply(c, sameOrigin, none, false)

  def apply(c: JsChannel, so: Boolean, uid: Option[String], tr: Boolean): SocketMember = new SocketMember {
    val channel = c
    val sameOrigin = so
    val userId = uid
    val troll = tr
  }
}
