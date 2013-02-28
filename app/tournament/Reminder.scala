package lila.app
package tournament

import akka.actor._
import akka.actor.ReceiveTimeout
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.{ ask, pipe }
import scala.concurrent.{ Future, Promise }
import play.api.libs.concurrent._
import play.api.Play.current
import play.api.libs.json._

import socket.SendTos

private[tournament] final class Reminder(hubNames: List[String]) extends Actor {

  lazy val hubRefs = hubNames map { name ⇒ Akka.system.actorFor("/user/" + name) }

  implicit val timeout = Timeout(1 second)

  def receive = {

    case RemindTournaments(tours) ⇒ tours foreach { tour =>
      val msg = SendTos(tour.activeUserIds.toSet, JsObject(Seq(
        "t" -> JsString("tournamentReminder"),
        "d" -> JsObject(Seq(
          "id" -> JsString(tour.id),
          "html" -> JsString(views.html.tournament.reminder(tour).toString)
        ))
      )))
      hubRefs foreach { _ ! msg }
    }
  }
}
