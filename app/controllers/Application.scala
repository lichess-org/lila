package controllers

import lila.http._
import DataForm._

import play.api._
import mvc._

object Application extends LilaController {

  def sync(gameId: String, color: String, version: Int, fullId: Option[String]) =
    Action {
      JsonOk(env.syncer.sync(gameId, color, version, fullId).unsafePerformIO)
    }

  def move(fullId: String) = Action { implicit request ⇒
    ValidOk(moveForm.bindFromRequest.toValid flatMap { move ⇒
      env.server.play(fullId, move._1, move._2, move._3).unsafePerformIO
    })
  }

  def ping() = Action { implicit request =>
    JsonOk(env.pinger.ping(
      get("username"),
      get("player_key"),
      get("watcher"),
      get("get_nb_watchers")
    ).unsafePerformIO)
  }
}
