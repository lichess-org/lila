package lila
package game

import play.api.libs.concurrent._
import play.api.Play.current
import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.duration._
import akka.util.{ Duration, Timeout }
import akka.dispatch.{ Future }
import akka.event.Logging
import scalaz.effects._
import socket._

final class HubMaster(hubMemo: HubMemo) extends Actor {

  private implicit val timeout = Timeout(200 millis)
  private implicit val executor = Akka.system.dispatcher
  private val log = Logging(context.system, this)

  def receive = {

    case KeepAlive ⇒ hubMemo.all foreach {
      case (id, hub) ⇒ hub ! WithMembers(
        _.nonEmpty.fold(hubMemo shake id, io())
      )
    }

    case GetNbMembers ⇒ Future.traverse(hubActors)(a ⇒
      (a ? GetNbMembers).mapTo[Int]
    ) map (_.sum) pipeTo sender

    case msg @ NbPlayers(nb) ⇒ hubActors foreach (_ ! msg)

    case msg                 ⇒ log.info("HubMaster unknown message: " + msg)
  }

  def hubActors = hubMemo.all.values

  private def hubs = hubMemo.all
}
