package lila.round
package actorApi

import chess.Color
import lila.socket.SocketMember
import lila.game.{ Game, Event }
import lila.user.User

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
    val userId = user map (_.id)
    val muted = ~user.map(_.muted)
    owner.fold(
      Owner(channel, userId, color, muted),
      Watcher(channel, userId, color, muted))
  }
}

case class Owner(
    channel: JsChannel,
    userId: Option[String],
    color: Color,
    muted: Boolean) extends Member {

  val owner = true
}

case class Watcher(
    channel: JsChannel,
    userId: Option[String],
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
case class FinishGame(game: Game)
