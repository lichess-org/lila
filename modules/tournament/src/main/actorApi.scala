package lila.tournament
package actorApi

import lila.game.Game
import lila.socket.SocketMember
import lila.user.User

private[tournament] case class Member(
  channel: JsChannel,
  userId: Option[String],
  troll: Boolean) extends SocketMember

private[tournament] object Member {
  def apply(channel: JsChannel, user: Option[User]): Member = Member(
    channel = channel,
    userId = user map (_.id),
    troll = user.??(_.troll))
}

private[tournament] case class Messadata(trollish: Boolean = false)

private[tournament] case class Join(
  uid: String,
  user: Option[User],
  version: Int)
private[tournament] case class Talk(tourId: String, u: String, t: String, troll: Boolean)
private[tournament] case object Reload
private[tournament] case class StartGame(game: Game)
private[tournament] case class Joining(userId: String)
private[tournament] case class Connected(enumerator: JsEnumerator, member: Member)

// organizer
private[tournament] case object AllCreatedTournaments
private[tournament] case object StartedTournaments
case class RemindTournament(tour: Tournament, activeUserIds: List[String])
case class TournamentTable(tours: List[Tournament])

private[tournament] case object ScheduleNow
private[tournament] case object NotifyCrowd
private[tournament] case object NotifyReload

private[tournament] case object GetWaitingUsers

private[tournament] case class SetTournament(tour: Option[Tournament])
