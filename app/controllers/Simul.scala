package controllers

import play.api.mvc.*
import views.*

import lila.api.Context
import lila.app.{ given, * }
import lila.chat.Chat
import lila.common.HTTPRequest
import lila.simul.{ Simul as Sim }

final class Simul(env: Env) extends LilaController(env):

  private def forms = lila.simul.SimulForm

  private def simulNotFound(implicit ctx: Context) = NotFound(html.simul.bits.notFound())

  def home     = Open(serveHome(_))
  def homeLang = LangPage(routes.Simul.home)(serveHome(_))
  private def serveHome(implicit ctx: Context) = NoBot {
    pageHit
    fetchSimuls(ctx.me) flatMap { case (((pending, created), started), finished) =>
      Ok(html.simul.home(pending, created, started, finished)).toFuccess
    }
  }

  val apiList = Action.async {
    fetchSimuls(none) flatMap { case (((pending, created), started), finished) =>
      env.simul.jsonView.apiAll(pending, created, started, finished) map JsonOk
    }
  }

  val homeReload = Open { implicit ctx =>
    fetchSimuls(ctx.me) map { case (((pending, created), started), finished) =>
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

  def show(id: SimulId) =
    Open { implicit ctx =>
      env.simul.repo find id flatMap {
        _.fold[Fu[Result]](simulNotFound.toFuccess) { sim =>
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
            chat <-
              canHaveChat(sim) ?? env.chat.api.userChat.cached.findMine(sim.id into ChatId, ctx.me).map(some)
            _ <- chat ?? { c =>
              env.user.lightUserApi.preloadMany(c.chat.userIds)
            }
            stream <- env.streamer.liveStreamApi one sim.hostId
          } yield html.simul.show(sim, version, json, chat, stream, team)
        }
      } dmap (_.noCache)
    }

  private[controllers] def canHaveChat(simul: Sim)(implicit ctx: Context): Boolean =
    ctx.noKid && ctx.noBot &&                     // no public chats for kids or bots
      ctx.me.fold(HTTPRequest.isHuman(ctx.req)) { // anon can see public chats
        env.chat.panic.allowed
      } && simul.team.fold(true) { teamId =>
        ctx.userId exists {
          env.team.api.syncBelongsTo(teamId, _) || isGranted(_.ChatTimeout)
        }
      }

  def hostPing(simulId: SimulId) =
    Open { implicit ctx =>
      AsHost(simulId) { simul =>
        env.simul.api hostPing simul inject jsonOkResult
      }
    }

  def start(simulId: SimulId) =
    Open { implicit ctx =>
      AsHost(simulId) { simul =>
        env.simul.api start simul.id inject jsonOkResult
      }
    }

  def abort(simulId: SimulId) =
    Auth { implicit ctx => me =>
      AsHost(simulId) { simul =>
        env.simul.api abort simul.id inject {
          if (!simul.isHost(me)) env.mod.logApi.terminateTournament(me.id into ModId, simul.fullName)
          if (HTTPRequest isXhr ctx.req) jsonOkResult
          else Redirect(routes.Simul.home)
        }
      }
    }

  def accept(simulId: SimulId, userId: UserStr) =
    Open { implicit ctx =>
      AsHost(simulId) { simul =>
        env.simul.api.accept(simul.id, userId.id, v = true) inject jsonOkResult
      }
    }

  def reject(simulId: SimulId, userId: UserStr) =
    Open { implicit ctx =>
      AsHost(simulId) { simul =>
        env.simul.api.accept(simul.id, userId.id, v = false) inject jsonOkResult
      }
    }

  def setText(simulId: SimulId) =
    OpenBody { implicit ctx =>
      AsHost(simulId) { simul =>
        given play.api.mvc.Request[?] = ctx.body
        forms.setText
          .bindFromRequest()
          .fold(
            _ => BadRequest.toFuccess,
            text => env.simul.api.setText(simul.id, text) inject jsonOkResult
          )
      }
    }

  def form =
    Auth { implicit ctx => me =>
      NoLameOrBot {
        env.team.api.lightsByLeader(me.id) map { teams =>
          Ok(html.simul.form.create(forms.create(me, teams), teams))
        }
      }
    }

  def create =
    AuthBody { implicit ctx => implicit me =>
      NoLameOrBot {
        given play.api.mvc.Request[?] = ctx.body
        env.team.api.lightsByLeader(me.id) flatMap { teams =>
          forms
            .create(me, teams)
            .bindFromRequest()
            .fold(
              err =>
                env.team.api.lightsByLeader(me.id) map { teams =>
                  BadRequest(html.simul.form.create(err, teams))
                },
              setup =>
                env.simul.api.create(setup, me) map { simul =>
                  Redirect(routes.Simul.show(simul.id))
                }
            )
        }
      }
    }

  def join(id: SimulId, variant: chess.variant.Variant.LilaKey) =
    Auth { implicit ctx => implicit me =>
      NoLameOrBot {
        env.team.cached.teamIds(me.id) flatMap { teamIds =>
          env.simul.api.addApplicant(id, me, teamIds.contains, variant) inject {
            if (HTTPRequest isXhr ctx.req) jsonOkResult
            else Redirect(routes.Simul.show(id))
          }
        }
      }
    }

  def withdraw(id: SimulId) =
    Auth { implicit ctx => me =>
      env.simul.api.removeApplicant(id, me) inject {
        if (HTTPRequest isXhr ctx.req) jsonOkResult
        else Redirect(routes.Simul.show(id))
      }
    }

  def edit(id: SimulId) =
    Auth { implicit ctx => me =>
      WithEditableSimul(id, me) { simul =>
        env.team.api.lightsByLeader(me.id) map { teams =>
          Ok(html.simul.form.edit(forms.edit(me, teams, simul), teams, simul))
        }
      }
    }

  def update(id: SimulId) =
    AuthBody { implicit ctx => me =>
      WithEditableSimul(id, me) { simul =>
        given play.api.mvc.Request[?] = ctx.body
        env.team.api.lightsByLeader(me.id) flatMap { teams =>
          forms
            .edit(me, teams, simul)
            .bindFromRequest()
            .fold(
              err => BadRequest(html.simul.form.edit(err, teams, simul)).toFuccess,
              data => env.simul.api.update(simul, data, me) inject Redirect(routes.Simul.show(id))
            )
        }
      }
    }

  private def AsHost(simulId: SimulId)(f: Sim => Fu[Result])(implicit ctx: Context): Fu[Result] =
    env.simul.repo.find(simulId) flatMap {
      case None                                                                    => notFound
      case Some(simul) if ctx.userId.has(simul.hostId) || isGranted(_.ManageSimul) => f(simul)
      case _                                                                       => fuccess(Unauthorized)
    }

  private def WithEditableSimul(id: SimulId, me: lila.user.User)(
      f: Sim => Fu[Result]
  )(implicit ctx: Context): Fu[Result] =
    AsHost(id) { sim =>
      if (sim.isStarted) Redirect(routes.Simul.show(sim.id)).toFuccess
      else f(sim)
    }
