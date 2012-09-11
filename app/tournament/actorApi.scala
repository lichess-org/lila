package lila
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
    muted = user.fold(_.muted, false))
}

case class Join(
  uid: String,
  user: Option[User],
  version: Int)
case class Connected(
  enumerator: JsEnumerator,
  member: Member)
case class Talk(u: String, txt: String)
case class GetTournamentVersion(tournamentId: String)
case class CloseTournament(tournamentId: String)
case class GetHub(tournamentId: String)
case class Forward(tournamentId: String, msg: Any)
case object ReloadUserList
case object HubTimeout
case object GetNbHubs
case class StartGame(game: DbGame)

// organizer
case object StartTournaments
case class StartTournament(tour: Created)
case object StartPairings
case class StartPairing(tour: Started)
case class GetTournamentUsernames(tournamentId: String)
