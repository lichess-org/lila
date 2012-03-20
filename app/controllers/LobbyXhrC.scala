package controllers

import lila.http._
import DataForm._

import play.api._
import mvc._

object LobbyXhrC extends LilaController {

  private val xhr = env.lobbyXhr

  def sync() = Action {
    Ok("")
  }
}
