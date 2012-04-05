package lila
package controllers

import lila.DataForm._

import play.api._
import mvc._

object AppApiC extends LilaController {

  private val api = env.appApi

  def show(fullId: String) = Action {
    JsonIOk(api show fullId)
  }

  def reloadTable(gameId: String) = Action {
    IOk(api reloadTable gameId)
  }

  def alive(gameId: String, color: String) = Action {
    IOk(api.alive(gameId, color))
  }

  def start(gameId: String) = Action { implicit request =>
    FormValidIOk[EntryData](entryForm)(entryData ⇒ api.start(gameId, entryData))
  }

  def join(fullId: String) = Action { implicit request ⇒
    FormValidIOk[JoinData](joinForm) { join ⇒
      api.join(fullId, join._1, join._2, join._3)
    }
  }

  def activity(gameId: String, color: String) = Action {
    Ok(api.activity(gameId, color))
  }

  def playerVersion(gameId: String, color: String) = Action {
    Ok(api.playerVersion(gameId, color).unsafePerformIO)
  }

  def rematchAccept(gameId: String, color: String, newGameId: String) = Action { implicit request ⇒
    FormValidIOk[RematchData](rematchForm)(r ⇒
      api.rematchAccept(gameId, newGameId, color, r._1, r._2, r._3, r._4))
  }
}
