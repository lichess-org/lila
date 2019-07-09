package controllers

import play.api.libs.json._
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.chat.Chat
import lila.common.HTTPRequest
import lila.hub.lightTeam._
import lila.simul.{ Simul => Sim }
import views._

object Simul extends LilaController {

  private def env = Env.simul
  private def forms = lila.simul.SimulForm

  private def simulNotFound(implicit ctx: Context) = NotFound(html.simul.bits.notFound())

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
          team <- sim.team ?? Env.team.api.team
          version <- env.version(sim.id)
          json <- env.jsonView(sim, team.map { t =>
            lila.simul.SimulTeam(t.id, t.name, ctx.userId exists {
              Env.team.api.syncBelongsTo(t.id, _)
            })
          })
          chat <- canHaveChat ?? Env.chat.api.userChat.cached.findMine(Chat.Id(sim.id), ctx.me).map(some)
          _ <- chat ?? { c => Env.user.lightUserApi.preloadMany(c.chat.userIds) }
          stream <- Env.streamer.liveStreamApi one sim.hostId
        } yield html.simul.show(sim, version, json, chat, stream, team)
      }
    } map NoCache
  }

  private[controllers] def canHaveChat(implicit ctx: Context): Boolean =
    !ctx.kid && // no public chats for kids
      ctx.me.fold(true) { // anon can see public chats
        Env.chat.panic.allowed
      }

  def start(simulId: String) = Open { implicit ctx =>
    AsHost(simulId) { simul =>
      env.api start simul.id
      jsonOkResult
    }
  }

  def abort(simulId: String) = Open { implicit ctx =>
    AsHost(simulId) { simul =>
      env.api abort simul.id
      jsonOkResult
    }
  }

  def accept(simulId: String, userId: String) = Open { implicit ctx =>
    AsHost(simulId) { simul =>
      env.api.accept(simul.id, userId, true)
      jsonOkResult
    }
  }

  def reject(simulId: String, userId: String) = Open { implicit ctx =>
    AsHost(simulId) { simul =>
      env.api.accept(simul.id, userId, false)
      jsonOkResult
    }
  }

  def setText(simulId: String) = OpenBody { implicit ctx =>
    AsHost(simulId) { simul =>
      implicit val req = ctx.body
      forms.setText.bindFromRequest.fold(
        err => BadRequest,
        text => {
          env.api.setText(simul.id, text)
          jsonOkResult
        }
      )
    }
  }

  def form = Auth { implicit ctx => me =>
    NoLameOrBot {
      teamsIBelongTo(me) map { teams =>
        Ok(html.simul.form(forms.create, teams))
      }
    }
  }

  def create = AuthBody { implicit ctx => implicit me =>
    NoLameOrBot {
      implicit val req = ctx.body
      forms.create.bindFromRequest.fold(
        err => teamsIBelongTo(me) map { teams =>
          BadRequest(html.simul.form(err, teams))
        },
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
      env.socketHandler.join(id, uid, ctx.me, getSocketVersion, apiVersion)
    }
  }

  private def AsHost(simulId: Sim.ID)(f: Sim => Result)(implicit ctx: Context): Fu[Result] =
    env.repo.find(simulId) flatMap {
      case None => notFound
      case Some(simul) if ctx.userId.exists(simul.hostId ==) => fuccess(f(simul))
      case _ => fuccess(Unauthorized)
    }

  private def teamsIBelongTo(me: lila.user.User): Fu[TeamIdsWithNames] =
    Env.team.api.mine(me) map { _.map(t => t._id -> t.name) }
}
