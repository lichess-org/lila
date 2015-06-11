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

    case msg@RemindTournament(tour, activeUserIds) =>
      renderer ? msg foreach {
        case html: Html => bus.publish(SendTos(activeUserIds, Json.obj(
          "t" -> "tournamentReminder",
          "d" -> Json.obj(
            "id" -> tour.id,
            "html" -> html.toString
          ))), 'users)
      }
  }
}
