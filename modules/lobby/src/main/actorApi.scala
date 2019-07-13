package lila.lobby
package actorApi

import scala.concurrent.Promise

import lila.game.Game
import lila.socket.SocketMember
import lila.socket.Socket.{ Sri, Sris }
import lila.user.User

private[lobby] case class Member(
    channel: JsChannel,
    user: Option[LobbyUser],
    sri: Sri,
    mobile: Boolean
) extends SocketMember {

  val userId = user.map(_.id)
}

private[lobby] object Member {

  def apply(channel: JsChannel, user: Option[User], blocking: Set[String], sri: Sri, mobile: Boolean): Member = Member(
    channel = channel,
    user = user map { LobbyUser.make(_, blocking) },
    sri = sri,
    mobile = mobile
  )
}

private[lobby] case class HookMeta(hookId: Option[String] = None)

private[lobby] case class Messadata(hook: Option[Hook] = None)

private[lobby] case class Connected(enumerator: JsEnumerator, member: Member)
private[lobby] case class WithHooks(op: Iterable[String] => Unit)
private[lobby] case class SaveSeek(msg: AddSeek)
private[lobby] case class RemoveHook(hookId: String)
private[lobby] case class RemoveSeek(seekId: String)
private[lobby] case class RemoveHooks(hooks: Set[Hook])
private[lobby] object SendHookRemovals
private[lobby] case class CancelHook(sri: Sri)
private[lobby] case class CancelSeek(seekId: String, user: LobbyUser)
private[lobby] case class BiteHook(hookId: String, sri: Sri, user: Option[LobbyUser])
private[lobby] case class BiteSeek(seekId: String, user: LobbyUser)
private[lobby] case class JoinHook(sri: Sri, hook: Hook, game: Game, creatorColor: chess.Color)
private[lobby] case class JoinSeek(userId: String, seek: Seek, game: Game, creatorColor: chess.Color)
private[lobby] case class Join(sri: Sri, user: Option[User], blocking: Set[String], mobile: Boolean, promise: Promise[Connected])
private[lobby] case object Resync
private[lobby] case class HookIds(ids: Vector[String])

private[lobby] case class SetIdle(sri: Sri, value: Boolean)

private[lobby] case class HookSub(member: Member, value: Boolean)
private[lobby] case class AllHooksFor(member: Member, hooks: Vector[Hook])

private[lobby] case class GetSrisP(promise: Promise[Sris])

case class AddHook(hook: Hook)
case class AddSeek(seek: Seek)
