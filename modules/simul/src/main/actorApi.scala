package lila.simul
package actorApi

import akka.actor.ActorRef

import lila.game.Game
import lila.socket.SocketMember
import lila.user.User

private[simul] case class Member(
  out: ActorRef,
  userId: Option[String],
  troll: Boolean) extends SocketMember

private[simul] object Member {
  def apply(out: ActorRef, user: Option[User]): Member = Member(
    out = out,
    userId = user map (_.id),
    troll = user.??(_.troll))
}
private[simul] case class AddMember(uid: String, member: Member)

private[simul] case class Messadata(trollish: Boolean = false)

private[simul] case class Talk(tourId: String, u: String, t: String, troll: Boolean)
private[simul] case class StartGame(game: Game, hostId: String)
private[simul] case class StartSimul(firstGame: Game, hostId: String)
private[simul] case class HostIsOn(gameId: String)
private[simul] case object Reload
private[simul] case object Aborted

private[simul] case object NotifyCrowd

case class SimulTable(simuls: List[Simul])
