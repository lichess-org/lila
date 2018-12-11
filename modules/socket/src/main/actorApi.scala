package lila.socket
package actorApi

import play.api.libs.json.JsObject

import chess.Centis
import lila.common.PimpedJson.centisReads

case class Connected[M <: SocketMember](
    enumerator: JsEnumerator,
    member: M
)
case class Ping(uid: Socket.Uid, version: Option[Socket.SocketVersion], lagCentis: Option[Centis])
case class BotConnected(color: chess.Color, v: Boolean)

object Ping {
  import Socket.{ SocketVersion, socketVersionFormat }
  def apply(uid: Socket.Uid, o: JsObject): Ping = Ping(
    uid = uid,
    version = o.get[SocketVersion]("v"),
    lagCentis = o.get[Centis]("l")
  )
}

case object Broom
case class Quit(uid: Socket.Uid)

case class SocketEnter[M <: SocketMember](uid: Socket.Uid, member: M)
case class SocketLeave[M <: SocketMember](uid: Socket.Uid, member: M)

case class Resync(uid: Socket.Uid)

case class SendToFlag(flag: String, message: JsObject)

case object PopulationTell
case class NbMembers(nb: Int)

case class StartWatching(uid: Socket.Uid, member: SocketMember, gameIds: Set[String])
