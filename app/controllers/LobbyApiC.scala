package controllers

import lila.system.model.Hook
import lila.http._
import DataForm._

import play.api._
import mvc._

object LobbyApiC extends LilaController {

  private val api = env.lobbyApi
  private val preloader = env.lobbyPreloader

  def preload = Action { implicit request ⇒
    JsonIOk(preloader(
      auth = getIntOr("auth", 0) == 1,
      chat = getIntOr("chat", 0) == 1,
      myHookId = get("hook") filter (""!=)
    ))
  }

  def join(gameId: String, color: String) = Action { implicit request ⇒
    FormValidIOk[LobbyJoinData](lobbyJoinForm)(join ⇒
      api.join(gameId, color, join._1, join._2)
    )
  }

  def create(hookOwnerId: String) = Action {
    IOk(api.create(hookOwnerId))
  }

  def alive(hookOwnerId: String) = Action {
    IOk(api.alive(hookOwnerId))
  }

  def message = Action {
    IOk(api.messageRefresh)
  }
}
