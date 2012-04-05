package controllers

import lila.http._
import DataForm._

import play.api._
import mvc._

import play.api.libs.concurrent.Akka
import play.api.Play.current

import scalaz.effects.IO

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
      } map {
        _.unsafePerformIO.fold(JsonOk, NotFound)
      }
    }
  }

  def move(fullId: String) = Action { implicit request ⇒
    Async {
      Akka.future {
        moveForm.bindFromRequest.toValid flatMap { move ⇒
          xhr.play(fullId, move._1, move._2, move._3).unsafePerformIO
        }
      } map ValidOk
    }
  }

  def outoftime(fullId: String) = Action { ValidIOk(xhr outoftime fullId) }

  def abort(fullId: String) = validAndRedirect(fullId, xhr.abort)

  def resign(fullId: String) = validAndRedirect(fullId, xhr.resign)

  def forceResign(fullId: String) = validAndRedirect(fullId, xhr.forceResign)

  def drawClaim(fullId: String) = validAndRedirect(fullId, xhr.drawClaim)

  def drawAccept(fullId: String) = validAndRedirect(fullId, xhr.drawAccept)

  def drawOffer(fullId: String) = validAndRedirect(fullId, xhr.drawOffer)

  def drawCancel(fullId: String) = validAndRedirect(fullId, xhr.drawCancel)

  def drawDecline(fullId: String) = validAndRedirect(fullId, xhr.drawDecline)

  def validAndRedirect(fullId: String, f: String ⇒ IO[Valid[Unit]]) = Action {
    ValidIORedir(f(fullId), fullId)
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
      time ⇒ Ok(time)
    )
  }

  def ping = Action { implicit request ⇒
    Ok(env.pinger.ping(
      username = get("username"),
      playerKey = get("player_key"),
      watcherKey = get("watcher"),
      getNbWatchers = get("get_nb_watchers"),
      hookId = get("hook_id")
    ).unsafePerformIO) as JSON
  }

  def nbPlayers = Action { Ok(env.aliveMemo.count) }

  def nbGames = Action { Ok(env.gameRepo.countPlaying.unsafePerformIO) }
}
