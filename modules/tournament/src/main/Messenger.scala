package lila.tournament

import lila.db.api._
import tube.tournamentTube
import lila.user.{ User, UserRepo, Room ⇒ UserRoom }

import org.apache.commons.lang3.StringEscapeUtils.escapeXml

private[tournament] final class Messenger(
    getUsername: String ⇒ Fu[Option[String]],
    val netDomain: String) extends UserRoom {

  import Room._

  def init(tour: Created): Fu[List[Message]] = for {
    username ← getUsername(tour.data.createdBy) flatMap { 
      _.fold[Fu[String]](fufail("No username found"))(fuccess(_))
    }
    message ← system(tour, "%s creates the tournament" format username)
  } yield List(message)

  def apply(tournamentId: String, userId: String, text: String): Fu[Message] = for {
    userOption ← UserRepo byId userId
    tourExists ← $count.exists($select(tournamentId))
    message ← (for {
      _ ← ().validIf(tourExists, "No such tournament") 
      msg ← userMessage(userOption, text)
      (u, t) = msg
    } yield Message(u.some, escapeXml(t))).future
    _ ← RoomRepo.addMessage(tournamentId, message)
  } yield message

  def system(tour: Tournament, text: String): Fu[Message] =
    Message(none, text) |> { message ⇒
      RoomRepo.addMessage(tour.id, message) inject message
    }

  def getMessages(tournamentId: String): Fu[List[Room.Message]] =
    RoomRepo room tournamentId map (_.decodedMessages)
}
