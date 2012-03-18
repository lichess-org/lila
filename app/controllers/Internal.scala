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

  def end(gameId: String) = Action { implicit request ⇒
    ValidIOk[String](endForm)(msgs ⇒ api.end(gameId, msgs))
  }

  def join(fullId: String) = Action { implicit request ⇒
    ValidIOk[JoinData](joinForm)(join ⇒ api.join(fullId, join._1, join._2))
  }

  def acceptRematch(gameId: String) = Action { implicit request ⇒
    ValidIOk[RematchData](rematchForm)(rematch ⇒
      api.acceptRematch(gameId, rematch._1, rematch._2))
  }
}
