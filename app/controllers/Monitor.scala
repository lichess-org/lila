package controllers

import play.api.mvc._
import play.api.libs.Comet
import play.api.libs.concurrent._
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout

import lila._
import socket.GetNbMembers
import monitor._

object Monitor extends LilaController {

  def reporting = env.monitor.reporting
  implicit val timeout = Timeout(100 millis)

  val index = Action {
    Ok(views.html.monitor.monitor(env.monitor.stream.maxMemory))
  }

  val stream = Action {
    Ok.stream(env.monitor.stream.getData &> Comet(callback = "parent.message"))
  }

  val status = Action {
    Async {
      (reporting ? GetStatus).mapTo[String].asPromise map { Ok(_) }
    }
  }

  val nbPlayers = Action {
    Async {
      (reporting ? GetNbMembers).mapTo[Int].asPromise map { Ok(_) }
    }
  }

  val nbPlaying = Action {
    Async {
      (reporting ? GetNbPlaying).mapTo[Int].asPromise map { Ok(_) }
    }
  }
}
