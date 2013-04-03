package lila.app

import lila.hub.actorApi.router._

import controllers.routes
import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.concurrent.Execution.Implicits._

private[app] final class Router extends Actor {

  import makeTimeout.large

  def receive = {

    case Abs(route) ⇒ self ? route mapTo manifest[String] map { route ⇒
      "huhu" + route
    } pipeTo sender

    case TeamShow(id) ⇒ sender ! routes.Team.show(id)
  }
}
