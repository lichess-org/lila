package lila.setup

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json.Json
import play.api.templates.Html

import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.SendTo
import lila.hub.actorApi.setup._
import makeTimeout.short

private[setup] final class Challenger(
    bus: akka.event.EventStream,
    roundHub: ActorSelection,
    renderer: ActorSelection) extends Actor {

  def receive = {

    case msg@RemindChallenge(gameId, from, to) ⇒
      renderer ? msg foreach {
        case html: Html ⇒ bus publish {
          SendTo(to, Json.obj(
            "t" -> "challengeReminder",
            "d" -> Json.obj(
              "id" -> gameId,
              "html" -> html.toString
            )
          ))
        }
      }

    case msg@DeclineChallenge(gameId) ⇒ roundHub ! Tell(gameId, msg)
  }
}
