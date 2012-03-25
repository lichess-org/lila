package controllers

import lila.http._
import DataForm._

import play.api._
import mvc._

object AppXhrC extends LilaController {

  private val xhr = env.appXhr
  private val syncer = env.appSyncer

  def sync(gameId: String, color: String, version: Int, fullId: String) = Action {
    JsonOk(syncer.sync(gameId, color, version, Some(fullId)).unsafePerformIO)
  }

  def syncPublic(gameId: String, color: String, version: Int) = Action {
    JsonOk(syncer.sync(gameId, color, version, None).unsafePerformIO)
  }

  def move(fullId: String) = Action { implicit request ⇒
    ValidOk(moveForm.bindFromRequest.toValid flatMap { move ⇒
      xhr.play(fullId, move._1, move._2, move._3).unsafePerformIO
    })
  }

  def ping() = Action { implicit request =>
    JsonOk(env.pinger.ping(
      username = get("username"),
      playerKey = get("player_key"),
      watcherKey = get("watcher"),
      getNbWatchers = get("get_nb_watchers"),
      hookId = get("hook_id")
    ).unsafePerformIO)
  }

  def nbPlayers() = Action {
    Ok(env.aliveMemo.count.toString)
  }
}
