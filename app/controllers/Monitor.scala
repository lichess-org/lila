package controllers

import play.api.mvc._
import play.api.libs.Comet
import play.api.libs.concurrent._
import play.api.libs.json._
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout

import lila._
import socket.GetNbMembers
import monitor._

object Monitor extends LilaController {

  def reporting = env.monitor.reporting
  def usernameMemo = env.user.usernameMemo
  implicit val timeout = Timeout(500 millis)

  val index = Action {
    Ok(views.html.monitor.monitor())
  }

  val websocket = WebSocket.async[JsValue] { implicit req ⇒
    env.monitor.socket.join(uidOption = get("uid", req))
  }

  val status = Action {
    Async {
      (reporting ? GetStatus).mapTo[String].asPromise map { Ok(_) }
    }
  }

  val nbPlayers = Action {
    Async {
      (reporting ? GetNbMembers).mapTo[Int].asPromise map { players ⇒
        Ok("%d %d".format(players, usernameMemo.preciseCount))
      }
    }
  }

  val nbMoves = Action {
    Async {
      (reporting ? GetNbMoves).mapTo[Int].asPromise map { Ok(_) }
    }
  }
}
