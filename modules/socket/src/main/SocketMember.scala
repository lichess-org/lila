package lila.socket

import akka.actor.{ ActorRef, PoisonPill }
import play.api.libs.json.JsValue

trait SocketMember extends Ordered[SocketMember] {

  protected val actor: ActorRef
  val userId: Option[String]
  val troll: Boolean

  def isAuth = userId.isDefined

  def compare(other: SocketMember) = ~userId compare ~other.userId

  def push(msg: JsValue) = actor ! msg

  def end = actor ! PoisonPill
}

object SocketMember {

  def apply(a: ActorRef): SocketMember = apply(a, none, false)

  def apply(a: ActorRef, uid: Option[String], tr: Boolean): SocketMember = new SocketMember {
    val actor = a
    val userId = uid
    val troll = tr
  }
}
