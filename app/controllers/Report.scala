package controllers

import lila._

import play.api.mvc._
import play.api.libs.concurrent._
import scalaz.effects._
import akka.pattern.ask
import akka.util.duration._
import akka.util.{ Duration, Timeout }
import socket.GetNbMembers
import report.{ GetStatus, GetNbPlaying }

object Report extends LilaController {

  val reporting = env.site.reporting
  implicit val timeout = Timeout(100 millis)

  def status = Action {
    Async {
      (reporting ? GetStatus).mapTo[String].asPromise map { Ok(_) }
    }
  }

  def nbPlayers = Action {
    Async {
      (reporting ? GetNbMembers).mapTo[Int].asPromise map { Ok(_) }
    }
  }

  def nbPlaying = Action {
    Async {
      (reporting ? GetNbPlaying).mapTo[Int].asPromise map { Ok(_) }
    }
  }
}
