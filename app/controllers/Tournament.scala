package controllers

import play.api.libs.json.*
import play.api.mvc.*
import views.*

import lila.app.{ given, * }
import lila.common.{ HTTPRequest, Preload }
import lila.common.Json.given
import lila.memo.CacheApi.*
import lila.tournament.{ Tournament as Tour, TournamentForm, VisibleTournaments, MyInfo }
import lila.gathering.Condition.GetMyTeamIds
import play.api.i18n.Lang

final class Tournament(env: Env, apiC: => Api)(using akka.stream.Materializer) extends LilaController(env):

  private def repo       = env.tournament.tournamentRepo
  private def api        = env.tournament.api
  private def jsonView   = env.tournament.jsonView
  private def forms      = env.tournament.forms
  private def cachedTour = env.tournament.cached.tourCache.byId

  private def tournamentNotFound(using Context) = NotFound.page(html.tournament.bits.notFound())

  private[controllers] val upcomingCache = env.memo.cacheApi.unit[(VisibleTournaments, List[Tour])] {
    _.refreshAfterWrite(3.seconds)
      .buildAsyncFuture { _ =>
        for
          visible   <- api.fetchVisibleTournaments
          scheduled <- repo.allScheduledDedup
        yield (visible, scheduled)
      }
  }

  def home     = Open(serveHome)
  def homeLang = LangPage(routes.Tournament.home)(serveHome)

  private def serveHome(using ctx: Context) = NoBot:
    for
      (visible, scheduled) <- upcomingCache.getUnit
      teamIds              <- ctx.userId.so(env.team.cached.teamIdsList)
      allTeamIds = (TeamId.from(env.featuredTeamsSetting.get().value) ++ teamIds).distinct
      teamVisible  <- repo.visibleForTeams(allTeamIds, 5 * 60)
      scheduleJson <- env.tournament.apiJsonView(visible add teamVisible)
      response <- negotiate(
        html = for
          finished <- api.notableFinished
          winners  <- env.tournament.winners.all
          page     <- renderPage(html.tournament.home(scheduled, finished, winners, scheduleJson))
        yield
          pageHit
          Ok(page).noCache
        ,
        json = Ok(scheduleJson)
      )
    yield response

  def help = Open:
    Ok.page(html.tournament.faq.page)

  def leaderboard = Open:
    for
      winners <- env.tournament.winners.all
      _       <- env.user.lightUserApi preloadMany winners.userIds
      page    <- renderPage(html.tournament.leaderboard(winners))
    yield Ok(page)

  private[controllers] def canHaveChat(tour: Tour, json: Option[JsObject])(using ctx: Context): Boolean =
    tour.hasChat && ctx.noKid && ctx.noBot && // no public chats for kids
      ctx.me.fold(!tour.isPrivate && HTTPRequest.isHuman(ctx.req)) {
        u => // anon can see public chats, except for private tournaments
          (!tour.isPrivate || json.fold(true)(jsonHasMe) || ctx.is(tour.createdBy) ||
            isGrantedOpt(_.ChatTimeout)) && // private tournament that I joined or has ChatTimeout
          (env.chat.panic.allowed(u) || isGrantedOpt(_.ChatTimeout))
      }

  private def jsonHasMe(js: JsObject): Boolean = (js \ "me").toOption.isDefined

  def show(id: TourId) = Open:
    val page = getInt("page")
    cachedTour(id) flatMap { tourOption =>
      def loadChat(tour: Tour, json: JsObject) =
        canHaveChat(tour, json.some) so env.chat.api.userChat.cached
          .findMine(ChatId(tour.id))
          .flatMap: c =>
            env.user.lightUserApi.preloadMany(c.chat.userIds) inject
              c.copy(locked = !env.api.chatFreshness.of(tour)).some
      negotiate(
        html = tourOption
          .fold(tournamentNotFound): tour =>
            for
              myInfo   <- ctx.me.so { jsonView.fetchMyInfo(tour, _) }
              verdicts <- api.getVerdicts(tour, myInfo.isDefined)
              version  <- env.tournament.version(tour.id)
              json <- jsonView(
                tour = tour,
                page = page,
                getTeamName = env.team.getTeamName.apply,
                playerInfoExt = none,
                socketVersion = version.some,
                partial = false,
                withScores = true,
                myInfo = Preload[Option[MyInfo]](myInfo)
              ).map(jsonView.addReloadEndpoint(_, tour, env.tournament.lilaHttp.handles))
              chat <- loadChat(tour, json)
              _ <- tour.teamBattle.so: b =>
                env.team.cached.preloadSet(b.teams)
              streamers   <- streamerCache get tour.id
              shieldOwner <- env.tournament.shieldApi currentOwner tour
              page <- renderPage(html.tournament.show(tour, verdicts, json, chat, streamers, shieldOwner))
            yield
              env.tournament.lilaHttp.hit(tour)
              Ok(page).noCache
          .monSuccess(_.tournament.apiShowPartial(partial = false, HTTPRequest clientName ctx.req)),
        json = tourOption
          .fold[Fu[Result]](notFoundJson("No such tournament")): tour =>
            for
              playerInfoExt <- getUserStr("playerInfo").map(_.id).so(api.playerInfo(tour, _))
              socketVersion <- getBool("socketVersion").soFu(env.tournament version tour.id)
              partial = getBool("partial")
              json <- jsonView(
                tour = tour,
                page = page,
                getTeamName = env.team.getTeamName.apply,
                playerInfoExt = playerInfoExt,
                socketVersion = socketVersion,
                partial = partial,
                withScores = getBoolOpt("scores") | true
              )
              chat <- !partial so loadChat(tour, json)
            yield Ok(json.add("chat" -> chat.map: c =>
              lila.chat.JsonView.mobile(chat = c.chat))).noCache
          .monSuccess(_.tournament.apiShowPartial(getBool("partial"), HTTPRequest clientName ctx.req))
      )
    }

  def standing(id: TourId, page: Int) = Open:
    Found(cachedTour(id)): tour =>
      JsonOk:
        env.tournament.standingApi(tour, page, withScores = getBoolOpt("scores") | true)

  def pageOf(id: TourId, userId: UserStr) = Open:
    Found(cachedTour(id)): tour =>
      Found(api.pageOf(tour, userId.id)): page =>
        JsonOk:
          env.tournament.standingApi(tour, page, withScores = getBoolOpt("scores") | true)

  def player(tourId: TourId, userId: UserStr) = Anon:
    Found(cachedTour(tourId)): tour =>
      Found(api.playerInfo(tour, userId.id)): player =>
        JsonOk:
          jsonView.playerInfoExtended(tour, player)

  def teamInfo(tourId: TourId, teamId: TeamId) = Open:
    Found(cachedTour(tourId)): tour =>
      Found(env.team.teamRepo mini teamId): team =>
        negotiate(
          FoundPage(api.teamBattleTeamInfo(tour, teamId)):
            views.html.tournament.teamBattle.teamInfo(tour, team, _)
          ,
          jsonView.teamInfo(tour, teamId) orNotFound JsonOk
        )

  private val JoinLimitPerUser = lila.memo.RateLimit[UserId](
    credits = 30,
    duration = 10 minutes,
    key = "tournament.user.join"
  )

  def join(id: TourId) = AuthBody(parse.json) { ctx ?=> me ?=>
    NoLameOrBot:
      NoPlayban:
        JoinLimitPerUser(me, rateLimited):
          val data = TournamentForm.TournamentJoin(
            password = ctx.body.body.\("p").asOpt[String],
            team = ctx.body.body.\("team").asOpt[TeamId]
          )
          doJoin(id, data).dmap(_.error) map {
            case None        => jsonOkResult
            case Some(error) => BadRequest(Json.obj("joined" -> false, "error" -> error))
          }
  }

  def apiJoin(id: TourId) = ScopedBody(_.Tournament.Write) { ctx ?=> me ?=>
    NoLameOrBot:
      NoPlayban:
        JoinLimitPerUser(me, rateLimited):
          val data = TournamentForm.joinForm
            .bindFromRequest()
            .fold(_ => TournamentForm.TournamentJoin(none, none), identity)
          doJoin(id, data) map {
            _.error.fold(jsonOkResult): error =>
              BadRequest(Json.obj("error" -> error))
          }
  }

  private def doJoin(tourId: TourId, data: TournamentForm.TournamentJoin)(using me: Me) =
    data.team
      .so { env.team.cached.isLeader(_, me) }
      .flatMap: isLeader =>
        api.joinWithResult(tourId, data = data, isLeader)

  def pause(id: TourId) = Auth { ctx ?=> me ?=>
    Found(cachedTour(id)): tour =>
      api.selfPause(tour.id, me)
      if HTTPRequest.isXhr(ctx.req) then jsonOkResult
      else Redirect(routes.Tournament.show(tour.id))
  }

  def apiWithdraw(id: TourId) = ScopedBody(_.Tournament.Write) { _ ?=> me ?=>
    Found(cachedTour(id)): tour =>
      api.selfPause(tour.id, me) inject jsonOkResult
  }

  def form = Auth { ctx ?=> me ?=>
    NoBot:
      env.team.api.lightsByLeader(me) flatMap { teams =>
        Ok.page(html.tournament.form.create(forms.create(teams), teams))
      }
  }

  def teamBattleForm(teamId: TeamId) = Auth { ctx ?=> me ?=>
    NoBot:
      env.team.api.lightsByLeader(me) flatMap { teams =>
        env.team.api.leads(teamId, me) elseNotFound
          Ok.page(html.tournament.form.create(forms.create(teams, teamId.some), Nil))
      }
  }

  private val CreateLimitPerUser = lila.memo.RateLimit[UserId](
    credits = 240,
    duration = 1.day,
    key = "tournament.user"
  )

  private val CreateLimitPerIP = env.security.ipTrust.rateLimit(800, 1.day, "tournament.ip")

  private[controllers] def rateLimitCreation(
      isPrivate: Boolean,
      fail: => Fu[Result]
  )(create: => Fu[Result])(using me: Me, req: RequestHeader): Fu[Result] =
    val cost =
      if isGranted(_.ManageTournament) then 2
      else if me.hasTitle ||
        env.streamer.liveStreamApi.isStreaming(me) ||
        me.isVerified ||
        isPrivate
      then 5
      else 20
    CreateLimitPerUser(me, fail, cost = cost):
      CreateLimitPerIP(req.ipAddress, fail, cost = cost, msg = me.username):
        create

  def webCreate = AuthBody(_ ?=> _ ?=> create)
  def apiCreate = ScopedBody(_.Tournament.Write)(_ ?=> _ ?=> create)

  private def create(using BodyContext[?])(using me: Me) = NoBot:
    def whenRateLimited = negotiate(Redirect(routes.Tournament.home), rateLimited)
    env.team.api
      .lightsByLeader(me)
      .flatMap: teams =>
        forms
          .create(teams)
          .bindFromRequest()
          .fold(
            err =>
              negotiate(
                BadRequest.page(html.tournament.form.create(err, teams)),
                doubleJsonFormError(err)
              ),
            setup =>
              rateLimitCreation(setup.isPrivate, whenRateLimited):
                api
                  .createTournament(setup, teams, andJoin = ctx.isWebAuth)
                  .flatMap: tour =>
                    given GetMyTeamIds = _ => fuccess(teams.map(_.id))
                    negotiate(
                      html = Redirect {
                        if tour.isTeamBattle then routes.Tournament.teamBattleEdit(tour.id)
                        else routes.Tournament.show(tour.id)
                      }.flashSuccess,
                      json = jsonView(
                        tour,
                        none,
                        env.team.getTeamName.apply,
                        none,
                        none,
                        partial = false,
                        withScores = false
                      ) map { Ok(_) }
                    )
          )

  def apiUpdate(id: TourId) = ScopedBody(_.Tournament.Write) { ctx ?=> me ?=>
    cachedTour(id).flatMap:
      _.filter(_.createdBy.is(me) || isGranted(_.ManageTournament)) so { tour =>
        env.team.api.lightsByLeader(me) flatMap { teams =>
          forms
            .edit(teams, tour)
            .bindFromRequest()
            .fold(
              jsonFormError,
              data =>
                given GetMyTeamIds = _ => fuccess(teams.map(_.id))
                api.apiUpdate(tour, data) flatMap { tour =>
                  jsonView(
                    tour,
                    none,
                    env.team.getTeamName.apply,
                    none,
                    none,
                    partial = false,
                    withScores = true
                  ) map { Ok(_) }
                }
            )
        }
      }
  }

  def apiTerminate(id: TourId) = ScopedBody(_.Tournament.Write) { _ ?=> me ?=>
    Found(cachedTour(id)):
      case tour if tour.createdBy.is(me) || isGranted(_.ManageTournament) =>
        api.kill(tour).inject(jsonOkResult)
      case _ => BadRequest(jsonError("Can't terminate that tournament: Permission denied"))
  }

  def teamBattleEdit(id: TourId) = Auth { ctx ?=> me ?=>
    Found(cachedTour(id)):
      case tour if tour.createdBy.is(me) || isGranted(_.ManageTournament) =>
        tour.teamBattle.so: battle =>
          env.team.teamRepo.byOrderedIds(battle.sortedTeamIds) flatMap { teams =>
            env.user.lightUserApi.preloadMany(teams.map(_.createdBy)) >> {
              val form = lila.tournament.TeamBattle.DataForm.edit(
                teams.map: t =>
                  s"""${t.id} "${t.name}" by ${env.user.lightUserApi
                      .sync(t.createdBy)
                      .fold(t.createdBy)(_.name)}""",
                battle.nbLeaders
              )
              Ok.page(html.tournament.teamBattle.edit(tour, form))
            }
          }
      case tour => Redirect(routes.Tournament.show(tour.id))
  }

  def teamBattleUpdate(id: TourId) = AuthBody { ctx ?=> me ?=>
    Found(cachedTour(id)):
      case tour if tour.createdBy.is(me) || isGranted(_.ManageTournament) && !tour.isFinished =>
        lila.tournament.TeamBattle.DataForm.empty
          .bindFromRequest()
          .fold(
            err => BadRequest.page(html.tournament.teamBattle.edit(tour, err)),
            res =>
              api.teamBattleUpdate(tour, res, env.team.api.filterExistingIds) inject
                Redirect(routes.Tournament.show(tour.id))
          )
      case tour => Redirect(routes.Tournament.show(tour.id))
  }

  def apiTeamBattleUpdate(id: TourId) = ScopedBody(_.Tournament.Write) { ctx ?=> me ?=>
    Found(cachedTour(id)):
      case tour if tour.createdBy.is(me) || isGranted(_.ManageTournament) && !tour.isFinished =>
        lila.tournament.TeamBattle.DataForm.empty
          .bindFromRequest()
          .fold(
            jsonFormError,
            res =>
              api.teamBattleUpdate(tour, res, env.team.api.filterExistingIds) >> {
                cachedTour(tour.id) map (_ | tour) flatMap { tour =>
                  jsonView(
                    tour,
                    none,
                    env.team.getTeamName.apply,
                    none,
                    none,
                    partial = false,
                    withScores = true
                  )
                } map { Ok(_) }
              }
          )
      case _ => BadRequest(jsonError("Can't update that tournament."))
  }

  def featured = Open:
    negotiateJson:
      WithMyPerfs:
        env.tournament.cached.onHomepage.getUnit.recoverDefault map {
          lila.tournament.Spotlight.select(_, 4)
        } flatMap env.tournament.apiJsonView.featured map { Ok(_) }

  def shields = Open:
    for
      history <- env.tournament.shieldApi.history(5.some)
      _       <- env.user.lightUserApi preloadMany history.userIds
      page    <- renderPage(html.tournament.shields(history))
    yield Ok(page)

  def categShields(k: String) = Open:
    FoundPage(env.tournament.shieldApi.byCategKey(k)): (categ, awards) =>
      env.user.lightUserApi preloadMany awards.map(_.owner) inject
        html.tournament.shields.byCateg(categ, awards)

  def calendar = Open:
    api.calendar.flatMap: tours =>
      Ok.page(html.tournament.calendar(env.tournament.apiJsonView calendar tours))

  def history(freq: String, page: Int) = Open:
    lila.tournament.Schedule.Freq.byName.get(freq) so { fr =>
      api.history(fr, page) flatMap { pager =>
        val userIds = pager.currentPageResults.flatMap(_.winnerId)
        env.user.lightUserApi.preloadMany(userIds) >>
          Ok.page(html.tournament.history(fr, pager))
      }
    }

  def edit(id: TourId) = Auth { ctx ?=> me ?=>
    WithEditableTournament(id): tour =>
      env.team.api.lightsByLeader(me) flatMap { teams =>
        val form = forms.edit(teams, tour)
        Ok.page(html.tournament.form.edit(tour, form, teams))
      }
  }

  def update(id: TourId) = AuthBody { ctx ?=> me ?=>
    WithEditableTournament(id): tour =>
      env.team.api.lightsByLeader(me) flatMap { teams =>
        forms
          .edit(teams, tour)
          .bindFromRequest()
          .fold(
            err => BadRequest.page(html.tournament.form.edit(tour, err, teams)),
            data => api.update(tour, data) inject Redirect(routes.Tournament.show(id)).flashSuccess
          )
      }
  }

  def terminate(id: TourId) = Auth { ctx ?=> me ?=>
    WithEditableTournament(id): tour =>
      api kill tour inject {
        env.mod.logApi.terminateTournament(tour.name())
        Redirect(routes.Tournament.home)
      }
  }

  def byTeam(id: TeamId) = Anon:
    apiC
      .jsonDownload:
        repo
          .byTeamCursor(id)
          .documentSource(getInt("max") | 100)
          .mapAsync(1)(env.tournament.apiJsonView.fullJson)
          .throttle(20, 1.second)

  def battleTeams(id: TourId) = Open:
    cachedTour(id).flatMap:
      _.filter(_.isTeamBattle) so { tour =>
        env.tournament.cached.battle.teamStanding.get(tour.id) flatMap { standing =>
          Ok.page(views.html.tournament.teamBattle.standing(tour, standing))
        }
      }

  private def WithEditableTournament(id: TourId)(
      f: Tour => Fu[Result]
  )(using ctx: Context, me: Me): Fu[Result] =
    Found(cachedTour(id)): t =>
      if (t.createdBy.is(me) && !t.isFinished) || isGranted(_.ManageTournament)
      then f(t)
      else Redirect(routes.Tournament.show(t.id))

  private val streamerCache = env.memo.cacheApi[TourId, List[UserId]](64, "tournament.streamers"):
    _.refreshAfterWrite(15.seconds)
      .maximumSize(256)
      .buildAsyncFuture: tourId =>
        repo.isUnfinished(tourId) flatMapz {
          env.streamer.liveStreamApi.all.flatMap {
            _.streams
              .map: stream =>
                env.tournament.hasUser(tourId, stream.streamer.userId).dmap(_ option stream.streamer.userId)
              .parallel
              .dmap(_.flatten)
          }
        }

  private given GetMyTeamIds = me => env.team.cached.teamIdsList(me.userId)
