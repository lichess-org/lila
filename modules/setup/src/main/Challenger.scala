package lila.setup

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json.Json
import play.api.templates.Html

import lila.hub.actorApi.SendTos
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.setup._
import makeTimeout.short

private[setup] final class Challenger(
    hub: ActorSelection,
    roundHub: ActorSelection,
    renderer: ActorSelection) extends Actor {

  def receive = {

    case msg @ RemindChallenge(gameId, from, to) ⇒
      renderer ? msg map {
        case html: Html ⇒ SendTos(Set(to), Json.obj(
          "t" -> "challengeReminder",
          "d" -> Json.obj(
            "id" -> gameId,
            "html" -> html.toString
          )
        ))
      } pipeToSelection hub

    case msg @ DeclineChallenge(gameId) ⇒ roundHub ! Tell(gameId, msg)
  }
}
