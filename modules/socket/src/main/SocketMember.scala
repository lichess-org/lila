package lila.socket

import akka.actor.{ ActorRef, PoisonPill }
import play.api.libs.json.JsObject

trait SocketMember extends Ordered[SocketMember] {

  protected val out: ActorRef
  val userId: Option[String]
  val troll: Boolean

  def isAuth = userId.isDefined

  def compare(other: SocketMember) = ~userId compare ~other.userId

  def push(msg: JsObject) = out ! msg

  def end = out ! PoisonPill
}

object SocketMember {

  def apply(o: ActorRef): SocketMember = apply(o, none, false)

  def apply(o: ActorRef, uid: Option[String], tr: Boolean): SocketMember = new SocketMember {
    val out = o
    val userId = uid
    val troll = tr
  }
}
