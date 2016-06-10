package controllers

import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.game.{ Pov, GameRepo }
import lila.simul.{ Simul => Sim }
import lila.user.UserRepo
import views._

object Simul extends LilaController {

  private def env = Env.simul

  private def simulNotFound(implicit ctx: Context) = NotFound(html.simul.notFound())

  val home = Open { implicit ctx =>
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
    env.allCreated(true) zip env.repo.allStarted zip env.repo.allFinished(10)

  def show(id: String) = Open { implicit ctx =>
    env.repo find id flatMap {
      _.fold(simulNotFound.fuccess) { sim =>
        env.version(sim.id) zip
          env.jsonView(sim) zip
          chatOf(sim) map {
            case ((version, data), chat) => html.simul.show(sim, version, data, chat)
          }
      }
    } map NoCache
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

  def form = Auth { implicit ctx =>
    me =>
      NoEngine {
        Ok(html.simul.form(env.forms.create, env.forms)).fuccess
      }
  }

  def create = AuthBody { implicit ctx =>
    implicit me =>
      NoEngine {
        implicit val req = ctx.body
        env.forms.create.bindFromRequest.fold(
          err => BadRequest(html.simul.form(err, env.forms)).fuccess,
          setup => env.api.create(setup, me) map { simul =>
            Redirect(routes.Simul.show(simul.id))
          })
      }
  }

  def join(id: String, variant: String) = Auth { implicit ctx =>
    implicit me =>
      NoEngine {
        fuccess {
          env.api.addApplicant(id, me, variant)
          if (HTTPRequest isXhr ctx.req) Ok(Json.obj("ok" -> true)) as JSON
          else Redirect(routes.Simul.show(id))
        }
      }
  }

  def withdraw(id: String) = Auth { implicit ctx =>
    me =>
      fuccess {
        env.api.removeApplicant(id, me)
        if (HTTPRequest isXhr ctx.req) Ok(Json.obj("ok" -> true)) as JSON
        else Redirect(routes.Simul.show(id))
      }
  }

  def websocket(id: String, apiVersion: Int) = SocketOption[JsValue] { implicit ctx =>
    get("sri") ?? { uid =>
      env.socketHandler.join(id, uid, ctx.me)
    }
  }

  private def chatOf(sim: Sim)(implicit ctx: Context): Fu[Option[lila.chat.UserChat.Mine]] =
    ctx.me ?? { me =>
      Env.chat.api.userChat.findMine(sim.id, me) map (_.some)
    }

  private def AsHost(simulId: Sim.ID)(f: Sim => Result)(implicit ctx: Context): Fu[Result] =
    env.repo.find(simulId) flatMap {
      case None => notFound
      case Some(simul) if ctx.userId.exists(simul.hostId ==) => fuccess(f(simul))
      case _ => fuccess(Unauthorized)
    }
}
