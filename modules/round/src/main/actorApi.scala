package lila.round
package actorApi

import scala.concurrent.Promise

import shogi.format.usi.Usi
import shogi.{ Color, LagMetrics }

import lila.common.IpAddress
import lila.game.Game.PlayerId
import lila.socket.Socket.SocketVersion

case class ByePlayer(playerId: PlayerId)
case class GetSocketStatus(promise: Promise[SocketStatus])
case class SocketStatus(
    version: SocketVersion,
    senteOnGame: Boolean,
    senteIsGone: Boolean,
    goteOnGame: Boolean,
    goteIsGone: Boolean
) {
  def onGame(color: Color)     = color.fold(senteOnGame, goteOnGame)
  def isGone(color: Color)     = color.fold(senteIsGone, goteIsGone)
  def colorsOnGame: Set[Color] = Color.all.filter(onGame).toSet
}
case class RoomCrowd(sente: Boolean, gote: Boolean)
case class BotConnected(color: Color, v: Boolean)

package round {

  case class HumanPlay(
      playerId: PlayerId,
      usi: Usi,
      blur: Boolean,
      lagMetrics: LagMetrics = LagMetrics(),
      promise: Option[Promise[Unit]] = None
  )

  case class PlayResult(events: Events, sfen: String, lastUsi: Option[String])

  case object AbortForce
  case object ResignAi
  case class ResignForce(playerId: PlayerId)
  case class DrawForce(playerId: PlayerId)
  case class DrawClaim(playerId: PlayerId)
  case class DrawYes(playerId: PlayerId)
  case class DrawNo(playerId: PlayerId)
  case class PauseYes(playerId: PlayerId)
  case class PauseNo(playerId: PlayerId)
  case class ResumeYes(playerId: PlayerId)
  case class ResumeNo(playerId: PlayerId)
  case class TakebackYes(playerId: PlayerId)
  case class TakebackNo(playerId: PlayerId)
  case class Moretime(playerId: PlayerId)
  case object QuietFlag
  case class ClientFlag(color: Color, fromPlayerId: Option[PlayerId])
  case object Abandon
  case class ForecastPlay(lastUsi: Usi)
  case class Cheat(color: Color)
  case class HoldAlert(playerId: PlayerId, mean: Int, sd: Int, ip: IpAddress)
  case class GoBerserk(color: Color, promise: Promise[Boolean])
  case object NoStart
  case object TooManyPlies
}
