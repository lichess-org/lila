package lila.site
package actorApi

import lila.socket.SocketMember

import play.api.libs.json._

case class Member(
  channel: JsChannel,
  userId: Option[String],
  flag: Option[String]) extends SocketMember {

  def hasFlag(f: String) = flag zmap (f ==)
}

case class Join(uid: String, userId: Option[String], flag: Option[String])
