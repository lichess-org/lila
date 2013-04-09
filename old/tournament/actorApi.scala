package lila.app
package tournament

import socket.SocketMember
import user.User
import game.DbGame

import akka.actor.ActorRef
import scalaz.effects.IO

case class Member(
    channel: JsChannel,
    username: Option[String],
    muted: Boolean) extends SocketMember {

  def canChat = !muted
}

object Member {
  def apply(channel: JsChannel, user: Option[User]): Member = Member(
    channel = channel,
    username = user map (_.username),
    muted = ~user.map(_.muted))
}

case class Join(
  uid: String,
  user: Option[User],
  version: Int)
case class Talk(u: String, txt: String)
case class GetTournamentVersion(tournamentId: String)
case class CloseTournament(tournamentId: String)
case object GetTournamentIds
case class GetHub(tournamentId: String)
case class Forward(tournamentId: String, msg: Any)
case object Start
case object Reload
case object ReloadPage
case object HubTimeout
case object GetNbHubs
case class StartGame(game: DbGame)
case class Joining(userId: String)

// organizer
case object CreatedTournaments
case class CreatedTournament(tour: Created)
case object StartedTournaments
case class StartedTournament(tour: Started)
case object StartPairings
case class StartPairing(tour: Started)
case class GetTournamentUsernames(tournamentId: String)
case class RemindTournaments(tours: List[Started])
