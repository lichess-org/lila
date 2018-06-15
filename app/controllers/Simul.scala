package controllers

import play.api.libs.json._
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.simul.{ Simul => Sim }
import lila.chat.Chat
import views._

object Simul extends LilaController {

  private def env = Env.simul

  private def simulNotFound(implicit ctx: Context) = NotFound(html.simul.notFound())

  val home = Open { implicit ctx =>
    pageHit
    fetchSimuls map {
      case ((created, started), finished) =>
        Ok(html.simul.home(created, started, finished))
    }
  }

  val homeReload = Open { implicit ctx =>
    fetchSimuls map {
      case ((created, started), finished) =>
        Ok(html.simul.homeInner(created, started, finished))
    }
  }

  private def fetchSimuls =
    env.allCreated.get zip env.repo.allStarted zip env.repo.allFinished(30)

  def show(id: String) = Open { implicit ctx =>
    env.repo find id flatMap {
      _.fold(simulNotFound.fuccess) { sim =>
        for {
          version <- env.version(sim.id)
          json <- env.jsonView(sim)
          chat <- canHaveChat ?? Env.chat.api.userChat.cached.findMine(Chat.Id(sim.id), ctx.me).map(some)
          _ <- chat ?? { c => Env.user.lightUserApi.preloadMany(c.chat.userIds) }
          stream <- Env.streamer.liveStreamApi one sim.hostId
        } yield html.simul.show(sim, version, json, chat, stream)
      }
    } map NoCache
  }

  private[controllers] def canHaveChat(implicit ctx: Context): Boolean = ctx.me ?? { u =>
    if (ctx.kid) false
    else Env.chat.panic allowed u
  }

  def start(simulId: String) = Open { implicit ctx =>
    AsHost(simulId) { simul =>
      env.api start simul.id
      Ok(Json.obj("ok" -> true)) as JSON
    }
  }

  def abort(simulId: String) = Open { implicit ctx =>
    AsHost(simulId) { simul =>
      env.api abort simul.id
      Ok(Json.obj("ok" -> true)) as JSON
    }
  }

  def accept(simulId: String, userId: String) = Open { implicit ctx =>
    AsHost(simulId) { simul =>
      env.api.accept(simul.id, userId, true)
      Ok(Json.obj("ok" -> true)) as JSON
    }
  }

  def reject(simulId: String, userId: String) = Open { implicit ctx =>
    AsHost(simulId) { simul =>
      env.api.accept(simul.id, userId, false)
      Ok(Json.obj("ok" -> true)) as JSON
    }
  }

  def form = Auth { implicit ctx => me =>
    NoLameOrBot {
      Ok(html.simul.form(env.forms.create, env.forms)).fuccess
    }
  }

  def create = AuthBody { implicit ctx => implicit me =>
    NoLameOrBot {
      implicit val req = ctx.body
      env.forms.create.bindFromRequest.fold(
        err => BadRequest(html.simul.form(err, env.forms)).fuccess,
        setup => env.api.create(setup, me) map { simul =>
          Redirect(routes.Simul.show(simul.id))
        }
      )
    }
  }

  def join(id: String, variant: String) = Auth { implicit ctx => implicit me =>
    NoLameOrBot {
      fuccess {
        env.api.addApplicant(id, me, variant)
        if (HTTPRequest isXhr ctx.req) Ok(Json.obj("ok" -> true)) as JSON
        else Redirect(routes.Simul.show(id))
      }
    }
  }

  def withdraw(id: String) = Auth { implicit ctx => me =>
    fuccess {
      env.api.removeApplicant(id, me)
      if (HTTPRequest isXhr ctx.req) Ok(Json.obj("ok" -> true)) as JSON
      else Redirect(routes.Simul.show(id))
    }
  }

  def websocket(id: String, apiVersion: Int) = SocketOption[JsValue] { implicit ctx =>
    getSocketUid("sri") ?? { uid =>
      env.socketHandler.join(id, uid, ctx.me)
    }
  }

  private def AsHost(simulId: Sim.ID)(f: Sim => Result)(implicit ctx: Context): Fu[Result] =
    env.repo.find(simulId) flatMap {
      case None => notFound
      case Some(simul) if ctx.userId.exists(simul.hostId ==) => fuccess(f(simul))
      case _ => fuccess(Unauthorized)
    }
}
