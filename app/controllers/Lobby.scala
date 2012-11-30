package controllers

import lila._
import http.Context
import lobby.Hook
import views._

import play.api.mvc._
import play.api.libs.json.JsValue
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import scala.concurrent.{ Future, Promise }
import scalaz.effects._

object Lobby extends LilaController with Results {

  private def preloader = env.lobby.preloader
  private def hookRepo = env.lobby.hookRepo
  private def fisherman = env.lobby.fisherman
  private def joiner = env.setup.hookJoiner
  private def forumRecent = env.forum.recent
  private def timelineRecent = env.timeline.entryRepo.recent
  private def messageRepo = env.lobby.messageRepo
  private def featured = env.game.featured
  private def openTours = env.tournament.repo.created

  val home = Open { implicit ctx ⇒ Async { renderHome(none, Ok) } }

  def handleNotFound(req: RequestHeader): Result = handleNotFound(reqToCtx(req))

  def handleNotFound(implicit ctx: Context): Result = Async { renderHome(none, NotFound) }

  private def renderHome[A](myHook: Option[Hook], status: Status)(implicit ctx: Context): Future[Result] =
    preloader(
      auth = ctx.isAuth,
      chat = ctx.canSeeChat,
      myHook = myHook,
      timeline = timelineRecent,
      posts = forumRecent(ctx.me),
      tours = openTours
    ).map(_.fold(Redirect(_), {
        case (preload, posts, tours, featured) ⇒ status(html.lobby.home(
          Json stringify preload,
          myHook,
          posts,
          tours,
          featured))
      }))

  def socket = WebSocket.async[JsValue] { implicit req ⇒
    implicit val ctx = reqToCtx(req)
    env.lobby.socket.join(
      uidOption = get("sri"),
      username = ctx.me map (_.username),
      versionOption = getInt("version"),
      hook = get("hook")
    )
  }

  def hook(ownerId: String) = Open { implicit ctx ⇒
    Async {
      hookRepo.ownedHook(ownerId).unsafePerformIO.fold(
        Future successful { Redirect(routes.Lobby.home): Result }
      ) { hook ⇒ renderHome(hook.some, Ok) }
    }
  }

  def join(hookId: String) = Open { implicit ctx ⇒
    IORedirect {
      val myHookId = get("cancel")
      joiner(hookId, myHookId)(ctx.me) map { result ⇒
        result.fold(
          _ ⇒ myHookId.fold(routes.Lobby.home)(routes.Lobby.hook(_)),
          pov ⇒ routes.Round.player(pov.fullId))
      }
    }
  }

  def cancel(ownerId: String) = Open { implicit ctx ⇒
    IORedirect {
      for {
        hook ← hookRepo ownedHook ownerId
        _ ← hook.fold(io())(fisherman.delete)
      } yield routes.Lobby.home
    }
  }

  val log = Open { implicit ctx ⇒
    IOk(messageRepo.all map { html.lobby.log(_) })
  }
}
