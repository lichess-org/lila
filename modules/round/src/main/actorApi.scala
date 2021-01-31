package lila.round
package actorApi

import scala.concurrent.Promise
import scala.concurrent.duration._

import chess.format.Uci
import chess.{ Color, MoveMetrics }

import lila.common.IpAddress
import lila.game.Game.PlayerId
import lila.socket.Socket.SocketVersion

case class ByePlayer(playerId: PlayerId)
case class GetSocketStatus(promise: Promise[SocketStatus])
case class SocketStatus(
    version: SocketVersion,
    whiteOnGame: Boolean,
    whiteIsGone: Boolean,
    blackOnGame: Boolean,
    blackIsGone: Boolean
) {
  def onGame(color: Color)     = color.fold(whiteOnGame, blackOnGame)
  def isGone(color: Color)     = color.fold(whiteIsGone, blackIsGone)
  def colorsOnGame: Set[Color] = Color.all.filter(onGame).toSet
}
case class RoomCrowd(white: Boolean, black: Boolean)
case class BotConnected(color: Color, v: Boolean)

package round {

  case class HumanPlay(
      playerId: PlayerId,
      uci: Uci,
      blur: Boolean,
      moveMetrics: MoveMetrics = MoveMetrics(),
      promise: Option[Promise[Unit]] = None
  )

  case class PlayResult(events: Events, fen: String, lastMove: Option[String])

  case object AbortForce
  case object Threefold
  case object ResignAi
  case class ResignForce(playerId: PlayerId)
  case class DrawForce(playerId: PlayerId)
  case class DrawClaim(playerId: PlayerId)
  case class DrawYes(playerId: PlayerId)
  case class DrawNo(playerId: PlayerId)
  case class TakebackYes(playerId: PlayerId)
  case class TakebackNo(playerId: PlayerId)
  object Moretime { val defaultDuration = 15.seconds }
  case class Moretime(playerId: PlayerId, seconds: FiniteDuration = Moretime.defaultDuration)
  case object QuietFlag
  case class ClientFlag(color: Color, fromPlayerId: Option[PlayerId])
  case object Abandon
  case class ForecastPlay(lastMove: chess.Move)
  case class Cheat(color: Color)
  case class HoldAlert(playerId: PlayerId, mean: Int, sd: Int, ip: IpAddress)
  case class GoBerserk(color: Color, promise: Promise[Boolean])
  case object NoStart
  case object StartClock
  case object TooManyPlies
}
