package lila
package tournament

import akka.actor._
import akka.actor.ReceiveTimeout
import akka.util.duration._
import akka.util.Timeout
import akka.pattern.{ ask, pipe }
import akka.dispatch.{ Future, Promise }
import play.api.libs.concurrent._
import play.api.Play.current
import play.api.libs.json._

import socket.SendTos

private[tournament] final class Reminder(hubNames: List[String]) extends Actor {

  lazy val hubRefs = hubNames map { name ⇒ Akka.system.actorFor("/user/" + name) }

  implicit val timeout = Timeout(1 second)

  def receive = {
    case tour: Started ⇒ {
      val msg = SendTos(tour.userIds.toSet, JsObject(Seq(
        "t" -> JsString("tournamentReminder"),
        "d" -> JsString(views.html.tournament.reminder(tour).toString)
      )))
      hubRefs foreach { _ ! msg }
    }
  }
}
