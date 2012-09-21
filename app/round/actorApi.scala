package lila
package round

import chess.Color
import socket.SocketMember
import game.DbGame
import user.User

sealed trait Member extends SocketMember {

  val color: Color
  val owner: Boolean
  val muted: Boolean

  def watcher = !owner
  def canChat = !muted
}

object Member {
  def apply(
    channel: JsChannel,
    user: Option[User],
    color: Color,
    owner: Boolean): Member = {
    val username = user map (_.username)
    val muted = user.fold(_.muted, false)
    owner.fold(
      Owner(channel, username, color, muted),
      Watcher(channel, username, color, muted))
  }
}

case class Owner(
    channel: JsChannel,
    username: Option[String],
    color: Color,
    muted: Boolean) extends Member {

  val owner = true
}

case class Watcher(
    channel: JsChannel,
    username: Option[String],
    color: Color,
    muted: Boolean) extends Member {

  val owner = false
}

case class Join(
  uid: String,
  user: Option[User],
  version: Int,
  color: Color,
  owner: Boolean)
case class Connected(
  enumerator: JsEnumerator,
  member: Member)
case class Events(events: List[Event])
case class GameEvents(gameId: String, events: List[Event])
case class GetGameVersion(gameId: String)
case object ClockSync
case class IsConnectedOnGame(gameId: String, color: Color)
case class IsGone(gameId: String, color: Color)
case class CloseGame(gameId: String)
case class GetHub(gameId: String)
case object HubTimeout
case object GetNbHubs
case class AnalysisAvailable(gameId: String)
case class Ack(uid: String)
case class FinishGame(game: DbGame)
