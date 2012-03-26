package controllers

import lila.http._
import DataForm._

import play.api._
import mvc._

object AppApiC extends LilaController {

  private val api = env.appApi

  def talk(gameId: String) = Action { implicit request ⇒
    ValidIOk[TalkData](talkForm)(talk ⇒ api.talk(gameId, talk._1, talk._2))
  }

  def updateVersion(gameId: String) = Action {
    IOk(api updateVersion gameId)
  }

  def reloadTable(gameId: String) = Action {
    IOk(api reloadTable gameId)
  }

  def alive(gameId: String, color: String) = Action {
    IOk(api.alive(gameId, color))
  }

  def draw(gameId: String, color: String) = Action { implicit request ⇒
    ValidIOk[String](drawForm)(msgs ⇒ api.draw(gameId, color, msgs))
  }

  def drawAccept(gameId: String, color: String) = Action { implicit request ⇒
    ValidIOk[String](drawForm)(msgs ⇒ api.drawAccept(gameId, color, msgs))
  }

  def end(gameId: String) = Action { implicit request ⇒
    ValidIOk[String](endForm)(msgs ⇒ api.end(gameId, msgs))
  }

  def start(gameId: String) = Action { implicit request =>
    ValidIOk[EntryData](entryForm)(entryData ⇒ api.start(gameId, entryData))
  }

  def join(fullId: String) = Action { implicit request ⇒
    ValidIOk[JoinData](joinForm) { join ⇒
      api.join(fullId, join._1, join._2, join._3)
    }
  }

  def activity(gameId: String, color: String) = Action {
    Ok(api.activity(gameId, color).toString)
  }

  def possibleMoves(gameId: String, color: String) = Action {
    JsonIOk(api.possibleMoves(gameId, color))
  }

  def rematchAccept(gameId: String, color: String, newGameId: String) = Action { implicit request ⇒
    ValidIOk[RematchData](rematchForm)(r ⇒
      api.rematchAccept(gameId, newGameId, color, r._1, r._2, r._3))
  }
}
