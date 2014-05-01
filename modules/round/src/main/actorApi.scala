package lila.round
package actorApi

import scala.concurrent.duration.FiniteDuration

import chess.Color

import lila.game.{ Game, Event, PlayerRef }
import lila.socket.SocketMember
import lila.user.User

sealed trait Member extends SocketMember {

  val color: Color
  val playerIdOption: Option[String]
  val troll: Boolean
  val ip: String

  def owner = playerIdOption.isDefined
  def watcher = !owner
}

object Member {
  def apply(
    channel: JsChannel,
    user: Option[User],
    color: Color,
    playerIdOption: Option[String],
    ip: String): Member = {
    val userId = user map (_.id)
    val troll = user.??(_.troll)
    playerIdOption.fold[Member](Watcher(channel, userId, color, troll, ip)) { playerId =>
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
}

case class Watcher(
    channel: JsChannel,
    userId: Option[String],
    color: Color,
    troll: Boolean,
    ip: String) extends Member {

  val playerIdOption = none
}

case class Join(
  uid: String,
  user: Option[User],
  version: Int,
  color: Color,
  playerId: Option[String],
  ip: String)
case class Connected(enumerator: JsEnumerator, member: Member)
case class Bye(color: Color)
case class IsGone(color: Color)
case object AnalysisAvailable
case class Ack(uid: String)

package round {

  case class HumanPlay(
    playerId: String,
    ip: String,
    orig: String,
    dest: String,
    prom: Option[String],
    blur: Boolean,
    lag: FiniteDuration,
    onFailure: Exception => Unit)
  case object AiPlay

  case class PlayResult(events: Events, fen: String, lastMove: Option[String])

  case class Abort(playerId: String)
  case class Resign(playerId: String)
  case class ResignColor(color: Color)
  case class ResignForce(playerId: String)
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
  case class Cheat(color: Color)
  case class HoldAlert(playerId: String, mean: Int, sd: Int)
}
