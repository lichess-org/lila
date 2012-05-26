package controllers

import lila._
import http.Context
import lobby.Hook
import views._

import play.api.mvc._
import play.api.libs.json.JsValue
import scalaz.effects._

object Lobby extends LilaController {

  def preloader = env.preloader
  def hookRepo = env.lobby.hookRepo
  def fisherman = env.lobby.fisherman
  def joiner = env.setup.hookJoiner
  def forumRecent = env.forum.recent

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
      preload ⇒ html.lobby.home(toJson(preload), myHook, forumRecent(ctx.me))
    )

  def socket = WebSocket.async[JsValue] { implicit req ⇒
    implicit val ctx = reqToCtx(req)
    env.lobby.socket.join(
      uidOption = get("uid"),
      username = ctx.me map (_.username),
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
}
