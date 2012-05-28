package lila
package socket

import play.api.libs.json.JsObject

trait SocketMember {
  val channel: Channel
  val username: Option[String]

  lazy val userId: Option[String] = username map (_.toLowerCase)
}
case object Close
case object GetUsernames
case object GetNbMembers
case class NbMembers(nb: Int)
case class Ping(uid: String)
case object Broom
case class Quit(uid: String)

case class SendTo(userId: String, message: JsObject)
