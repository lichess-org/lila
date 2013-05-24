package lila.tournament

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json.Json
import play.api.templates.Html

import actorApi._
import lila.hub.actorApi.SendTos
import lila.hub.Broadcast
import makeTimeout.short

private[tournament] final class Reminder(
    hub: lila.hub.ActorLazyRef,
    renderer: lila.hub.ActorLazyRef) extends Actor {

  def receive = {

    case RemindTournaments(tours) ⇒ tours foreach { tour ⇒
      renderer ? RemindTournament(tour) foreach {
        case html: Html ⇒ hub ! SendTos(tour.activeUserIds.toSet, Json.obj(
          "t" -> "tournamentReminder",
          "d" -> Json.obj(
            "id" -> tour.id,
            "html" -> html.toString
          )))
      }
    }
  }
}
