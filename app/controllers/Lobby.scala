package controllers

import lila._
import http.Context
import views._
import DataForm._

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.iteratee._

object Lobby extends LilaController {

  private val api = env.lobbyApi
  private val preloader = env.lobbyPreloader

  val home = Open { implicit ctx ⇒
    preloader(
      auth = ctx.isAuth,
      chat = ctx.canSeeChat,
      myHookId = get("hook")
    ).unsafePerformIO.fold(
        url ⇒ Redirect(url),
        preload ⇒ Ok(html.lobby.home(toJson(preload)))
      )
  }

  def socket = WebSocket.async[JsValue] { implicit req ⇒
    implicit val ctx = Context(req, None)
    env.lobbySocket.join(
      uidOption = get("uid"),
      username = get("username"),
      versionOption = getInt("version"),
      hook = get("hook")
    )
  }

  def cancel(ownerId: String) = Action {
    api.cancel(ownerId).unsafePerformIO
    Redirect("/")
  }

  def join(hookId: String) = TODO

  //def join(gameId: String, color: String) = Action { implicit req ⇒
  //FormValidIOk[LobbyJoinData](lobbyJoinForm)(join ⇒
  //api.join(gameId, color, join._1, join._2, join._3, join._4)
  //)
  //}

  def create(hookOwnerId: String) = Action {
    IOk(api create hookOwnerId)
  }

  def chatBan(username: String) = Action {
    IOk(env.lobbyMessenger ban username)
  }
}
