package lila.round
package actorApi

import chess.format.Uci
import chess.{ Color, MoveMetrics }

import lila.common.IpAddress
import lila.socket.SocketVersion

case class ByePlayer(playerId: GamePlayerId)
case class GetSocketStatus(promise: Promise[SocketStatus])
case class GetGameAndSocketStatus(promise: Promise[GameAndSocketStatus])
case class SocketStatus(
    version: SocketVersion,
    whiteOnGame: Boolean,
    whiteIsGone: Boolean,
    blackOnGame: Boolean,
    blackIsGone: Boolean
):
  def onGame(color: Color)     = color.fold(whiteOnGame, blackOnGame)
  def isGone(color: Color)     = color.fold(whiteIsGone, blackIsGone)
  def colorsOnGame: Set[Color] = Color.all.filter(onGame).toSet
object SocketStatus:
  val default = SocketStatus(SocketVersion(0), false, false, false, false)
case class GameAndSocketStatus(game: lila.game.Game, socket: SocketStatus)
case class RoomCrowd(white: Boolean, black: Boolean)
case class BotConnected(color: Color, v: Boolean)

package round:

  case class HumanPlay(
      playerId: GamePlayerId,
      uci: Uci,
      blur: Boolean,
      moveMetrics: MoveMetrics = MoveMetrics(),
      promise: Option[Promise[Unit]] = None
  )

  case class PlayResult(events: Events, fen: String, lastMove: Option[String])

  case object AbortForce
  case object Threefold
  case object ResignAi
  case class ResignForce(playerId: GamePlayerId)
  case class DrawForce(playerId: GamePlayerId)
  case class DrawClaim(playerId: GamePlayerId)
  case class Blindfold(playerId: GamePlayerId, blindfold: Boolean)
  case class Draw(playerId: GamePlayerId, draw: Boolean)
  case class Takeback(playerId: GamePlayerId, takeback: Boolean)
  object Moretime:
    val defaultDuration = 15.seconds
  case class Moretime(playerId: GamePlayerId, seconds: FiniteDuration = Moretime.defaultDuration)
  case object QuietFlag
  case class ClientFlag(color: Color, fromPlayerId: Option[GamePlayerId])
  case object Abandon
  case class ForecastPlay(lastMove: chess.Move)
  case class Cheat(color: Color)
  case class HoldAlert(playerId: GamePlayerId, mean: Int, sd: Int, ip: IpAddress)
  case class GoBerserk(color: Color, promise: Promise[Boolean])
  case object NoStart
  case object StartClock
  case object TooManyPlies
