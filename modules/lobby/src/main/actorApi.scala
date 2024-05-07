package lila.lobby

import lila.core.socket.{ Sri, Sris }

private case class SaveSeek(msg: AddSeek)
private case class RemoveHook(hookId: String)
private case class RemoveSeek(seekId: String)
private case class RemoveHooks(hooks: Set[Hook])
private case class CancelHook(sri: Sri)
private case class CancelSeek(seekId: String, user: LobbyUser)
private case class BiteHook(hookId: String, sri: Sri, user: Option[LobbyUser])
private case class BiteSeek(seekId: String, user: LobbyUser)
private case class JoinHook(sri: Sri, hook: Hook, game: Game, creatorColor: Color)
private case class JoinSeek(userId: UserId, seek: Seek, game: Game, creatorColor: Color)
private case class HookSub(member: LobbySocket.Member, value: Boolean)
private case class AllHooksFor(member: LobbySocket.Member, hooks: Seq[Hook])
private case class LeaveBatch(sris: Iterable[Sri])
private case object LeaveAll
private case object Resync
private case class HookIds(ids: Iterable[String])

private case class GetSrisP(promise: Promise[Sris])

case class AddHook(hook: Hook)
case class AddSeek(seek: Seek)
