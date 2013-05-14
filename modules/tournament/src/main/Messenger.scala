package lila.tournament

import lila.db.api._
import tube.tournamentTube
import lila.user.{ User, UserRepo, Room ⇒ UserRoom }

private[tournament] final class Messenger(
    getUsername: String ⇒ Fu[Option[String]],
    val netDomain: String) extends UserRoom {

  import Room._

  def init(tour: Created): Fu[List[Message]] = for {
    username ← getUsername(tour.data.createdBy) flatMap { 
      _.fold[Fu[String]](fufail("No username found"))(fuccess(_))
    }
    message ← systemMessage(tour, "%s creates the tournament" format username)
  } yield List(message)

  def userMessage(tournamentId: String, userId: String, text: String): Fu[Message] = for {
    userOption ← UserRepo byId userId
    tourExists ← $count.exists($select(tournamentId))
    message ← (for {
      _ ← Unit.validIf(tourExists, "No such tournament") 
      msg ← userMessage(userOption, text)
      (u, t) = msg
    } yield Message(u.some, t)).future
    _ ← RoomRepo.addMessage(tournamentId, message)
  } yield message

  def systemMessage(tour: Tournament, text: String): Fu[Message] =
    Message(none, text) |> { message ⇒
      RoomRepo.addMessage(tour.id, message) inject message
    }

  def getMessages(tournamentId: String): Fu[List[Room.Message]] =
    RoomRepo room tournamentId map (_.decodedMessages)
}
