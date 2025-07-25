package controllers

import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.simul.Simul as Sim

final class Simul(env: Env) extends LilaController(env):

  private def forms = lila.simul.SimulForm

  private def simulNotFound(using Context) = NotFound.page(views.simul.ui.notFound)

  def home = Open(serveHome)
  def homeLang = LangPage(routes.Simul.home)(serveHome)

  private def serveHome(using ctx: Context) = NoBot:
    for
      (pending, created, started, finished) <- fetchSimuls
      _ <- env.simul.api.checkOngoingSimuls(started)
      page <- Ok.page(views.simul.home(pending, created, started, finished))
    yield page

  val apiList = OpenOrScoped(): ctx ?=>
    fetchSimuls.flatMap: (pending, created, started, finished) =>
      env.simul.jsonView.apiAll(pending, created, started, finished).map(JsonOk)

  val homeReload = Open:
    fetchSimuls.flatMap: (pending, created, started, finished) =>
      Ok.snip(views.simul.home.homeInner(pending, created, started, finished))

  private def fetchSimuls(using me: Option[Me]): Fu[(List[Sim], List[Sim], List[Sim], List[Sim])] =
    (
      me.so(u => env.simul.repo.findPending(u.userId)),
      env.simul.allCreatedFeaturable.get {},
      env.simul.repo.allStarted,
      env.simul.repo.allFinishedFeaturable(20)
    ).tupled

  def show(id: SimulId) = Open:
    env.simul.repo
      .find(id)
      .flatMap:
        _.fold(simulNotFound): sim =>
          WithMyPerf(sim.mainPerfType):
            for
              verdicts <- env.simul.api.getVerdicts(sim)
              version <- env.simul.version(sim.id)
              json <- env.simul.jsonView(sim, verdicts)
              chat <- canHaveChat(sim).soFu(env.chat.api.userChat.cached.findMine(sim.id.into(ChatId)))
              stream <- env.streamer.liveStreamApi.one(sim.hostId)
              page <- renderPage(views.simul.show(sim, version, json, chat, stream, verdicts))
            yield Ok(page).noCache

  private[controllers] def canHaveChat(simul: Sim)(using ctx: Context): Boolean =
    ctx.kid.no && ctx.noBot && // no public chats for kids or bots
      (ctx.isAuth || HTTPRequest.isHuman(ctx.req)) &&
      simul.conditions.teamMember
        .map(_.teamId)
        .forall: teamId =>
          ctx.userId.exists:
            env.team.api.syncBelongsTo(teamId, _) || isGrantedOpt(_.ChatTimeout)

  def hostPing(simulId: SimulId) = Open:
    AsHost(simulId): simul =>
      env.simul.api.hostPing(simul).inject(jsonOkResult)

  def start(simulId: SimulId) = Open:
    AsHost(simulId): simul =>
      env.simul.api.start(simul.id).inject(jsonOkResult)

  def abort(simulId: SimulId) = Auth { ctx ?=> me ?=>
    AsHost(simulId): simul =>
      env.simul.api
        .abort(simul.id)
        .inject:
          if !simul.isHost(me) then env.mod.logApi.terminateTournament(simul.fullName)
          if HTTPRequest.isXhr(ctx.req)
          then jsonOkResult
          else Redirect(routes.Simul.home)
  }

  def accept(simulId: SimulId, userId: UserStr) = Open:
    AsHost(simulId): simul =>
      env.simul.api.accept(simul.id, userId.id, v = true).inject(jsonOkResult)

  def reject(simulId: SimulId, userId: UserStr) = Open:
    AsHost(simulId): simul =>
      env.simul.api.accept(simul.id, userId.id, v = false).inject(jsonOkResult)

  def setText(simulId: SimulId) = OpenBody:
    AsHost(simulId): simul =>
      bindForm(forms.setText)(
        _ => BadRequest,
        text => env.simul.api.setText(simul.id, text).inject(jsonOkResult)
      )

  def form = Auth { ctx ?=> me ?=>
    NoLameOrBot:
      Ok.async:
        env.team.api
          .lightsOf(me)
          .map: teams =>
            views.simul.form.create(forms.create(teams), teams)
  }

  def create = AuthBody { ctx ?=> me ?=>
    NoLameOrBot:
      for
        teams <- env.team.api.lightsOf(me)
        res <- bindForm(forms.create(teams))(
          err => BadRequest.page(views.simul.form.create(err, teams)),
          setup =>
            for simul <- env.simul.api.create(setup)
            yield Redirect(routes.Simul.show(simul.id))
        )
      yield res
  }

  def join(id: SimulId, variant: chess.variant.Variant.LilaKey) = Auth { ctx ?=> me ?=>
    NoLameOrBot:
      env.simul.api
        .addApplicant(id, variant)
        .inject:
          if HTTPRequest.isXhr(ctx.req)
          then jsonOkResult
          else Redirect(routes.Simul.show(id))
  }

  def withdraw(id: SimulId) = Auth { ctx ?=> me ?=>
    env.simul.api
      .removeApplicant(id, me)
      .inject:
        if HTTPRequest.isXhr(ctx.req) then jsonOkResult
        else Redirect(routes.Simul.show(id))
  }

  def edit(id: SimulId) = Auth { ctx ?=> me ?=>
    AsHost(id): simul =>
      Ok.async:
        env.team.api.lightsOf(me).map { teams =>
          views.simul.form.edit(forms.edit(teams, simul), teams, simul)
        }
  }

  def update(id: SimulId) = AuthBody { ctx ?=> me ?=>
    AsHost(id): simul =>
      env.team.api
        .lightsOf(me)
        .flatMap: teams =>
          def errPage(err: lila.simul.SimulForm.EitherForm) =
            BadRequest.page(views.simul.form.edit(err, teams, simul))
          def redirect = Redirect(routes.Simul.show(id))
          forms
            .edit(teams, simul)
            .fold(
              f =>
                bindForm(f)(
                  err => errPage(Left(err)),
                  data => for _ <- env.simul.api.update(simul, data) yield redirect
                ),
              f =>
                bindForm(f)(
                  err => errPage(Right(err)),
                  data => for _ <- env.simul.api.update(simul, data) yield redirect
                )
            )
  }

  def byUser(username: UserStr, page: Int) = Open:
    Reasonable(page):
      Found(meOrFetch(username).map(_.filter(_.enabled.yes || isGrantedOpt(_.SeeReport)))): user =>
        Ok.async:
          env.simul.api
            .hostedByUser(user.id, page)
            .map:
              views.simul.home.hosted(user, _)

  private def AsHost(simulId: SimulId)(f: Sim => Fu[Result])(using ctx: Context): Fu[Result] =
    Found(env.simul.repo.find(simulId)): simul =>
      if ctx.is(simul.hostId) || isGrantedOpt(_.ManageSimul) then f(simul)
      else Unauthorized

  private given lila.gathering.Condition.GetMyTeamIds = me => env.team.cached.teamIdsList(me.userId)
