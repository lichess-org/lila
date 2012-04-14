package lila
package game

import play.api.libs.concurrent._
import play.api.Play.current
import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.duration._
import akka.util.{ Duration, Timeout }
import akka.dispatch.{ Await, Future }
import scalaz.effects._
import socket._

final class HubMaster(hubMemo: HubMemo) extends Actor {

  implicit val timeout = Timeout(200 millis)
  //implicit val context = Akka.system.dispatcher

  def receive = {

    case Cleanup ⇒ {
      hubMemo.all foreach {
        case (id, hub) ⇒ hub ! WithMembers(
          _.nonEmpty.fold(hubMemo shake id, io())
        )
      }
    }

    case GetNbMembers ⇒ {
      implicit val executor = Akka.system.dispatcher
      val futures = hubActors map { actor ⇒ (actor ? GetNbMembers).mapTo[Int] }
      val futureList = Future.sequence(futures)
      val futureNb = futureList map (_.sum)
      futureNb pipeTo sender
    }

    case NbPlayers(nb) ⇒ hubActors foreach { actor ⇒
      actor ! NbPlayers(nb)
    }
  }

  private def hubs = hubMemo.all

  private def hubActors = hubMemo.all.values
}
