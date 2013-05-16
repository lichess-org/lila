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
    troll: Boolean) extends SocketMember {

  def ownsHook(hook: Hook) = hookOwnerId zmap (hook.ownerId ==)
}

object Member {
  def apply(channel: JsChannel, user: Option[User], hookOwnerId: Option[String]): Member = Member(
    channel = channel,
    userId = user map (_.id),
    hookOwnerId = hookOwnerId,
    troll = user.zmap(_.troll))
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
