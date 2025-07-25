package lila.round

import chess.format.Uci
import chess.{ Color, MoveMetrics }

import lila.core.socket.SocketVersion

private case class HumanPlay(
    playerId: GamePlayerId,
    uci: Uci,
    blur: Boolean,
    moveMetrics: chess.MoveMetrics = chess.MoveMetrics(),
    promise: Option[Promise[Unit]] = None
)
private case class ByePlayer(playerId: GamePlayerId)
private case class GetSocketStatus(promise: Promise[SocketStatus])
private case class GetGameAndSocketStatus(promise: Promise[GameAndSocketStatus])
private case class SocketStatus(
    version: SocketVersion,
    whiteOnGame: Boolean,
    whiteIsGone: Boolean,
    blackOnGame: Boolean,
    blackIsGone: Boolean
):
  def onGame(color: Color) = color.fold(whiteOnGame, blackOnGame)
  def isGone(color: Color) = color.fold(whiteIsGone, blackIsGone)
  def colorsOnGame: Set[Color] = Color.all.filter(onGame).toSet
private object SocketStatus:
  val default = SocketStatus(SocketVersion(0), false, false, false, false)
private case class GameAndSocketStatus(game: Game, socket: SocketStatus)
private case class RoomCrowd(white: Boolean, black: Boolean)
