package lila.lobby
package actorApi

import lila.socket.SocketMember
import lila.timeline.Entry
import lila.game.Game

case class Member(
    channel: JsChannel,
    userId: Option[String],
    hookOwnerId: Option[String]) extends SocketMember {

  def ownsHook(hook: Hook) = hookOwnerId zmap (hook.ownerId ==)
}

case class Connected(enumerator: JsEnumerator, member: Member)
case class ReloadTournaments(html: String)
case class WithHooks(op: Iterable[String] ⇒ Funit)
case class AddHook(hook: Hook)
case class RemoveHook(hook: Hook)
case class BiteHook(hook: Hook, game: Game)
case class AddEntry(entry: Entry)
case class Join(
  uid: String,
  userId: Option[String],
  version: Int,
  hookOwnerId: Option[String])
case class Talk(u: String, txt: String)
case class SysTalk(txt: String)
case class UnTalk(r: util.matching.Regex)
