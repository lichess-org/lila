package lila.round
package actorApi

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Promise

import chess.Color
import chess.format.Uci

import lila.game.{ Game, Event, PlayerRef }
import lila.socket.SocketMember
import lila.user.User

case class EventList(events: List[Event])

sealed trait Member extends SocketMember {

  val color: Color
  val playerIdOption: Option[String]
  val troll: Boolean
  val ip: String
  val userTv: Option[String]

  def owner = playerIdOption.isDefined
  def watcher = !owner

  def onUserTv(userId: String) = userTv == Some(userId)
}

object Member {
  def apply(
    channel: JsChannel,
    user: Option[User],
    color: Color,
    playerIdOption: Option[String],
    ip: String,
    userTv: Option[String]): Member = {
    val userId = user map (_.id)
    val troll = user.??(_.troll)
    playerIdOption.fold[Member](Watcher(channel, userId, color, troll, ip, userTv)) { playerId =>
      Owner(channel, userId, playerId, color, troll, ip)
    }
  }
}

case class Owner(
    channel: JsChannel,
    userId: Option[String],
    playerId: String,
    color: Color,
    troll: Boolean,
    ip: String) extends Member {

  val playerIdOption = playerId.some
  val userTv = none
}

case class Watcher(
    channel: JsChannel,
    userId: Option[String],
    color: Color,
    troll: Boolean,
    ip: String,
    userTv: Option[String]) extends Member {

  val playerIdOption = none
}

case class Join(
  uid: String,
  user: Option[User],
  color: Color,
  playerId: Option[String],
  ip: String,
  userTv: Option[String])
case class Connected(enumerator: JsEnumerator, member: Member)
case class Bye(color: Color)
case class IsGone(color: Color)
case object AnalysisAvailable
case object GetSocketStatus
case class SocketStatus(
    version: Int,
    whiteOnGame: Boolean,
    whiteIsGone: Boolean,
    blackOnGame: Boolean,
    blackIsGone: Boolean) {
  def onGame(color: Color) = color.fold(whiteOnGame, blackOnGame)
  def isGone(color: Color) = color.fold(whiteIsGone, blackIsGone)
}
case class SetGame(game: Option[lila.game.Game])

package round {

case class HumanPlay(
    playerId: String,
    uci: Uci,
    blur: Boolean,
    lag: FiniteDuration,
    promise: Option[Promise[Unit]] = None) {

  val atNanos = nowNanos
}

case class FishnetPlay(uci: Uci)

case class PlayResult(events: Events, fen: String, lastMove: Option[String])

case class Abort(playerId: String)
case object AbortForMaintenance
case object Threefold
case class Resign(playerId: String)
case class ResignForce(playerId: String)
case class NoStartColor(color: Color)
case class DrawForce(playerId: String)
case class DrawClaim(playerId: String)
case class DrawYes(playerId: String)
case class DrawNo(playerId: String)
case object DrawForce
case class RematchYes(playerId: String)
case class RematchNo(playerId: String)
case class TakebackYes(playerId: String)
case class TakebackNo(playerId: String)
case class Moretime(playerId: String)
case object Outoftime
case object Abandon
case class ForecastPlay(lastMove: chess.Move)
case class Cheat(color: Color)
case class HoldAlert(playerId: String, mean: Int, sd: Int, ip: String)
case class GoBerserk(color: Color)
case class TournamentStanding(id: String)
}

private[round] case object GetNbRounds
private[round] case object NotifyCrowd
