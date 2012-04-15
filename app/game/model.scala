package lila
package game

import chess.Color
import model._

import akka.actor.ActorRef
import scalaz.effects.IO

sealed trait Member {
  val channel: Channel
  val ref: PovRef
  val username: Option[String]
  val owner: Boolean

  def watcher = !owner
  def gameId = ref.gameId
  def color = ref.color
  def className = owner.fold("Owner", "Watcher")
  override def toString = "%s(%s-%s,%s)".format(className, gameId, color, username)
}
object Member {
  def apply(
    channel: Channel,
    ref: PovRef,
    owner: Boolean,
    username: Option[String]): Member =
    if (owner) Owner(channel, ref, username)
    else Watcher(channel, ref, username)
}

case class Owner(
    channel: Channel,
    ref: PovRef,
    username: Option[String]) extends Member {

  val owner = true
}

case class Watcher(
    channel: Channel,
    ref: PovRef,
    username: Option[String]) extends Member {

  val owner = false
}

case class Join(
  uid: String,
  version: Int,
  color: Color,
  owner: Boolean,
  username: Option[String])
case class Quit(uid: String)
case class Connected(member: Member)
case class Events(events: List[Event])
case object GetVersion
case class Version(version: Int)
case class WithMembers(op: Iterable[Member] ⇒ IO[Unit])
case class IfEmpty(op: IO[Unit])
case class WithHubs(op: Map[String, ActorRef] ⇒ IO[Unit])
case object ClockSync
case class IsConnected(color: Color)
