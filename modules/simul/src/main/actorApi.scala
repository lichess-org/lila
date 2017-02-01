package lila.simul
package actorApi

import lila.game.Game
import lila.socket.Socket.Uid
import lila.socket.SocketMember
import lila.user.User

private[simul] case class Member(
  channel: JsChannel,
  userId: Option[String],
  troll: Boolean) extends SocketMember

private[simul] object Member {
  def apply(channel: JsChannel, user: Option[User]): Member = Member(
    channel = channel,
    userId = user map (_.id),
    troll = user.??(_.troll))
}

private[simul] case class Messadata(trollish: Boolean = false)

private[simul] case class Join(uid: Uid, user: Option[User])
private[simul] case class Talk(tourId: String, u: String, t: String, troll: Boolean)
private[simul] case class StartGame(game: Game, hostId: String)
private[simul] case class StartSimul(firstGame: Game, hostId: String)
private[simul] case class HostIsOn(gameId: String)
private[simul] case object Reload
private[simul] case object Aborted
private[simul] case class Connected(enumerator: JsEnumerator, member: Member)

private[simul] case object NotifyCrowd

case class SimulTable(simuls: List[Simul])
