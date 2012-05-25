package controllers

import lila._
import http.Context
import lobby.Hook
import views._

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.iteratee._
import scalaz.effects._

object Lobby extends LilaController {

  def preloader = env.preloader
  def hookRepo = env.lobby.hookRepo
  def fisherman = env.lobby.fisherman
  def joiner = env.setup.hookJoiner

  val home = Open { implicit ctx ⇒
    renderHome(none).fold(identity, Ok(_))
  }

  def handleNotFound(req: RequestHeader): Result =
    handleNotFound(reqToCtx(req))

  def handleNotFound(implicit ctx: Context): Result =
    renderHome(none).fold(identity, NotFound(_))

  private def renderHome(myHook: Option[Hook])(implicit ctx: Context) = preloader(
    auth = ctx.isAuth,
    chat = ctx.canSeeChat,
    myHook = myHook
  ).unsafePerformIO.bimap(
      url ⇒ Redirect(url),
      preload ⇒ html.lobby.home(toJson(preload), myHook)
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

  def hook(ownerId: String) = Open { implicit ctx ⇒
    hookRepo.ownedHook(ownerId).unsafePerformIO.fold(
      hook ⇒ renderHome(hook.some).fold(identity, Ok(_)),
      Redirect(routes.Lobby.home))
  }

  def join(hookId: String) = Open { implicit ctx ⇒
    IORedirect {
      val myHookId = get("cancel")
      joiner(hookId, myHookId)(ctx.me) map { result ⇒
        result.fold(
          _ ⇒ myHookId.fold(routes.Lobby.hook(_), routes.Lobby.home),
          pov ⇒ routes.Round.player(pov.fullId))
      }
    }
  }

  def cancel(ownerId: String) = Open { implicit ctx ⇒
    IORedirect {
      for {
        hook ← hookRepo ownedHook ownerId
        _ ← hook.fold(fisherman.delete, io())
      } yield routes.Lobby.home
    }
  }

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
