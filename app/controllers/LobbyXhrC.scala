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

  def socket(uid: String) = WebSocket.async[JsValue] { request ⇒
    Lobby.join(uid)
  }

  def cancel(ownerId: String) = Action {
    xhr.cancel(ownerId).unsafePerformIO
    Redirect("/")
  }
}
