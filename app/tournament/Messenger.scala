package lila
package tournament

import scalaz.effects._

import user.{ User, UserRepo }

final class Messenger(
    roomRepo: RoomRepo,
    userRepo: UserRepo) extends core.Room {

  import Room._

  def init(tour: Created): IO[List[Message]] =
    userRepo byId tour.data.createdBy flatMap {
      _.fold(
        user ⇒ systemMessage(tour, "%s creates the tournament" format user) map { List(_) },
        io(Nil)
      )
    }

  def userMessage(tour: Tournament, text: String, username: String): IO[Valid[Message]] = for {
    userOption ← userRepo byId username
    message = for {
      user ← userOption filter (_.canChat) toValid "This user cannot chat"
      msg ← createMessage(user, text)
      (author, text) = msg
    } yield Message(author.some, text)
    _ ← message.fold(_ ⇒ io(), msg ⇒ roomRepo.addMessage(tour.id, msg))
  } yield message

  def systemMessage(tour: Tournament, text: String): IO[Message] =
    Message(none, text) |> { message =>
      roomRepo.addMessage(tour.id, message) map (_ => message)
    }
}
