package lila.tournament
package actorApi

import lila.game.Game
import lila.socket.SocketMember
import lila.user.User

case class Member(
  channel: JsChannel,
  userId: Option[String],
  troll: Boolean) extends SocketMember

object Member {
  def apply(channel: JsChannel, user: Option[User]): Member = Member(
    channel = channel,
    userId = user map (_.id),
    troll = user.??(_.troll))
}

case class Join(
  uid: String,
  user: Option[User],
  version: Int)
case class Talk(tourId: String, u: String, t: String, troll: Boolean)
case object Start
case object Reload
case object ReloadPage
case object HubTimeout
case class StartGame(game: Game)
case class Joining(userId: String)
case class Connected(enumerator: JsEnumerator, member: Member)

// organizer
private[tournament] case object AllCreatedTournaments
private[tournament] case object StartedTournaments
private[tournament] case object CheckLeaders
case class RemindTournaments(tours: List[Started])
case class RemindTournament(tour: Started)
case class TournamentTable(tours: List[Created])

private[tournament] case object ScheduleNow
