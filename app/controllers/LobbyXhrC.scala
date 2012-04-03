package controllers

import lila.http._
import DataForm._

import play.api._
import mvc._

import play.api.libs.concurrent.Akka
import play.api.Play.current

import play.api.libs.json._
import play.api.libs.iteratee._

object LobbyXhrC extends LilaController {

  private val xhr = env.lobbyXhr
  private val syncer = env.lobbySyncer

  def socket(username: String) = WebSocket.async[JsValue] { request ⇒
    Lobby.join(username)
  }

  def cancel(ownerId: String) = Action {
    xhr.cancel(ownerId).unsafePerformIO
    Redirect("/")
  }

  def test = Action {
    Ok.stream(
      Enumerator("kiki", "foo", "bar").andThen(Enumerator.eof)
    )
  }

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
