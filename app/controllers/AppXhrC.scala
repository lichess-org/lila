package controllers

import lila.http._
import DataForm._

import play.api._
import mvc._

object AppXhrC extends LilaController {

  private val xhr = env.appXhr
  private val syncer = env.appSyncer

  def sync(gameId: String, color: String, version: Int, fullId: String) = Action {
    JsonIOk(syncer.sync(gameId, color, version, Some(fullId)))
  }

  def syncPublic(gameId: String, color: String, version: Int) = Action {
    JsonIOk(syncer.sync(gameId, color, version, None))
  }

  def move(fullId: String) = Action { implicit request ⇒
    ValidOk(moveForm.bindFromRequest.toValid flatMap { move ⇒
      xhr.play(fullId, move._1, move._2, move._3).unsafePerformIO
    })
  }

  def abort(fullId: String) = Action {
    ValidIORedir(xhr abort fullId, fullId)
  }

  def outoftime(fullId: String) = Action {
    ValidIORedir(xhr outoftime fullId, fullId)
  }

  def resign(fullId: String) = Action {
    ValidIORedir(xhr resign fullId, fullId)
  }

  def forceResign(fullId: String) = Action {
    ValidIORedir(xhr forceResign fullId, fullId)
  }

  def claimDraw(fullId: String) = Action {
    ValidIORedir(xhr claimDraw fullId, fullId)
  }

  def ping() = Action { implicit request ⇒
    JsonIOk(env.pinger.ping(
      username = get("username"),
      playerKey = get("player_key"),
      watcherKey = get("watcher"),
      getNbWatchers = get("get_nb_watchers"),
      hookId = get("hook_id")
    ))
  }

  def nbPlayers = Action { Ok(env.aliveMemo.count.toString) }
}
