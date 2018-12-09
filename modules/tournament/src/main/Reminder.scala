package lila.tournament

import akka.actor._
import play.api.libs.json.Json

import actorApi._
import lila.hub.actorApi.socket.SendTos
import lila.socket.Socket.makeMessage

private[tournament] final class Reminder extends Actor {

  private val max = 50

  def receive = {

    case RemindTournament(tour, activeUserIds) if !tour.pairingsClosed =>
      val userIds = {
        if (activeUserIds.size > max) scala.util.Random.shuffle(activeUserIds) take max
        else activeUserIds
      }.toSet
      if (userIds.nonEmpty) context.system.lilaBus.publish(SendTos(userIds, makeMessage(
        "tournamentReminder",
        Json.obj(
          "id" -> tour.id,
          "name" -> tour.fullName
        )
      )), 'socketUsers)
  }
}
