package lila
package lobby

import socket.SocketMember
import timeline.Entry
import game.DbGame

import scalaz.effects.IO

case class Member(
    channel: JsChannel,
    username: Option[String],
    hookOwnerId: Option[String]) extends SocketMember {

  def ownsHook(hook: Hook) = Some(hook.ownerId) == hookOwnerId
}

case class ReloadTournaments(html: String)
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
case class Talk(u: String, txt: String)
case class SysTalk(txt: String)
case class Connected(
  enumerator: JsEnumerator, 
  channel: JsChannel)
