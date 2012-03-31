package controllers

import lila.http._
import DataForm._

import play.api._
import mvc._

import play.api.libs.concurrent.Akka
import play.api.Play.current

object AppXhrC extends LilaController {

  private val xhr = env.appXhr
  private val syncer = env.appSyncer

  def sync(gameId: String, color: String, version: Int, fullId: String) =
    syncAll(gameId, color, version, Some(fullId))

  def syncPublic(gameId: String, color: String, version: Int) =
    syncAll(gameId, color, version, None)

  private def syncAll(
    gameId: String,
    color: String,
    version: Int,
    fullId: Option[String]) = Action {
    Async {
      Akka.future {
        syncer.sync(gameId, color, version, fullId)
      } map JsonIOk
    }
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

  def drawAccept(fullId: String) = Action { implicit request ⇒
    ValidIORedir(xhr drawAccept fullId, fullId)
  }

  def talk(fullId: String) = Action { implicit request ⇒
    talkForm.bindFromRequest.fold(
      form ⇒ BadRequest(form.errors mkString "\n"),
      message ⇒ IOk(xhr.talk(fullId, message))
    )
  }

  def moretime(fullId: String) = Action {
    (xhr moretime fullId).unsafePerformIO.fold(
      e ⇒ BadRequest(e.list mkString "\n"),
      time ⇒ Ok(time.toString)
    )
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
