package lila.setup

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json.Json
import play.twirl.api.Html

import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.SendTo
import lila.hub.actorApi.setup._
import makeTimeout.short

private[setup] final class Challenger(
    roundHub: ActorSelection,
    renderer: ActorSelection) extends Actor {

  private val bus = context.system.lilaBus

  def receive = {

    case msg@RemindChallenge(gameId, from, to) =>
      renderer ? msg foreach {
        case html: Html => {
          val event = SendTo(to, Json.obj(
            "t" -> "challengeReminder",
            "d" -> Json.obj(
              "id" -> gameId,
              "html" -> html.toString
            )
          ))
          bus.publish(event, 'users)
        }
      }

    case msg@DeclineChallenge(gameId) => roundHub ! Tell(gameId, msg)
  }
}
