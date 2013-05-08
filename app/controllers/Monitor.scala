package controllers

import play.api.mvc._
import play.api.libs.json._
import akka.pattern.ask

import lila.app._
import lila.monitor.actorApi._
import lila.socket.actorApi.GetNbMembers
import makeTimeout.short

object Monitor extends LilaController {

  private def env = Env.monitor

  def index = Action {
    Ok(views.html.monitor.monitor())
  }

  def websocket = WebSocket.async[JsValue] { implicit req ⇒
    get("sri", req) zmap env.socketHandler.join
  }

  def status = Open { implicit ctx ⇒
    (~get("key") match {
      case "elo" ⇒
        lila.user.UserRepo.idsAverageElo(Env.user.usernameMemo.keys) zip
          lila.game.GameRepo.recentAverageElo(5) map {
            case (users, (rated, casual)) ⇒ List(users, rated, casual) mkString " "
          }
      case "moves"   ⇒ (env.reporting ? GetNbMoves).mapTo[Int]
      case "players" ⇒ (env.reporting ? GetNbMembers).mapTo[Int] map { "%d %d".format(_, Env.user.usernameMemo.count) }
      case _         ⇒ (env.reporting ? GetStatus).mapTo[String]
    }) map { x ⇒ Ok(x.toString) }
  }
}
