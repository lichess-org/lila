package lila.lobby
package actorApi

import lila.game.Game
import lila.socket.SocketMember
import lila.socket.Socket.Uid
import lila.user.User

private[lobby] case class LobbyUser(
  id: String,
  username: String,
  lame: Boolean,
  ratingMap: Map[String, Int],
  blocking: Set[String])

private[lobby] object LobbyUser {

  def make(user: User, blocking: Set[String]) = LobbyUser(
    id = user.id,
    username = user.username,
    lame = user.lame,
    ratingMap = user.perfs.ratingMap,
    blocking = blocking)
}

private[lobby] case class Member(
    channel: JsChannel,
    user: Option[LobbyUser],
    uid: String,
    mobile: Boolean) extends SocketMember {

  val userId = user.map(_.id)
  val troll = false
}

private[lobby] object Member {

  def apply(channel: JsChannel, user: Option[User], blocking: Set[String], uid: String, mobile: Boolean): Member = Member(
    channel = channel,
    user = user map { LobbyUser.make(_, blocking) },
    uid = uid,
    mobile = mobile)
}

private[lobby] case class HookMeta(hookId: Option[String] = None)

private[lobby] case class Messadata(hook: Option[Hook] = None)

private[lobby] case class Connected(enumerator: JsEnumerator, member: Member)
private[lobby] case class WithHooks(op: Iterable[String] => Unit)
private[lobby] case class SaveHook(msg: AddHook)
private[lobby] case class SaveSeek(msg: AddSeek)
private[lobby] case class RemoveHook(hookId: String)
private[lobby] case class RemoveSeek(seekId: String)
private[lobby] case class RemoveHooks(hooks: Set[Hook])
private[lobby] object SendHookRemovals
private[lobby] case class CancelHook(uid: String)
private[lobby] case class CancelSeek(seekId: String, user: LobbyUser)
private[lobby] case class BiteHook(hookId: String, uid: String, user: Option[LobbyUser])
private[lobby] case class BiteSeek(seekId: String, user: LobbyUser)
private[lobby] case class JoinHook(uid: String, hook: Hook, game: Game, creatorColor: chess.Color)
private[lobby] case class JoinSeek(userId: String, seek: Seek, game: Game, creatorColor: chess.Color)
private[lobby] case class Join(uid: Uid, user: Option[User], blocking: Set[String], mobile: Boolean)
private[lobby] case object Resync
private[lobby] case class HookIds(ids: Vector[String])

private[lobby] case class SetIdle(uid: String, value: Boolean)

private[lobby] case class HookSub(member: Member, value: Boolean)
private[lobby] case class AllHooksFor(member: Member, hooks: Vector[Hook])

private[lobby] case object GetUids
private[lobby] case class SocketUids(uids: Set[String])

case class AddHook(hook: Hook)
case class AddSeek(seek: Seek)
