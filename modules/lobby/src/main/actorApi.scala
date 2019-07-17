package lila.lobby
package actorApi

import scala.concurrent.Promise

import lila.game.Game
import lila.socket.{ SocketMember, DirectSocketMember, RemoteSocketMember }
import lila.socket.Socket.{ Sri, Sris }
import lila.user.User

private[lobby] sealed trait LobbySocketMember extends SocketMember {
  val user: Option[LobbyUser]
  val sri: Sri
  val mobile: Boolean
}

private[lobby] case class LobbyDirectSocketMember(
    channel: JsChannel,
    user: Option[LobbyUser],
    sri: Sri,
    mobile: Boolean
) extends LobbySocketMember with DirectSocketMember {
  val userId = user.map(_.id)
}

private[lobby] case class LobbyRemoteSocketMember(
    bus: lila.common.Bus,
    user: Option[LobbyUser],
    sri: Sri,
    mobile: Boolean
) extends LobbySocketMember with RemoteSocketMember {
  val userId = user.map(_.id)
}

private[lobby] object LobbySocketMember {

  def apply(channel: JsChannel, user: Option[User], blocking: Set[String], sri: Sri, mobile: Boolean): LobbyDirectSocketMember =
    LobbyDirectSocketMember(
      channel = channel,
      user = user map { LobbyUser.make(_, blocking) },
      sri = sri,
      mobile = mobile
    )

  def apply(bus: lila.common.Bus, user: Option[User], blocking: Set[String], sri: Sri, mobile: Boolean): LobbyRemoteSocketMember =
    LobbyRemoteSocketMember(
      bus = bus,
      user = user map { LobbyUser.make(_, blocking) },
      sri = sri,
      mobile = mobile
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
private[lobby] case class Join(sri: Sri, user: Option[User], blocking: Set[String], mobile: Boolean, promise: Promise[Connected])
private[lobby] case class JoinRemote(member: LobbyRemoteSocketMember)
private[lobby] case object Resync
private[lobby] case class HookIds(ids: Vector[String])

private[lobby] case class SetIdle(sri: Sri, value: Boolean)

private[lobby] case class HookSub(member: LobbySocketMember, value: Boolean)
private[lobby] case class AllHooksFor(member: LobbySocketMember, hooks: Vector[Hook])

private[lobby] case class GetSrisP(promise: Promise[Sris])

case class AddHook(hook: Hook)
case class AddSeek(seek: Seek)
