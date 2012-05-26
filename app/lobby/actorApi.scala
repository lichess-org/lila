package lila
package lobby

import socket.SocketMember
import timeline.Entry
import game.DbGame

import scalaz.effects.IO

case class Member(
    channel: Channel,
    username: Option[String],
    hookOwnerId: Option[String]) extends SocketMember {

  def ownsHook(hook: Hook) = Some(hook.ownerId) == hookOwnerId
}

case class WithHooks(op: Iterable[String] ⇒ IO[Unit])
case class AddHook(hook: Hook)
case class RemoveHook(hook: Hook)
case class BiteHook(hook: Hook, game: DbGame)
case class AddEntry(entry: Entry)
case class Join(
  uid: String,
  username: Option[String],
  version: Int,
  hookOwnerId: Option[String])
case class Talk(txt: String, u: String)
case class Connected(channel: Channel)
