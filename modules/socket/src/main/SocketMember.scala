package lila.socket

import play.api.libs.json.JsValue

trait SocketMember extends Ordered[SocketMember] {

  protected val channel: JsChannel
  val userId: Option[String]
  val troll: Boolean

  def isAuth = userId.isDefined

  def compare(other: SocketMember) = ~userId compare ~other.userId

  def push(msg: JsValue) {
    // import play.api.Play.current
    // import play.api.libs.concurrent.Akka
    // import scala.concurrent.duration._
    // Akka.system.scheduler.scheduleOnce(2.second) {
    //   channel push msg
    // }
    try {
      channel push msg
    }
    catch {
      case _: java.nio.channels.ClosedChannelException =>
      // catching because it's quite polluting the production logs
    }
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
