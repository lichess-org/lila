package lila
package socket

trait SocketMember {
  val channel: Channel
  val username: Option[String]
}
case object Close
case object GetUsernames
case object GetNbMembers
case class NbMembers(nb: Int)
case class Ping(uid: String)
case object Broom
case class Quit(uid: String)
