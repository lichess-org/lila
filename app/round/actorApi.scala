package lila
package round

import chess.Color
import socket.SocketMember
import game.PovRef

import akka.actor.ActorRef
import scalaz.effects.IO

sealed trait Member extends SocketMember {

  val ref: PovRef
  val owner: Boolean

  def watcher = !owner
  def gameId = ref.gameId
  def color = ref.color
  def className = owner.fold("Owner", "Watcher")
  override def toString = "%s(%s-%s,%s)".format(className, gameId, color, username)
}

object Member {
  def apply(
    channel: JsChannel,
    username: Option[String],
    ref: PovRef,
    owner: Boolean): Member =
    if (owner) Owner(channel, username, ref)
    else Watcher(channel, username, ref)
}

case class Owner(
    channel: JsChannel,
    username: Option[String],
    ref: PovRef) extends Member {

  val owner = true
}

case class Watcher(
    channel: JsChannel,
    username: Option[String],
    ref: PovRef) extends Member {

  val owner = false
}

case class Join(
  uid: String,
  username: Option[String],
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
