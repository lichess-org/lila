package lila.round
package actorApi

import scala.concurrent.Promise

import chess.format.Uci
import chess.{ MoveMetrics, Color }

import lila.common.{ IpAddress, IsMobile }
import lila.socket.Socket.{ SocketVersion, Uid }
import lila.socket.SocketMember
import lila.user.User

case class EventList(events: List[lila.game.Event])

sealed trait Member extends SocketMember {

  val color: Color
  val playerIdOption: Option[String]
  val troll: Boolean
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
    userTv: Option[User.ID]
  ): Member = {
    val userId = user map (_.id)
    val troll = user.??(_.troll)
    playerIdOption.fold[Member](Watcher(channel, userId, color, troll, userTv)) { playerId =>
      Owner(channel, userId, playerId, color, troll)
    }
  }
}

case class Owner(
    channel: JsChannel,
    userId: Option[User.ID],
    playerId: String,
    color: Color,
    troll: Boolean
) extends Member {

  val playerIdOption = playerId.some
  val userTv = none

  override def toString = s"$color owner: ${userId | "anon"}"
}

case class Watcher(
    channel: JsChannel,
    userId: Option[User.ID],
    color: Color,
    troll: Boolean,
    userTv: Option[User.ID]
) extends Member {

  val playerIdOption = none

  override def toString = s"$color watcher: ${userId | "anon"}"
}

case class Join(
    uid: Uid,
    user: Option[User],
    color: Color,
    playerId: Option[String],
    userTv: Option[UserTv],
    version: Option[SocketVersion],
    mobile: IsMobile,
    promise: Promise[Connected]
)
case class UserTv(userId: User.ID, reload: Fu[Boolean])
case class Connected(enumerator: JsEnumerator, member: Member)
case class Bye(color: Color)
case class IsGone(color: Color, promise: Promise[Boolean])
case class GetSocketStatus(promise: Promise[SocketStatus])
case class SocketStatus(
    version: SocketVersion,
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
