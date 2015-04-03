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
    }
  }

  def start(simulId: String) = Open { implicit ctx =>
    AsHost(simulId) { simul =>
      env.api start simul.id
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

  private def newForm(me: lila.user.User) =
    env.forms.create(s"${me.username}'s simul")

  def form = Auth { implicit ctx =>
    me =>
      NoEngine {
        Ok(html.simul.form(newForm(me), env.forms)).fuccess
      }
  }

  def create = AuthBody { implicit ctx =>
    implicit me =>
      NoEngine {
        implicit val req = ctx.body
        newForm(me).bindFromRequest.fold(
          err => BadRequest(html.simul.form(err.pp, env.forms)).fuccess,
          setup => env.repo.create(setup, me) map { simul =>
            println(simul)
            Redirect(routes.Simul.show(simul.id))
          })
      }
  }

  def join(id: String, variant: String) = AuthBody { implicit ctx =>
    implicit me =>
      NoEngine {
        negotiate(
          html = fuccess {
            env.api.addApplicant(id, me, variant)
            Redirect(routes.Simul.show(id))
          },
          api = _ => fuccess {
            env.api.addApplicant(id, me, variant)
            Ok(Json.obj("ok" -> true))
          }
        )
      }
  }

  def withdraw(id: String) = Auth { implicit ctx =>
    me =>
      negotiate(
        html = fuccess {
          env.api.removeApplicant(id, me)
          Redirect(routes.Simul.show(id))
        },
        api = _ => fuccess {
          env.api.removeApplicant(id, me)
          Ok(Json.obj("ok" -> true))
        }
      )
  }

  def websocket(id: String, apiVersion: Int) = SocketOption[JsValue] { implicit ctx =>
    (getInt("version") |@| get("sri")).tupled ?? {
      case (version, uid) => env.socketHandler.join(id, version, uid, ctx.me)
    }
  }

  private def chatOf(sim: Sim)(implicit ctx: Context) =
    ctx.isAuth ?? {
      Env.chat.api.userChat find sim.id map (_.forUser(ctx.me).some)
    }

  private def AsHost(simulId: Sim.ID)(f: Sim => Result)(implicit ctx: Context): Fu[Result] =
    env.repo.find(simulId) flatMap {
      case None => notFound
      case Some(simul) if ctx.userId.exists(simul.hostId ==) => fuccess(f(simul))
      case _ => fuccess(Unauthorized)
    }
}
