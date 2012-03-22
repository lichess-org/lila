package controllers

import lila.http._
import lila.system.model.Hook
import DataForm._

import play.api._
import mvc._

object LobbyApiC extends LilaController {

  private val api = env.lobbyApi

  def join(gameId: String, color: String) = Action {
    IOk(api.join(gameId, color))
  }

  def inc = Action {
    IOk(api.inc)
  }

  def create = Action { implicit request =>
    ValidIOk[Hook](hookForm)(hook ⇒ api.create(hook))
  }
}
