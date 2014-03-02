package controllers

import akka.pattern.ask
import play.api.libs.json._
import play.api.mvc._

import lila.app._
import lila.socket.actorApi.PopulationGet
import lila.monitor.actorApi._
import makeTimeout.short

object Monitor extends LilaController {

  private def env = Env.monitor

  def index = Action {
    Ok(views.html.monitor.monitor(Env.ai.nbStockfishRemotes))
  }

  def websocket = Socket[JsValue] { implicit ctx =>
    get("sri") ?? env.socketHandler.apply
  }

  def status = Action.async { implicit req =>
    (~get("key", req) match {
      case "rating" => {
        lila.user.UserRepo.idsAverageRating(Env.user.onlineUserIdMemo.keys) zip
          lila.game.GameRepo.recentAverageRating(5) map {
            case (users, (rated, casual)) => List(users, rated, casual) mkString " "
          }
      } map { Ok(_) }
      case "moves" => (env.reporting ? GetNbMoves).mapTo[Int] map { Ok(_) }
      case "players" => {
        (env.reporting ? PopulationGet).mapTo[Int] map { "%d %d".format(_, Env.user.onlineUserIdMemo.count) }
      } map { Ok(_) }
      case key => fuccess {
        BadRequest(s"Unknown monitor status key: $key")
      }
    })
  }
}
