package controllers

import lila.http._
import DataForm._

import play.api._
import mvc._

import play.api.libs.concurrent.Akka
import play.api.Play.current

object LobbyXhrC extends LilaController {

  private val xhr = env.lobbyXhr
  private val syncer = env.lobbySyncer

  def syncWithHook(hookId: String) = sync(Some(hookId))

  def syncWithoutHook() = sync(None)

  private def sync(hookId: Option[String]) = Action { implicit request ⇒
    Async {
      Akka.future {
        syncer.sync(
          hookId,
          getIntOr("auth", 0) == 1,
          getIntOr("state", 0),
          getIntOr("messageId", -1),
          getIntOr("entryId", 0)
        )
      } map JsonIOk
    }
  }
}
