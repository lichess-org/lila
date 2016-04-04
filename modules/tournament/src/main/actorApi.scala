package lila.tournament
package actorApi

import akka.actor.ActorRef

import lila.game.Game
import lila.socket.SocketMember
import lila.user.User

private[tournament] case class Member(
  out: ActorRef,
  userId: Option[String],
  troll: Boolean) extends SocketMember

private[tournament] object Member {
  def apply(out: ActorRef, user: Option[User]): Member = Member(
    out = out,
    userId = user map (_.id),
    troll = user.??(_.troll))
}

private[tournament] case class Messadata(trollish: Boolean = false)

private[tournament] case class AddMember(uid: String, member: Member)
private[tournament] case class Talk(tourId: String, u: String, t: String, troll: Boolean)
private[tournament] case object Reload
private[tournament] case class StartGame(game: Game)

case class RemindTournament(tour: Tournament, activeUserIds: List[String])
case class TournamentTable(tours: List[Tournament])

private[tournament] case object ScheduleNow
private[tournament] case object NotifyCrowd
private[tournament] case object NotifyReload

private[tournament] case object GetWaitingUsers

private[tournament] case class SetTournament(tour: Option[Tournament])
