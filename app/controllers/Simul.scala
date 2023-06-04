package controllers

import play.api.mvc.*
import views.*

import lila.api.Context
import lila.app.{ given, * }
import lila.common.HTTPRequest
import lila.simul.{ Simul as Sim }

final class Simul(env: Env) extends LilaController(env):

  private def forms = lila.simul.SimulForm

  private def simulNotFound(using Context) = NotFound(html.simul.bits.notFound())

  def home     = Open(serveHome)
  def homeLang = LangPage(routes.Simul.home)(serveHome)

  private def serveHome(using ctx: Context) = NoBot:
    pageHit
    fetchSimuls(ctx.me) flatMap { case (((pending, created), started), finished) =>
      Ok(html.simul.home(pending, created, started, finished)).toFuccess
    }

  val apiList = OpenOrScoped(): me =>
    fetchSimuls(me) flatMap { case (((pending, created), started), finished) =>
      env.simul.jsonView.apiAll(pending, created, started, finished) map JsonOk
    }

  val homeReload = Open:
    fetchSimuls(ctx.me) map { case (((pending, created), started), finished) =>
      Ok(html.simul.homeInner(pending, created, started, finished))
    }

  private def fetchSimuls(me: Option[lila.user.User]) =
    me.?? { u =>
      env.simul.repo.findPending(u.id)
    } zip
      env.simul.allCreatedFeaturable.get {} zip
      env.simul.repo.allStarted zip
      env.simul.repo.allFinishedFeaturable(20)

  def show(id: SimulId) = Open:
    env.simul.repo find id flatMap {
      _.fold[Fu[Result]](simulNotFound.toFuccess) { sim =>
        for
          verdicts <- env.simul.api.getVerdicts(sim, ctx.me)
          version  <- env.simul.version(sim.id)
          json     <- env.simul.jsonView(sim, verdicts)
          chat <-
            canHaveChat(sim) ?? env.chat.api.userChat.cached.findMine(sim.id into ChatId, ctx.me).map(some)
          _ <- chat ?? { c =>
            env.user.lightUserApi.preloadMany(c.chat.userIds)
          }
          stream <- env.streamer.liveStreamApi one sim.hostId
        yield html.simul.show(sim, version, json, chat, stream, verdicts)
      }
    } dmap (_.noCache)

  private[controllers] def canHaveChat(simul: Sim)(using ctx: Context): Boolean =
    ctx.noKid && ctx.noBot &&                     // no public chats for kids or bots
      ctx.me.fold(HTTPRequest.isHuman(ctx.req)) { // anon can see public chats
        env.chat.panic.allowed
      } && simul.conditions.teamMember.map(_.teamId).fold(true) { teamId =>
        ctx.userId exists {
          env.team.api.syncBelongsTo(teamId, _) || isGranted(_.ChatTimeout)
        }
      }

  def hostPing(simulId: SimulId) = Open:
    AsHost(simulId): simul =>
      env.simul.api hostPing simul inject jsonOkResult

  def start(simulId: SimulId) = Open:
    AsHost(simulId): simul =>
      env.simul.api start simul.id inject jsonOkResult

  def abort(simulId: SimulId) = Auth { ctx ?=> me =>
    AsHost(simulId) { simul =>
      env.simul.api abort simul.id inject {
        if (!simul.isHost(me)) env.mod.logApi.terminateTournament(me.id into ModId, simul.fullName)
        if (HTTPRequest isXhr ctx.req) jsonOkResult
        else Redirect(routes.Simul.home)
      }
    }
  }

  def accept(simulId: SimulId, userId: UserStr) = Open:
    AsHost(simulId): simul =>
      env.simul.api.accept(simul.id, userId.id, v = true) inject jsonOkResult

  def reject(simulId: SimulId, userId: UserStr) = Open:
    AsHost(simulId): simul =>
      env.simul.api.accept(simul.id, userId.id, v = false) inject jsonOkResult

  def setText(simulId: SimulId) = OpenBody:
    AsHost(simulId): simul =>
      forms.setText
        .bindFromRequest()
        .fold(
          _ => BadRequest.toFuccess,
          text => env.simul.api.setText(simul.id, text) inject jsonOkResult
        )

  def form = Auth { ctx ?=> me =>
    NoLameOrBot {
      env.team.api.lightsByLeader(me.id) map { teams =>
        Ok(html.simul.form.create(forms.create(me, teams), teams))
      }
    }
  }

  def create = AuthBody { ctx ?=> me =>
    NoLameOrBot:
      env.team.api
        .lightsByLeader(me.id)
        .flatMap: teams =>
          forms
            .create(me, teams)
            .bindFromRequest()
            .fold(
              err =>
                env.team.api.lightsByLeader(me.id) map { teams =>
                  BadRequest(html.simul.form.create(err, teams))
                },
              setup =>
                env.simul.api.create(setup, me, teams) map { simul =>
                  Redirect(routes.Simul.show(simul.id))
                }
            )
  }

  def join(id: SimulId, variant: chess.variant.Variant.LilaKey) = Auth { ctx ?=> me =>
    NoLameOrBot:
      env.simul.api
        .addApplicant(id, me, variant)
        .inject:
          if (HTTPRequest isXhr ctx.req)
          then jsonOkResult
          else Redirect(routes.Simul.show(id))
  }

  def withdraw(id: SimulId) = Auth { ctx ?=> me =>
    env.simul.api.removeApplicant(id, me) inject {
      if (HTTPRequest isXhr ctx.req) jsonOkResult
      else Redirect(routes.Simul.show(id))
    }
  }

  def edit(id: SimulId) = Auth { ctx ?=> me =>
    WithEditableSimul(id) { simul =>
      env.team.api.lightsByLeader(me.id) map { teams =>
        Ok(html.simul.form.edit(forms.edit(me, teams, simul), teams, simul))
      }
    }
  }

  def update(id: SimulId) = AuthBody { ctx ?=> me =>
    WithEditableSimul(id): simul =>
      env.team.api
        .lightsByLeader(me.id)
        .flatMap: teams =>
          forms
            .edit(me, teams, simul)
            .bindFromRequest()
            .fold(
              err => BadRequest(html.simul.form.edit(err, teams, simul)).toFuccess,
              data => env.simul.api.update(simul, data, me, teams) inject Redirect(routes.Simul.show(id))
            )
  }

  private def AsHost(simulId: SimulId)(f: Sim => Fu[Result])(using ctx: Context): Fu[Result] =
    env.simul.repo.find(simulId).flatMap {
      case None                                                                    => notFound
      case Some(simul) if ctx.userId.has(simul.hostId) || isGranted(_.ManageSimul) => f(simul)
      case _                                                                       => fuccess(Unauthorized)
    }

  private def WithEditableSimul(id: SimulId)(f: Sim => Fu[Result])(using Context): Fu[Result] =
    AsHost(id): sim =>
      if (sim.isStarted) then Redirect(routes.Simul.show(sim.id)).toFuccess
      else f(sim)

  private given lila.gathering.Condition.GetUserTeamIds = user => env.team.cached.teamIdsList(user.id)
