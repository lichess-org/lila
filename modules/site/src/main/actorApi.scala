package lila.site
package actorApi

import play.api.libs.json._

import lila.socket.SocketMember

case class Member(
  out: akka.actor.ActorRef,
  userId: Option[String],
  flag: Option[String]) extends SocketMember {

  val troll = false

  def hasFlag(f: String) = flag ?? (f ==)
}
case class AddMember(uid: String, member: Member)
