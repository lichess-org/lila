package lila
package round

import chess.Color
import socket.SocketMember
import game.PovRef
import user.User

import akka.actor.ActorRef
import scalaz.effects.IO

sealed trait Member extends SocketMember {

  val ref: PovRef
  val owner: Boolean
  val muted: Boolean

  def watcher = !owner
  def gameId = ref.gameId
  def color = ref.color
  def className = owner.fold("Owner", "Watcher")
  def canChat = !muted
  override def toString = "%s(%s-%s,%s)".format(className, gameId, color, username)
}

object Member {
  def apply(
    channel: JsChannel,
    user: Option[User],
    ref: PovRef,
    owner: Boolean): Member = {
    val username = user map (_.username)
    val muted = user.fold(_.muted, false)
    owner.fold(
      Owner(channel, username, ref, muted),
      Watcher(channel, username, ref, muted))
  }
}

case class Owner(
    channel: JsChannel,
    username: Option[String],
    ref: PovRef,
    muted: Boolean) extends Member {

  val owner = true
}

case class Watcher(
    channel: JsChannel,
    username: Option[String],
    ref: PovRef,
    muted: Boolean) extends Member {

  val owner = false
}

case class Join(
  uid: String,
  user: Option[User],
  version: Int,
  color: Color,
  owner: Boolean)
case class Connected(
  enumerator: JsEnumerator,
  member: Member)
case class Events(events: List[Event])
case class GameEvents(gameId: String, events: List[Event])
case class GetGameVersion(gameId: String)
case object ClockSync
case class IsConnectedOnGame(gameId: String, color: Color)
case class IsGone(gameId: String, color: Color)
case class CloseGame(gameId: String)
case class GetHub(gameId: String)
case object HubTimeout
case object GetNbHubs
case class AnalysisAvailable(gameId: String)
case class Ack(uid: String)
case class FinishGame(gameId: String)
