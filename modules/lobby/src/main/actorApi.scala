package lila.lobby
package actorApi

import play.api.libs.json._
import scala.concurrent.Promise

import lila.game.Game
import lila.socket.Socket.{ Sri, Sris }
import lila.socket.{ SocketMember, DirectSocketMember, RemoteSocketMember }
import lila.user.User

private[lobby] sealed trait LobbySocketMember extends SocketMember {
  val user: Option[LobbyUser]
  val sri: Sri
  def bot = user.exists(_.bot)
}

private[lobby] case class LobbyDirectSocketMember(
    channel: JsChannel,
    user: Option[LobbyUser],
    sri: Sri
) extends LobbySocketMember with DirectSocketMember {
  val userId = user.map(_.id)
}

private[lobby] case class LobbyRemoteSocketMember(
    push: SocketMember.Push,
    user: Option[LobbyUser],
    sri: Sri
) extends LobbySocketMember with RemoteSocketMember {
  val userId = user.map(_.id)
}

private[lobby] object LobbySocketMember {

  def apply(channel: JsChannel, user: Option[User], blocking: Set[String], sri: Sri): LobbyDirectSocketMember =
    LobbyDirectSocketMember(
      channel = channel,
      user = user map { LobbyUser.make(_, blocking) },
      sri = sri
    )

  def apply(push: SocketMember.Push, user: Option[User], blocking: Set[String], sri: Sri): LobbyRemoteSocketMember =
    LobbyRemoteSocketMember(
      push = push,
      user = user map { LobbyUser.make(_, blocking) },
      sri = sri
    )
}

private[lobby] case class HookMeta(hookId: Option[String] = None)

private[lobby] case class Messadata(hook: Option[Hook] = None)

private[lobby] case class Connected(enumerator: JsEnumerator, member: LobbySocketMember)
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
private[lobby] case class Join(sri: Sri, user: Option[User], blocking: Set[String], promise: Promise[Connected])
private[lobby] case class JoinRemote(member: LobbyRemoteSocketMember)
private[lobby] case class LeaveRemote(sri: Sri)
private[lobby] case object LeaveAllRemote
private[lobby] case object Resync
private[lobby] case class HookIds(ids: Vector[String])

private[lobby] case class SetIdle(sri: Sri, value: Boolean)

private[lobby] case class HookSub(member: LobbySocketMember, value: Boolean)
private[lobby] case class AllHooksFor(member: LobbySocketMember, hooks: Vector[Hook])

private[lobby] case class GetSrisP(promise: Promise[Sris])
private[lobby] case class GetRemoteMember(sri: Sri, promise: Promise[Option[LobbyRemoteSocketMember]])

private[lobby] case class LobbySocketTellAll(msg: JsObject)
private[lobby] case class LobbySocketTellActive(msg: JsObject)
private[lobby] case class LobbySocketTellSris(sris: Iterable[Sri], msg: JsObject)

case class AddHook(hook: Hook)
case class AddSeek(seek: Seek)
