package lila.setup

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json.Json
import play.api.templates.Html

import lila.hub.actorApi.SendTos
import makeTimeout.short

private[setup] final class Challenger(
    hub: lila.hub.ActorLazyRef,
    renderer: lila.hub.ActorLazyRef) extends Actor {

  def receive = {

    case msg @ lila.hub.actorApi.setup.RemindChallenge(gameId, from, to) ⇒
      renderer ? msg map {
        case html: Html ⇒ SendTos(Set(to), Json.obj(
          "t" -> "challengeReminder",
          "d" -> Json.obj(
            "id" -> gameId,
            "html" -> html.toString
          )))
      } pipeTo hub.ref
  }
}
