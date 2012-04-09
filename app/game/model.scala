package lila
package game

import chess.Color
import model._

import scalaz.effects.IO

sealed trait Member {
  val channel: Channel
  val color: Color
  val username: Option[String]
  val owner: Boolean
}
object Member {
  def apply(
    channel: Channel,
    color: Color,
    owner: Boolean,
    username: Option[String]): Member =
    if (owner) Owner(channel, color, username)
    else Watcher(channel, color, username)
}

case class Owner(
    channel: Channel,
    color: Color,
    username: Option[String]) extends Member {

  val owner = true
}

case class Watcher(
    channel: Channel,
    color: Color,
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
