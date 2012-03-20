package controllers

import lila.http._
import DataForm._

import play.api._
import mvc._

object LobbyApiC extends LilaController {

  private val api = env.lobbyApi

  def join(gameId: String, color: String) = Action {
    IOk(api.join(gameId, color))
  }
}
