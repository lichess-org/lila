package controllers

import lila.http._
import DataForm._

import play.api._
import mvc._

object Internal extends LilaController {

  private val api = env.internalApi

  def talk(gameId: String) = Action { implicit request ⇒
    ValidOk(talkForm.bindFromRequest.value toValid "Invalid talk" map { talk ⇒
      api.talk(gameId, talk._1, talk._2).unsafePerformIO
    })
  }

  def updateVersion(gameId: String) = Action {
    IOk(api updateVersion gameId)
  }

  def endGame(gameId: String) = Action {
    IOk(api endGame gameId)
  }
}
