package controllers

import lila.http._
import DataForm._

import play.api._
import mvc._

object Internal extends LilaController {

  private val api = env.internalApi

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

  def draw(gameId: String, color: String) = Action { implicit request =>
    ValidIOk[String](drawForm)(msgs ⇒ api.draw(gameId, color, msgs))
  }

  def drawAccept(gameId: String, color: String) = Action { implicit request =>
    ValidIOk[String](drawForm)(msgs ⇒ api.drawAccept(gameId, color, msgs))
  }

  def end(gameId: String) = Action { implicit request ⇒
    ValidIOk[String](endForm)(msgs ⇒ api.end(gameId, msgs))
  }

  def join(fullId: String) = Action { implicit request ⇒
    ValidIOk[JoinData](joinForm)(join ⇒ api.join(fullId, join._1, join._2))
  }

  def activity(gameId: String, color: String) = Action {
    Ok(api.activity(gameId, color).toString)
  }

  def acceptRematch(gameId: String, color: String, newGameId: String) = Action { implicit request ⇒
    ValidIOk[RematchData](rematchForm)(rematch ⇒
      api.acceptRematch(gameId, newGameId, color, rematch._1, rematch._2))
  }
}
