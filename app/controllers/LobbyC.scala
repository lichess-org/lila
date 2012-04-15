package lila
package controllers

import DataForm._

import play.api._
import mvc._

import play.api.libs.concurrent.Akka
import play.api.Play.current

import play.api.libs.json._
import play.api.libs.iteratee._

object LobbyC extends LilaController {

  private val api = env.lobbyApi
  private val preloader = env.lobbyPreloader

  def socket = WebSocket.async[JsValue] { implicit request ⇒
    env.lobbySocket.join(
      uid = get("uid") err "Socket UID missing",
      version = getInt("version") err "Socket version missing",
      hook = get("hook")
    )
  }

  def cancel(ownerId: String) = Action {
    api.cancel(ownerId).unsafePerformIO
    Redirect("/")
  }

  def preload = Action { implicit request ⇒
    JsonIOk(preloader(
      auth = getIntOr("auth", 0) == 1,
      chat = getIntOr("chat", 0) == 1,
      myHookId = get("hook")
    ))
  }

  def join(gameId: String, color: String) = Action { implicit request ⇒
    FormValidIOk[LobbyJoinData](lobbyJoinForm)(join ⇒
      api.join(gameId, color, join._1, join._2, join._3, join._4)
    )
  }

  def create(hookOwnerId: String) = Action {
    IOk(api.create(hookOwnerId))
  }
}
