package lila.round
package actorApi

import scala.concurrent.Promise

import chess.format.Uci
import chess.{ MoveMetrics, Color }

import lila.common.IpAddress
import lila.game.Event
import lila.socket.Socket.Uid
import lila.socket.SocketMember
import lila.user.User

case class EventList(events: List[Event])

sealed trait Member extends SocketMember {

  val color: Color
  val playerIdOption: Option[String]
  val troll: Boolean
  val ip: IpAddress
  val userTv: Option[User.ID]

  def owner = playerIdOption.isDefined
  def watcher = !owner

  def onUserTv(userId: User.ID) = userTv has userId
}

object Member {
  def apply(
    channel: JsChannel,
    user: Option[User],
    color: Color,
    playerIdOption: Option[String],
    ip: IpAddress,
    userTv: Option[User.ID]
  ): Member = {
    val userId = user map (_.id)
    val troll = user.??(_.troll)
    playerIdOption.fold[Member](Watcher(channel, userId, color, troll, ip, userTv)) { playerId =>
      Owner(channel, userId, playerId, color, troll, ip)
    }
  }
}

case class Owner(
    channel: JsChannel,
    userId: Option[User.ID],
    playerId: String,
    color: Color,
    troll: Boolean,
    ip: IpAddress
) extends Member {

  val playerIdOption = playerId.some
  val userTv = none
}

case class Watcher(
    channel: JsChannel,
    userId: Option[User.ID],
    color: Color,
    troll: Boolean,
    ip: IpAddress,
    userTv: Option[User.ID]
) extends Member {

  val playerIdOption = none
}

case class Join(
    uid: Uid,
    user: Option[User],
    color: Color,
    playerId: Option[String],
    ip: IpAddress,
    userTv: Option[User.ID]
)
case class Connected(enumerator: JsEnumerator, member: Member)
case class Bye(color: Color)
case class IsGone(color: Color)
case object GetSocketStatus
case class SocketStatus(
    version: Int,
    whiteOnGame: Boolean,
    whiteIsGone: Boolean,
    blackOnGame: Boolean,
    blackIsGone: Boolean
) {
  def onGame(color: Color) = color.fold(whiteOnGame, blackOnGame)
  def isGone(color: Color) = color.fold(whiteIsGone, blackIsGone)
  def colorsOnGame: Set[Color] = Color.all.filter(onGame).toSet
}
case class SetGame(game: Option[lila.game.Game])
case object GetGame

package round {

  case class HumanPlay(
      playerId: String,
      uci: Uci,
      blur: Boolean,
      moveMetrics: MoveMetrics = MoveMetrics(),
      promise: Option[Promise[Unit]] = None
  ) {

    val trace = lila.mon.round.move.trace.create
  }

  case class PlayResult(events: Events, fen: String, lastMove: Option[String])

  case object AbortForMaintenance
  case object AbortForce
  case object Threefold
  case class Resign(playerId: String)
  case object ResignAi
  case class ResignForce(playerId: String)
  case class DrawForce(playerId: String)
  case class DrawClaim(playerId: String)
  case class DrawYes(playerId: String)
  case class DrawNo(playerId: String)
  case class TakebackYes(playerId: String)
  case class TakebackNo(playerId: String)
  case class Moretime(playerId: String)
  case object QuietFlag
  case class ClientFlag(color: Color, fromPlayerId: Option[String])
  case object Abandon
  case class ForecastPlay(lastMove: chess.Move)
  case class Cheat(color: Color)
  case class HoldAlert(playerId: String, mean: Int, sd: Int, ip: IpAddress)
  case class GoBerserk(color: Color)
  case object NoStart
  case object TooManyPlies
}

private[round] case object GetNbRounds
private[round] case object NotifyCrowd
