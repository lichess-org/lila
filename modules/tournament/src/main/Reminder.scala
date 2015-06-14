package lila.tournament

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json.Json
import play.twirl.api.Html

import actorApi._
import lila.hub.actorApi.SendTos
import makeTimeout.short

private[tournament] final class Reminder(
    renderer: ActorSelection) extends Actor {

  private val bus = context.system.lilaBus

  private val max = 100

  def receive = {

    case msg@RemindTournament(tour, activeUserIds) =>
      renderer ? msg foreach {
        case html: Html =>
          val userIds =
            if (activeUserIds.size > max) scala.util.Random.shuffle(activeUserIds) take max
            else activeUserIds
          bus.publish(SendTos(userIds.toSet, Json.obj(
            "t" -> "tournamentReminder",
            "d" -> Json.obj(
              "id" -> tour.id,
              "html" -> html.toString
            ))), 'users)
      }
  }
}
