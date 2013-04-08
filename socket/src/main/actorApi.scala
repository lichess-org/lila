package lila.socket
package actorApi

import play.api.libs.json.JsObject

case class Connected[A <: Member](
  enumerator: JsEnumerator,
  member: A)
case object Close
case object GetUserIds
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
