package controllers

import play.api.mvc.*
import views.*

import lila.app.{ given, * }
import lila.common.HTTPRequest
import lila.simul.{ Simul as Sim }

final class Simul(env: Env) extends LilaController(env):

  private def forms = lila.simul.SimulForm

  private def simulNotFound(using Context) = NotFound.page(html.simul.bits.notFound())

  def home     = Open(serveHome)
  def homeLang = LangPage(routes.Simul.home)(serveHome)

  private def serveHome(using ctx: Context) = NoBot:
    fetchSimuls(ctx.me) flatMap { case (((pending, created), started), finished) =>
      Ok.page(html.simul.home(pending, created, started, finished))
    }

  val apiList = OpenOrScoped(): ctx ?=>
    fetchSimuls(ctx.me) flatMap { case (((pending, created), started), finished) =>
      env.simul.jsonView.apiAll(pending, created, started, finished) map JsonOk
    }

  val homeReload = Open:
    fetchSimuls(ctx.me) flatMap { case (((pending, created), started), finished) =>
      Ok.page(html.simul.homeInner(pending, created, started, finished))
    }

  private def fetchSimuls(me: Option[lila.user.User]) =
    me.so { u =>
      env.simul.repo.findPending(u.id)
    } zip
      env.simul.allCreatedFeaturable.get {} zip
      env.simul.repo.allStarted zip
      env.simul.repo.allFinishedFeaturable(20)

  def show(id: SimulId) = Open:
    env.simul.repo find id flatMap {
      _.fold(simulNotFound): sim =>
        WithMyPerf(sim.mainPerfType):
          for
            verdicts <- env.simul.api.getVerdicts(sim)
            version  <- env.simul.version(sim.id)
            json     <- env.simul.jsonView(sim, verdicts)
            chat     <- canHaveChat(sim) soFu env.chat.api.userChat.cached.findMine(sim.id into ChatId)
            _ <- chat.so: c =>
              env.user.lightUserApi.preloadMany(c.chat.userIds)
            stream <- env.streamer.liveStreamApi one sim.hostId
            page   <- renderPage(html.simul.show(sim, version, json, chat, stream, verdicts))
          yield Ok(page).noCache
    }

  private[controllers] def canHaveChat(simul: Sim)(using ctx: Context): Boolean =
    ctx.kid.no && ctx.noBot &&                    // no public chats for kids or bots
      ctx.me.fold(HTTPRequest.isHuman(ctx.req)) { // anon can see public chats
        env.chat.panic.allowed(_)
      } && simul.conditions.teamMember
        .map(_.teamId)
        .forall: teamId =>
          ctx.userId.exists:
            env.team.api.syncBelongsTo(teamId, _) || isGrantedOpt(_.ChatTimeout)

  def hostPing(simulId: SimulId) = Open:
    AsHost(simulId): simul =>
      env.simul.api hostPing simul inject jsonOkResult

  def start(simulId: SimulId) = Open:
    AsHost(simulId): simul =>
      env.simul.api start simul.id inject jsonOkResult

  def abort(simulId: SimulId) = Auth { ctx ?=> me ?=>
    AsHost(simulId): simul =>
      env.simul.api abort simul.id inject {
        if !simul.isHost(me) then env.mod.logApi.terminateTournament(simul.fullName)
        if HTTPRequest isXhr ctx.req
        then jsonOkResult
        else Redirect(routes.Simul.home)
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
          _ => BadRequest,
          text => env.simul.api.setText(simul.id, text) inject jsonOkResult
        )

  def form = Auth { ctx ?=> me ?=>
    NoLameOrBot:
      Ok.pageAsync:
        env.team.api
          .lightsByTourLeader(me)
          .map: teams =>
            html.simul.form.create(forms.create(teams), teams)
  }

  def create = AuthBody { ctx ?=> me ?=>
    NoLameOrBot:
      env.team.api
        .lightsByTourLeader(me)
        .flatMap: teams =>
          forms
            .create(teams)
            .bindFromRequest()
            .fold(
              err => BadRequest.page(html.simul.form.create(err, teams)),
              setup =>
                env.simul.api.create(setup, teams) map { simul =>
                  Redirect(routes.Simul.show(simul.id))
                }
            )
  }

  def join(id: SimulId, variant: chess.variant.Variant.LilaKey) = Auth { ctx ?=> me ?=>
    NoLameOrBot:
      env.simul.api
        .addApplicant(id, variant)
        .inject:
          if HTTPRequest isXhr ctx.req
          then jsonOkResult
          else Redirect(routes.Simul.show(id))
  }

  def withdraw(id: SimulId) = Auth { ctx ?=> me ?=>
    env.simul.api.removeApplicant(id, me) inject {
      if HTTPRequest isXhr ctx.req then jsonOkResult
      else Redirect(routes.Simul.show(id))
    }
  }

  def edit(id: SimulId) = Auth { ctx ?=> me ?=>
    WithEditableSimul(id) { simul =>
      Ok.pageAsync:
        env.team.api.lightsByTourLeader(me) map { teams =>
          html.simul.form.edit(forms.edit(teams, simul), teams, simul)
        }
    }
  }

  def update(id: SimulId) = AuthBody { ctx ?=> me ?=>
    WithEditableSimul(id): simul =>
      env.team.api
        .lightsByTourLeader(me)
        .flatMap: teams =>
          forms
            .edit(teams, simul)
            .bindFromRequest()
            .fold(
              err => BadRequest.page(html.simul.form.edit(err, teams, simul)),
              data => env.simul.api.update(simul, data, teams) inject Redirect(routes.Simul.show(id))
            )
  }

  def byUser(username: UserStr, page: Int) = Open:
    Reasonable(page):
      val userOption =
        env.user.repo.byId(username).map { _.filter(_.enabled.yes || isGrantedOpt(_.SeeReport)) }
      Found(userOption): user =>
        Ok.pageAsync:
          env.simul.api.hostedByUser(user.id, page).map {
            html.simul.hosted(user, _)
          }

  private def AsHost(simulId: SimulId)(f: Sim => Fu[Result])(using ctx: Context): Fu[Result] =
    Found(env.simul.repo.find(simulId)): simul =>
      if ctx.is(simul.hostId) || isGrantedOpt(_.ManageSimul) then f(simul)
      else Unauthorized

  private def WithEditableSimul(id: SimulId)(f: Sim => Fu[Result])(using Context): Fu[Result] =
    AsHost(id): sim =>
      if sim.isStarted
      then Redirect(routes.Simul.show(sim.id))
      else f(sim)

  private given lila.gathering.Condition.GetMyTeamIds = me => env.team.cached.teamIdsList(me.userId)
