package lila.round
package actorApi

import chess.Color
import lila.socket.SocketMember
import lila.game.{ Game, Event, PlayerRef }
import lila.user.User

sealed trait Member extends SocketMember {

  val color: Color
  val playerIdOption: Option[String]
  val troll: Boolean

  def owner = playerIdOption.isDefined
  def watcher = !owner
}

object Member {
  def apply(
    channel: JsChannel,
    user: Option[User],
    color: Color,
    playerIdOption: Option[String]): Member = {
    val userId = user map (_.id)
    val troll = user.??(_.troll)
    playerIdOption.fold[Member](Watcher(channel, userId, color, troll)) { playerId ⇒
      Owner(channel, userId, playerId, color, troll)
    }
  }
}

case class Owner(
    channel: JsChannel,
    userId: Option[String],
    playerId: String,
    color: Color,
    troll: Boolean) extends Member {

  val playerIdOption = playerId.some
}

case class Watcher(
    channel: JsChannel,
    userId: Option[String],
    color: Color,
    troll: Boolean) extends Member {

  val playerIdOption = none
}

case class Join(
  uid: String,
  user: Option[User],
  version: Int,
  color: Color,
  playerId: Option[String])
case class Connected(enumerator: JsEnumerator, member: Member)
case class GetEventsSince(version: Int)
case class MaybeEvents(events: Option[List[VersionedEvent]])
case class AddEvents(events: List[Event])
case class IsConnectedOnGame(gameId: String, color: Color)
case class IsGone(gameId: String, color: Color)
case object AnalysisAvailable
case class Ack(uid: String)

package round {

  case class Play(
    playerId: String,
    orig: String,
    dest: String,
    prom: Option[String] = None,
    blur: Boolean = false,
    lag: Int = 0)

  case class PlayResult(events: Events, fen: String, lastMove: Option[String])

  case class Abort(playerId: String)
  case class Resign(playerId: String)
  case class ResignForce(playerId: String)
  case class DrawClaim(playerId: String)
  case class DrawAccept(playerId: String)
  case class DrawOffer(playerId: String)
  case class DrawCancel(playerId: String)
  case class DrawDecline(playerId: String)
  case class RematchCancel(playerId: String)
  case class RematchDecline(playerId: String)
  case class TakebackAccept(playerId: String)
  case class TakebackOffer(playerId: String)
  case class TakebackCancel(playerId: String)
  case class TakebackDecline(playerId: String)
  case class Moretime(playerId: String)
  case object Outoftime
}
