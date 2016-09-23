package lila.site
package actorApi

import play.api.libs.json._

import lila.socket.SocketMember

case class Member(
  channel: JsChannel,
  userId: Option[String],
  flag: Option[String]) extends SocketMember {

  val troll = false

  def hasFlag(f: String) = flag contains f

  def isApi = hasFlag("api")
}

case class Join(uid: String, userId: Option[String], flag: Option[String])
private[site] case class Connected(enumerator: JsEnumerator, member: Member)
