package lila
package tournament

import scalaz.effects._

import user.User

final class Messenger(
    roomRepo: RoomRepo,
    getTournament: String ⇒ IO[Option[Tournament]],
    getUser: String ⇒ IO[Option[User]],
    val netDomain: String) extends core.Room {

  import Room._

  def init(tour: Created): IO[List[Message]] = for {
    userOption ← getUser(tour.data.createdBy)
    username = userOption.fold(tour.data.createdBy)(_.username)
    message ← systemMessage(tour, "%s creates the tournament" format username)
  } yield List(message)

  def userMessage(tournamentId: String, username: String, text: String): IO[Valid[Message]] = for {
    userOption ← getUser(username)
    tourOption ← getTournament(tournamentId)
    message = for {
      user ← userOption filter (_.canChat) toValid "This user cannot chat"
      _ ← tourOption toValid "No such tournament"
      msg ← createMessage(user, text)
      (author, text) = msg
    } yield Message(author.some, text)
    _ ← message.fold(_ ⇒ io(), msg ⇒ roomRepo.addMessage(tournamentId, msg))
  } yield message

  def systemMessage(tour: Tournament, text: String): IO[Message] =
    Message(none, text) |> { message ⇒
      roomRepo.addMessage(tour.id, message) map (_ ⇒ message)
    }

  def render(tour: Tournament): IO[String] = render(tour.id) 

  def render(roomId: String): IO[String] = roomRepo room roomId map (_.render)
}
