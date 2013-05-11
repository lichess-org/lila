package lila.lobby
package actorApi

import lila.socket.SocketMember
import lila.timeline.Entry
import lila.user.User
import lila.game.Game

case class Member(
    channel: JsChannel,
    userId: Option[String],
    hookOwnerId: Option[String],
    muted: Boolean) extends SocketMember {

  def ownsHook(hook: Hook) = hookOwnerId zmap (hook.ownerId ==)

  def canChat = !muted
}

object Member {
  def apply(channel: JsChannel, user: Option[User], hookOwnerId: Option[String]): Member = Member(
    channel = channel,
    userId = user map (_.id),
    hookOwnerId = hookOwnerId,
    muted = user.zmap(_.muted))
}

case class Connected(enumerator: JsEnumerator, member: Member)
case class WithHooks(op: Iterable[String] ⇒ Unit)
case class AddHook(hook: Hook)
case class RemoveHook(hook: Hook)
case class BiteHook(hook: Hook, game: Game)
case class AddEntry(entry: Entry)
case class Join(
  uid: String,
  user: Option[User],
  hookOwnerId: Option[String])
