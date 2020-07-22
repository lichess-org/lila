package controllers

import play.api.libs.json._
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.chat.Chat
import lila.common.HTTPRequest
import lila.simul.{ Simul => Sim }
import views._

final class Simul(
    env: Env,
    apiC: => Team
) extends LilaController(env) {

  private def forms = lila.simul.SimulForm

  private def simulNotFound(implicit ctx: Context) = NotFound(html.simul.bits.notFound())

  val home = Open { implicit ctx =>
    pageHit
    fetchSimuls(ctx.me) flatMap {
      case pending ~ created ~ started ~ finished =>
        Ok(html.simul.home(pending, created, started, finished)).fuccess
    }
  }

  val apiList = Action.async {
    fetchSimuls(none) flatMap {
      case pending ~ created ~ started ~ finished =>
        env.simul.jsonView.apiAll(pending, created, started, finished) map { json =>
          Ok(json) as JSON
        }
    }
  }

  val homeReload = Open { implicit ctx =>
    fetchSimuls(ctx.me) map {
      case pending ~ created ~ started ~ finished =>
        Ok(html.simul.homeInner(pending, created, started, finished))
    }
  }

  private def fetchSimuls(me: Option[lila.user.User]) =
    me.?? { u =>
      env.simul.repo.findPending(u.id)
    } zip
      env.simul.allCreatedFeaturable.get {} zip
      env.simul.repo.allStarted zip
      env.simul.repo.allFinishedFeaturable(20)

  def show(id: String) =
    Open { implicit ctx =>
      env.simul.repo find id flatMap {
        _.fold(simulNotFound.fuccess) { sim =>
          for {
            team    <- sim.team ?? env.team.api.team
            version <- env.simul.version(sim.id)
            json <- env.simul.jsonView(
              sim,
              team.map { t =>
                lila.simul.SimulTeam(
                  t.id,
                  t.name,
                  ctx.userId exists {
                    env.team.api.syncBelongsTo(t.id, _)
                  }
                )
              }
            )
            chat <- canHaveChat ?? env.chat.api.userChat.cached.findMine(Chat.Id(sim.id), ctx.me).map(some)
            _ <- chat ?? { c =>
              env.user.lightUserApi.preloadMany(c.chat.userIds)
            }
            stream <- env.streamer.liveStreamApi one sim.hostId
          } yield html.simul.show(sim, version, json, chat, stream, team)
        }
      } map NoCache
    }

  private[controllers] def canHaveChat(implicit ctx: Context): Boolean =
    !ctx.kid &&           // no public chats for kids
      ctx.me.fold(true) { // anon can see public chats
        env.chat.panic.allowed
      }

  def hostPing(simulId: String) =
    Open { implicit ctx =>
      AsHost(simulId) { simul =>
        env.simul.repo setHostSeenNow simul inject jsonOkResult
      }
    }

  def start(simulId: String) =
    Open { implicit ctx =>
      AsHost(simulId) { simul =>
        env.simul.api start simul.id inject jsonOkResult
      }
    }

  def abort(simulId: String) =
    Open { implicit ctx =>
      AsHost(simulId) { simul =>
        env.simul.api abort simul.id inject jsonOkResult
      }
    }

  def accept(simulId: String, userId: String) =
    Open { implicit ctx =>
      AsHost(simulId) { simul =>
        env.simul.api.accept(simul.id, userId, true) inject jsonOkResult
      }
    }

  def reject(simulId: String, userId: String) =
    Open { implicit ctx =>
      AsHost(simulId) { simul =>
        env.simul.api.accept(simul.id, userId, false) inject jsonOkResult
      }
    }

  def setText(simulId: String) =
    OpenBody { implicit ctx =>
      AsHost(simulId) { simul =>
        implicit val req = ctx.body
        forms.setText
          .bindFromRequest()
          .fold(
            _ => BadRequest.fuccess,
            text => env.simul.api.setText(simul.id, text) inject jsonOkResult
          )
      }
    }

  def form =
    Auth { implicit ctx => me =>
      NoLameOrBot {
        apiC.teamsIBelongTo(me) map { teams =>
          Ok(html.simul.form(forms.create(me), teams))
        }
      }
    }

  def create =
    AuthBody { implicit ctx => implicit me =>
      NoLameOrBot {
        implicit val req = ctx.body
        forms
          .create(me)
          .bindFromRequest()
          .fold(
            err =>
              apiC.teamsIBelongTo(me) map { teams =>
                BadRequest(html.simul.form(err, teams))
              },
            setup =>
              env.simul.api.create(setup, me) map { simul =>
                Redirect(routes.Simul.show(simul.id))
              }
          )
      }
    }

  def join(id: String, variant: String) =
    Auth { implicit ctx => implicit me =>
      NoLameOrBot {
        env.simul.api.addApplicant(id, me, variant) inject {
          if (HTTPRequest isXhr ctx.req) Ok(Json.obj("ok" -> true)) as JSON
          else Redirect(routes.Simul.show(id))
        }
      }
    }

  def withdraw(id: String) =
    Auth { implicit ctx => me =>
      env.simul.api.removeApplicant(id, me) inject {
        if (HTTPRequest isXhr ctx.req) Ok(Json.obj("ok" -> true)) as JSON
        else Redirect(routes.Simul.show(id))
      }
    }

  private def AsHost(simulId: Sim.ID)(f: Sim => Fu[Result])(implicit ctx: Context): Fu[Result] =
    env.simul.repo.find(simulId) flatMap {
      case None                                              => notFound
      case Some(simul) if ctx.userId.exists(simul.hostId ==) => f(simul)
      case _                                                 => fuccess(Unauthorized)
    }
}
