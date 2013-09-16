package lila.lobby
package actorApi

import lila.game.Game
import lila.socket.SocketMember
import lila.timeline.GameEntry
import lila.user.User

case class Member(
    channel: JsChannel,
    userId: Option[String],
    troll: Boolean) extends SocketMember 

object Member {
  def apply(channel: JsChannel, user: Option[User]): Member = Member(
    channel = channel,
    userId = user map (_.id),
    troll = user.??(_.troll))
}

case class Connected(enumerator: JsEnumerator, member: Member)
case class WithHooks(op: Iterable[String] ⇒ Unit)
case class AddHook(hook: Hook, user: Option[User])
case class RemoveHook(hookId: String)
case class CancelHook(uid: String)
case class BiteHook(hookId: String, uid: String, userId: Option[String])
case class JoinHook(uid: String, hook: Hook, game: Game)
case class Join(uid: String, user: Option[User])
case object Resync
case class HookIds(ids: List[String])

case object GetOpen
