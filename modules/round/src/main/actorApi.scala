package lila.round

import chess.format.Uci
import chess.{ Color, MoveMetrics }

import lila.core.socket.SocketVersion

private class HumanPlay(
    val playerId: GamePlayerId,
    val uci: Uci,
    val blur: Boolean,
    val moveMetrics: chess.MoveMetrics = chess.MoveMetrics(),
    val promise: Option[Promise[Unit]] = None
)
private case class ByePlayer(playerId: GamePlayerId)
private case class GetSocketStatus(promise: Promise[SocketStatus])
private case class GetGameAndSocketStatus(val promise: Promise[GameAndSocketStatus])
private class SocketStatus(
    val version: SocketVersion,
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
private class GameAndSocketStatus(val game: Game, val socket: SocketStatus)
private case class RoomCrowd(val white: Boolean, val black: Boolean)
