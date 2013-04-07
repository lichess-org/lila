package lila.socket

import play.api.libs.json.JsObject
import scala.collection.mutable

trait SocketMember {
  val channel: JsChannel
  val username: Option[String]

  lazy val userId: Option[String] = username map (_.toLowerCase)

  private val privateLiveGames = mutable.Set[String]()

  def liveGames = privateLiveGames

  def addLiveGames(ids: List[String]) {
    ids foreach liveGames.+=
  }
}
case object Close
case object GetUsernames
case object GetNbMembers
case class NbMembers(nb: Int)
case class Ping(uid: String)
case class PingVersion(uid: String, version: Int)
case object Broom
case class Quit(uid: String)

case class SendTo(userId: String, message: JsObject)
case class SendTos(userIds: Set[String], message: JsObject)
case class Fen(gameId: String, fen: String, lastMove: Option[String])
case class LiveGames(uid: String, gameIds: List[String])
case class Resync(uid: String)
