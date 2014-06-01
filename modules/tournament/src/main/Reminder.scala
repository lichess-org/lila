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

  def receive = {

    case RemindTournaments(tours) => tours foreach { tour =>
      renderer ? RemindTournament(tour) foreach {
        case html: Html => {
          val event = SendTos(tour.activeUserIds.toSet, Json.obj(
            "t" -> "tournamentReminder",
            "d" -> Json.obj(
              "id" -> tour.id,
              "html" -> html.toString
            )))
          bus.publish(event, 'users)
        }
      }
    }
  }
}
