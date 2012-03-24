package controllers

import lila.system.model.{ Hook, EntryGame }
import lila.http._
import DataForm._

import play.api._
import mvc._

object LobbyApiC extends LilaController {

  private val api = env.lobbyApi

  def join(gameId: String, color: String) = Action { implicit request ⇒
    ValidIOk[EntryGame](entryGameForm)(ec ⇒ api.join(gameId, color, ec))
  }

  def create(hookOwnerId: String) = Action {
    IOk(api.create(hookOwnerId))
  }

  def remove(hookId: String) = Action {
    IOk(api.remove(hookId))
  }

  def alive(hookOwnerId: String) = Action {
    IOk(api.alive(hookOwnerId))
  }
}
