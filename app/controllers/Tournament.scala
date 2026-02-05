package controllers

import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.common.Json.given
import scalalib.data.Preload
import lila.gathering.Condition.GetMyTeamIds
import lila.tournament.{ MyInfo, Tournament as Tour, TournamentForm }

final class Tournament(env: Env, apiC: => Api)(using akka.stream.Materializer) extends LilaController(env):

  private def repo = env.tournament.tournamentRepo
  private def api = env.tournament.api
  private def jsonView = env.tournament.jsonView
  private def forms = env.tournament.forms
  private def cachedTour(id: TourId) = env.tournament.cached.tourCache.byId(id)
  private given lila.core.team.LightTeam.Api = env.team.lightTeamApi

  private def tournamentNotFound(using Context) = NotFound.page(views.tournament.ui.notFound)

  def home = Open(serveHome)
  def homeLang = LangPage(routes.Tournament.home)(serveHome)

  private def serveHome(using ctx: Context) = NoBot:
    for
      teamIds <- ctx.userId.so(env.team.cached.teamIdsList)
      (scheduled, visible) <- env.tournament.featuring.tourIndex.get(teamIds)
      scheduleJson <- env.tournament.apiJsonView(visible)
      response <- negotiate(
        html = for
          finished <- api.notableFinished
          winners <- env.tournament.winners.all
          page <- renderPage(views.tournament.list.home(scheduled, finished, winners, scheduleJson))
        yield Ok(page).noCache,
        json = Ok(scheduleJson)
      )
    yield response

  def help = Open:
    Ok.page(views.tournament.faq)

  def leaderboard = Open:
    for
      winners <- env.tournament.winners.all
      _ <- env.user.lightUserApi.preloadMany(winners.userIds)
      page <- renderPage(views.tournament.list.leaderboard(winners))
    yield Ok(page)

  private[controllers] def canHaveChat(tour: Tour, json: Option[JsObject])(using ctx: Context): Boolean =
    tour.hasChat && ctx.kid.no && ctx.noBot && // no public chats for kids
      ctx.me.fold(!tour.isPrivate && HTTPRequest.isHuman(ctx.req)):
        _ => // anon can see public chats, except for private tournaments
          (!tour.isPrivate || json.forall(jsonHasMe) || ctx.is(tour.createdBy) ||
            isGrantedOpt(_.ChatTimeout)) // private tournament that I joined or has ChatTimeout

  private def loadChat(tour: Tour, json: JsObject)(using Context): Fu[Option[lila.chat.UserChat.Mine]] =
    canHaveChat(tour, json.some).optionFu:
      env.chat.api.userChat.cached
        .findMine(tour.id.into(ChatId))
        .map(_.copy(locked = !env.api.chatFreshness.of(tour)))

  private def jsonHasMe(js: JsObject): Boolean = (js \ "me").toOption.isDefined

  def show(id: TourId) = Open:
    val page = getInt("page")
    WithVisibleTournament(id): tour =>
      negotiate(
        html = for
          myInfo <- ctx.me.so { jsonView.fetchMyInfo(tour, _) }
          verdicts <- api.getVerdicts(tour, myInfo.isDefined)
          version <- env.tournament.version(tour.id)
          json <- jsonView(
            tour = tour,
            page = page,
            playerInfoExt = none,
            socketVersion = version.some,
            partial = false,
            withScores = true,
            withAllowList = false,
            withDescription = false,
            myInfo = Preload[Option[MyInfo]](myInfo),
            addReloadEndpoint = env.tournament.lilaHttp.handles.some
          )
          chat <- loadChat(tour, json)
          _ <- tour.teamBattle.so: b =>
            env.team.cached.preloadSet(b.teams)
          streamers <- streamerCache.get(tour.id)
          shieldOwner <- env.tournament.shieldApi.currentOwner(tour)
          page <- renderPage(views.tournament.show(tour, verdicts, json, chat, streamers, shieldOwner))
        yield
          env.tournament.lilaHttp.hit(tour)
          Ok(page).noCache
        ,
        json = for
          playerInfoExt <- getUserStr("playerInfo").map(_.id).so(api.playerInfo(tour, _))
          socketVersion <- getBool("socketVersion").optionFu(env.tournament.version(tour.id))
          partial = getBool("partial")
          json <- jsonView(
            tour = tour,
            page = page,
            playerInfoExt = playerInfoExt,
            socketVersion = socketVersion,
            partial = partial,
            withScores = getBoolOpt("scores") | true,
            withAllowList = true,
            withDescription = true,
            addReloadEndpoint = env.tournament.lilaHttp.handles.some
          )
          chatOpt <- partial.not.so(loadChat(tour, json))
          jsChat <- chatOpt.traverse: c =>
            env.chat.json.mobile(c.chat)
        yield Ok(json.add("chat" -> jsChat)).noCache
      )
        .monSuccess:
          _.tournament.apiShowPartial(partial = getBool("partial"), HTTPRequest.clientName(ctx.req))

  def apiShow(id: TourId) = AnonOrScoped(): ctx ?=>
    WithVisibleTournament(id): tour =>
      val maxPage = if ctx.isMobileOauth then 5_000 else 200
      val page = (getInt("page") | 1).atLeast(1).atMost(maxPage)
      given GetMyTeamIds = me => env.team.cached.teamIdsList(me.userId)
      for
        data <- env.tournament.jsonView(
          tour = tour,
          page = page.some,
          playerInfoExt = none,
          socketVersion = none,
          partial = false,
          withScores = true,
          withDescription = true,
          withAllowList = true
        )
        chatOpt <- getBool("chat").so(loadChat(tour, data))
        jsChat <- chatOpt.traverse(c => env.chat.json.mobile(c.chat))
        socketVersion <- getBool("socketVersion").optionFu(env.tournament.version(tour.id))
      yield JsonOk:
        data.add("chat", jsChat).add("socketVersion" -> socketVersion)

  def standing(id: TourId, page: Int) = Open:
    WithVisibleTournament(id): tour =>
      JsonOk:
        env.tournament.standingApi(tour, page, withScores = getBoolOpt("scores") | true)

  def pageOf(id: TourId, userId: UserStr) = Open:
    WithVisibleTournament(id): tour =>
      Found(api.pageOf(tour, userId.id)): page =>
        JsonOk:
          env.tournament.standingApi(tour, page, withScores = getBoolOpt("scores") | true)

  def player(tourId: TourId, userId: UserStr) = Open:
    WithVisibleTournament(tourId): tour =>
      Found(api.playerInfo(tour, userId.id)): player =>
        JsonOk:
          jsonView.playerInfoExtended(tour, player)

  def teamInfo(tourId: TourId, teamId: TeamId) = Open:
    WithVisibleTournament(tourId): tour =>
      Found(env.team.lightTeam(teamId)): team =>
        for
          joined <- ctx.useMe(env.team.api.isMember(team.id))
          res <- negotiate(
            FoundPage(api.teamBattleTeamInfo(tour, teamId)):
              views.tournament.teamBattle.teamInfo(tour, team, _)
            ,
            jsonView.teamInfo(tour, teamId, joined).orNotFound(JsonOk)
          )
        yield res

  def join(id: TourId) = AuthBody(parse.json) { ctx ?=> me ?=>
    NoLame:
      NoPlayban:
        limit.tourJoinOrResume(me, rateLimited):
          doJoin(id, TournamentForm.tournamentJoin(ctx.body.body)).map:
            _.error.fold(jsonOkResult): error =>
              BadRequest(Json.obj("joined" -> false, "error" -> error))
  }

  def apiJoin(id: TourId) = ScopedBody(_.Tournament.Write, _.Bot.Play, _.Web.Mobile) { ctx ?=> me ?=>
    NoLame:
      NoPlayban:
        limit.tourJoinOrResume(me, rateLimited):
          val data =
            bindForm(TournamentForm.joinForm)(_ => TournamentForm.TournamentJoin(none, none), identity)
          doJoin(id, data).map:
            _.error.fold(jsonOkResult): error =>
              BadRequest(Json.obj("error" -> error))
  }

  private def doJoin(tourId: TourId, data: TournamentForm.TournamentJoin)(using Me) =
    data.team
      .so(env.team.api.isGranted(_, _.Tour))
      .flatMap: isLeader =>
        api.join(tourId, data = data, asLeader = isLeader)

  def pause(id: TourId) = Auth { ctx ?=> me ?=>
    WithVisibleTournament(id): tour =>
      api.selfPause(tour.id, me)
      if HTTPRequest.isXhr(ctx.req) then jsonOkResult
      else Redirect(routes.Tournament.show(tour.id))
  }

  def apiWithdraw(id: TourId) = ScopedBody(_.Tournament.Write, _.Bot.Play, _.Web.Mobile) { _ ?=> me ?=>
    WithVisibleTournament(id): tour =>
      api.selfPause(tour.id, me).inject(jsonOkResult)
  }

  def form = Auth { ctx ?=> me ?=>
    NoBot:
      env.team.api.lightsByTourLeader(me).flatMap { teams =>
        Ok.page(views.tournament.form.create(forms.create(teams, forClas = getBool("clas")), teams))
      }
  }

  def teamBattleForm(teamId: TeamId) = Auth { _ ?=> me ?=>
    NoBot:
      env.team.api.lightsByTourLeader(me).flatMap { teams =>
        env.team.api
          .isGranted(teamId, _.Tour)
          .elseNotFound(Ok.page(views.tournament.form.create(forms.create(teams, teamId.some), Nil)))
      }
  }

  private val createLimitPerIP = env.security.ipTrust.rateLimit(800, 1.day, "tournament.ip")

  private[controllers] def rateLimitCreation(
      isPrivate: Boolean,
      fail: => Fu[Result]
  )(create: => Fu[Result])(using me: Me, req: RequestHeader): Fu[Result] =
    val cost =
      if me.is(UserId.lichess) then 1
      else if isGranted(_.ManageTournament) then 2
      else if me.hasTitle ||
        env.streamer.liveApi.isStreaming(me) ||
        me.isVerified ||
        isPrivate
      then 5
      else 20
    limit.tourCreate(me, fail, cost = cost):
      createLimitPerIP(fail, cost = cost, msg = me.username.value):
        create

  def webCreate = AuthBody(_ ?=> _ ?=> create)
  def apiCreate = ScopedBody(_.Tournament.Write)(_ ?=> _ ?=> create)

  private def create(using BodyContext[?])(using me: Me) = NoBot:
    def whenRateLimited = negotiate(Redirect(routes.Tournament.home), rateLimited)
    env.team.api
      .lightsByTourLeader(me)
      .flatMap: teams =>
        bindForm(forms.create(teams))(
          err =>
            negotiate(
              BadRequest.page(views.tournament.form.create(err, teams)),
              doubleJsonFormError(err)
            ),
          setup =>
            rateLimitCreation(setup.isPrivate, whenRateLimited):
              given GetMyTeamIds = _ => fuccess(teams.map(_.id))
              for
                tour <- api.createTournament(setup, teams, andJoin = ctx.isWebAuth)
                _ <- env.api.clas.onArenaCreate(tour)
                tourUrl = routes.Tournament.show(tour.id)
                _ = env.report.api.automodComms(setup.automodText, tourUrl.url).discard
                result <- negotiate(
                  html = Redirect {
                    if tour.isTeamBattle then routes.Tournament.teamBattleEdit(tour.id)
                    else tourUrl
                  }.flashSuccess,
                  json = jsonView(
                    tour,
                    none,
                    none,
                    none,
                    partial = false,
                    withScores = false,
                    withAllowList = true,
                    withDescription = true
                  ).map { Ok(_) }
                )
              yield result
        )

  def apiUpdate(id: TourId) = ScopedBody(_.Tournament.Write) { ctx ?=> me ?=>
    WithEditableTournament(id): tour =>
      env.team.api.lightsByTourLeader(me).flatMap { teams =>
        bindForm(forms.edit(teams, tour))(
          jsonFormError,
          data =>
            given GetMyTeamIds = _ => fuccess(teams.map(_.id))
            for
              tour <- api.apiUpdate(tour, data)
              json <- jsonView(
                tour,
                none,
                none,
                none,
                partial = false,
                withScores = true,
                withAllowList = true,
                withDescription = true
              )
            yield
              discard { env.report.api.automodComms(data.automodText, routes.Tournament.show(tour.id).url) }
              Ok(json)
        )
      }
  }

  def apiTerminate(id: TourId) = ScopedBody(_.Tournament.Write) { _ ?=> me ?=>
    WithEditableTournament(id): tour =>
      api.kill(tour).inject(jsonOkResult)
  }

  def teamBattleEdit(id: TourId) = Auth { ctx ?=> me ?=>
    WithEditableTournament(id): tour =>
      tour.teamBattle.so: battle =>
        for
          teams <- env.team.teamRepo.byOrderedIds(battle.sortedTeamIds)
          _ <- env.user.lightUserApi.preloadMany(teams.map(_.createdBy))
          form = lila.tournament.TeamBattle.DataForm.edit(
            teams.map: t =>
              s"""${t.id} "${t.name}" by ${env.user.lightUserApi
                  .sync(t.createdBy)
                  .fold(t.createdBy)(_.name)}""",
            battle.nbLeaders
          )
          page <- Ok.page(views.tournament.teamBattle.edit(tour, form))
        yield page
  }

  def teamBattleUpdate(id: TourId) = AuthBody { ctx ?=> me ?=>
    WithEditableTournament(id): tour =>
      bindForm(lila.tournament.TeamBattle.DataForm.empty)(
        err => BadRequest.page(views.tournament.teamBattle.edit(tour, err)),
        res =>
          api
            .teamBattleUpdate(tour, res, env.team.api.filterExistingIds)
            .inject(Redirect(routes.Tournament.show(tour.id)))
      )
  }

  def apiTeamBattleUpdate(id: TourId) = ScopedBody(_.Tournament.Write) { ctx ?=> me ?=>
    Found(cachedTour(id)):
      case tour if tour.createdBy.is(me) || isGranted(_.ManageTournament) && !tour.isFinished =>
        bindForm(lila.tournament.TeamBattle.DataForm.empty)(
          jsonFormError,
          res =>
            api.teamBattleUpdate(tour, res, env.team.api.filterExistingIds) >> {
              cachedTour(tour.id)
                .map(_ | tour)
                .flatMap { tour =>
                  jsonView(
                    tour,
                    none,
                    none,
                    none,
                    partial = false,
                    withScores = true,
                    withAllowList = true,
                    withDescription = true
                  )
                }
                .map { Ok(_) }
            }
        )
      case _ => BadRequest(jsonError("Can't update that tournament."))
  }

  def featured = OpenOrScoped():
    negotiateJson:
      JsonOk(env.api.mobile.tournaments)

  def shields = Open:
    for
      history <- env.tournament.shieldApi.history(5.some)
      _ <- env.user.lightUserApi.preloadMany(history.userIds)
      page <- renderPage(views.tournament.list.shields(history))
    yield Ok(page)

  def categShields(k: String) = Open:
    FoundPage(env.tournament.shieldApi.byCategKey(k)): (categ, awards) =>
      env.user.lightUserApi
        .preloadMany(awards.map(_.owner))
        .inject(views.tournament.list.shields.byCateg(categ, awards))

  def calendar = Open:
    api.calendar.flatMap: tours =>
      Ok.page(views.tournament.list.calendar(env.tournament.apiJsonView.calendar(tours)))

  def history(freq: String, page: Int) = Open:
    lila.tournament.Schedule.Freq.byName.get(freq).so { fr =>
      api.history(fr, page).flatMap { pager =>
        val userIds = pager.currentPageResults.flatMap(_.winnerId)
        env.user.lightUserApi.preloadMany(userIds) >>
          Ok.page(views.tournament.list.history(fr, pager))
      }
    }

  def edit(id: TourId) = Auth { ctx ?=> me ?=>
    WithEditableTournament(id): tour =>
      env.team.api.lightsByTourLeader(me).flatMap { teams =>
        val form = forms.edit(teams, tour)
        Ok.page(views.tournament.form.edit(tour, form, teams))
      }
  }

  def update(id: TourId) = AuthBody { ctx ?=> me ?=>
    WithEditableTournament(id): tour =>
      env.team.api.lightsByTourLeader(me).flatMap { teams =>
        bindForm(forms.edit(teams, tour))(
          err => BadRequest.page(views.tournament.form.edit(tour, err, teams)),
          data => api.update(tour, data).inject(Redirect(routes.Tournament.show(id)).flashSuccess)
        )
      }
  }

  def terminate(id: TourId) = Auth { ctx ?=> me ?=>
    WithEditableTournament(id): tour =>
      api
        .kill(tour)
        .inject:
          env.mod.logApi.terminateTournament(tour.name())
          Redirect:
            tour.singleTeamId.fold(routes.Tournament.home)(routes.Team.show(_))
  }

  def byTeam(id: TeamId) = Anon:
    apiC.jsonDownload:
      val status = get("status").flatMap(lila.core.tournament.Status.byName)
      repo
        .byTeamCursor(id, status, getAs[UserStr]("createdBy"), get("name"))
        .documentSource(getInt("max") | 100)
        .mapAsync(1)(env.tournament.apiJsonView.fullJson)
        .throttle(20, 1.second)

  def battleTeams(id: TourId) = Open:
    cachedTour(id).flatMap:
      _.filter(_.isTeamBattle).so: tour =>
        env.tournament.cached.battle.teamStanding
          .get(tour.id)
          .flatMap: standing =>
            env.team.cached.preloadMany(standing.map(_.teamId)) >>
              Ok.page(views.tournament.teamBattle.standing(tour, standing))

  def moderation(id: TourId, view: String) = Secure(_.GamesModView) { ctx ?=> me ?=>
    Found(cachedTour(id)): tour =>
      env.tournament
        .moderation(tour.id, view)
        .flatMap: (view, players) =>
          Ok.page(views.tournament.moderation.page(tour, view, players))
  }

  private def WithEditableTournament(id: TourId)(
      f: Tour => Fu[Result]
  )(using ctx: Context, me: Me): Fu[Result] =
    WithVisibleTournament(id): t =>
      if (t.createdBy.is(me) && !t.isFinished) || isGranted(_.ManageTournament)
      then f(t)
      else Redirect(routes.Tournament.show(t.id))

  private def WithVisibleTournament(id: TourId)(
      f: Tour => Fu[Result]
  )(using ctx: Context): Fu[Result] =
    def nope = negotiate(tournamentNotFound, notFoundJson("No such tournament"))
    cachedTour(id).flatMap:
      case None => nope
      case Some(tour) =>
        tour.singleTeamId
          .fold(fuTrue)(env.team.api.clasMemberCheck)
          .flatMap:
            if _ then f(tour) else nope

  private val streamerCache = env.memo.cacheApi[TourId, List[UserId]](256, "tournament.streamers"):
    _.expireAfterWrite(15.seconds)
      .maximumSize(256)
      .buildAsyncFuture: tourId =>
        repo
          .isUnfinished(tourId)
          .flatMapz:
            env.streamer.liveApi.all.flatMap:
              // #TODO it can become expensive to run `hasUser` for many streamers
              // there should be an `hasUsers` method
              _.streams
                .sequentially: stream =>
                  env.tournament
                    .hasUser(tourId, stream.streamer.userId)
                    .dmap(_.option(stream.streamer.userId))
                .dmap(_.flatten)

  private given GetMyTeamIds = me => env.team.cached.teamIdsList(me.userId)
