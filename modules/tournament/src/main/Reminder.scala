package lila.tournament

import actorApi._
import lila.hub.actorApi.SendTos
import makeTimeout.short

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json.Json
import play.api.templates.Html

private[tournament] final class Reminder(
    sockets: List[ActorRef],
    renderer: ActorRef) extends Actor {

  def receive = {

    case RemindTournaments(tours) ⇒ tours foreach { tour ⇒
      renderer ? RemindTournament(tour) foreach {
        case html: Html ⇒ {
          val msg = SendTos(tour.activeUserIds.toSet, Json.obj(
            "t" -> "tournamentReminder",
            "d" -> Json.obj(
              "id" -> tour.id,
              "html" -> html.toString
            )))
          sockets foreach { _ ! msg }
        }
      }
    }
  }
}
