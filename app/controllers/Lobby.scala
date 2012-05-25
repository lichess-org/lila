package controllers

import lila._
import http.Context
import views._

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.iteratee._

object Lobby extends LilaController {

  //private val api = env.lobby.api
  private val preloader = env.preloader

  val home = Open { implicit ctx ⇒
    renderHome(ctx).fold(identity, Ok(_))
  }

  def handleNotFound(req: RequestHeader): Result =
    handleNotFound(reqToCtx(req))

  def handleNotFound(ctx: Context): Result =
    renderHome(ctx).fold(identity, NotFound(_))

  private def renderHome(implicit ctx: Context) = preloader(
    auth = ctx.isAuth,
    chat = ctx.canSeeChat,
    myHookId = get("hook")
  ).unsafePerformIO.bimap(
      url ⇒ Redirect(url),
      preload ⇒ html.lobby.home(toJson(preload))
    )

  def socket = WebSocket.async[JsValue] { implicit req ⇒
    implicit val ctx = Context(req, None)
    env.lobby.socket.join(
      uidOption = get("uid"),
      username = get("username"),
      versionOption = getInt("version"),
      hook = get("hook")
    )
  }

  def cancel(ownerId: String) = TODO
    //api.cancel(ownerId).unsafePerformIO
    //Redirect("/")
  //}

  def join(hookId: String) = TODO

  //def join(gameId: String, color: String) = Action { implicit req ⇒
  //FormValidIOk[LobbyJoinData](lobbyJoinForm)(join ⇒
  //api.join(gameId, color, join._1, join._2, join._3, join._4)
  //)
  //}

  //def create(hookOwnerId: String) = Action {
    //IOk(api create hookOwnerId)
  //}

  //def chatBan(username: String) = Action {
    //IOk(env.lobby.messenger ban username)
  //}
}
