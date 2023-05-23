package controllers

import play.api.libs.json.*
import play.api.mvc.*
import views.*

import lila.api.Context
import lila.app.{ given, * }
import lila.common.{ HTTPRequest, Preload }
import lila.common.Json.given
import lila.memo.CacheApi.*
import lila.tournament.{ Tournament as Tour, TournamentForm, VisibleTournaments, MyInfo }
import lila.user.{ User as UserModel }
import lila.gathering.Condition.GetUserTeamIds

final class Tournament(env: Env, apiC: => Api)(using mat: akka.stream.Materializer)
    extends LilaController(env):

  private def repo       = env.tournament.tournamentRepo
  private def api        = env.tournament.api
  private def jsonView   = env.tournament.jsonView
  private def forms      = env.tournament.forms
  private def cachedTour = env.tournament.cached.tourCache.byId

  private def tournamentNotFound(using Context) = NotFound(html.tournament.bits.notFound())

  private[controllers] val upcomingCache = env.memo.cacheApi.unit[(VisibleTournaments, List[Tour])] {
    _.refreshAfterWrite(3.seconds)
      .buildAsyncFuture { _ =>
        for {
          visible   <- api.fetchVisibleTournaments
          scheduled <- repo.allScheduledDedup
        } yield (visible, scheduled)
      }
  }

  def home     = Open(serveHome)
  def homeLang = LangPage(routes.Tournament.home)(serveHome)

  private def serveHome(using ctx: Context) = NoBot:
    for
      (visible, scheduled) <- upcomingCache.getUnit
      teamIds              <- ctx.userId.??(env.team.cached.teamIdsList)
      allTeamIds = (TeamId.from(env.featuredTeamsSetting.get().value) ++ teamIds).distinct
      teamVisible  <- repo.visibleForTeams(allTeamIds, 5 * 60)
      scheduleJson <- env.tournament.apiJsonView(visible add teamVisible)
      response <- negotiate(
        html = for
          finished <- api.notableFinished
          winners  <- env.tournament.winners.all
        yield {
          pageHit
          Ok(html.tournament.home(scheduled, finished, winners, scheduleJson)).noCache
        },
        api = _ => Ok(scheduleJson).toFuccess
      )
    yield response

  def help = Open:
    Ok(html.tournament.faq.page).toFuccess

  def leaderboard = Open:
    for
      winners <- env.tournament.winners.all
      _       <- env.user.lightUserApi preloadMany winners.userIds
    yield Ok(html.tournament.leaderboard(winners))

  private[controllers] def canHaveChat(tour: Tour, json: Option[JsObject])(using ctx: Context): Boolean =
    tour.hasChat && ctx.noKid && ctx.noBot && // no public chats for kids
      ctx.me.fold(!tour.isPrivate && HTTPRequest.isHuman(ctx.req)) {
        u => // anon can see public chats, except for private tournaments
          (!tour.isPrivate || json.fold(true)(jsonHasMe) || ctx.userId.has(tour.createdBy) || isGranted(
            _.ChatTimeout
          )) && // private tournament that I joined or has ChatTimeout
          (env.chat.panic.allowed(u) || isGranted(_.ChatTimeout))
      }

  private def jsonHasMe(js: JsObject): Boolean = (js \ "me").toOption.isDefined

  def show(id: TourId) = Open:
    val page = getInt("page")
    cachedTour(id) flatMap { tourOption =>
      def loadChat(tour: Tour, json: JsObject) =
        canHaveChat(tour, json.some) ?? env.chat.api.userChat.cached
          .findMine(ChatId(tour.id), ctx.me)
          .flatMap { c =>
            env.user.lightUserApi.preloadMany(c.chat.userIds) inject c.some
          }
      negotiate(
        html = tourOption
          .fold(tournamentNotFound.toFuccess) { tour =>
            for
              myInfo   <- ctx.me.?? { jsonView.fetchMyInfo(tour, _) }
              verdicts <- api.getVerdicts(tour, ctx.me, myInfo.isDefined)
              version  <- env.tournament.version(tour.id)
              json <- jsonView(
                tour = tour,
                page = page,
                me = ctx.me,
                getTeamName = env.team.getTeamName.apply,
                playerInfoExt = none,
                socketVersion = version.some,
                partial = false,
                withScores = true,
                myInfo = Preload[Option[MyInfo]](myInfo)
              ).map(jsonView.addReloadEndpoint(_, tour, env.tournament.lilaHttp.handles))
              chat <- loadChat(tour, json)
              _ <- tour.teamBattle ?? { b =>
                env.team.cached.preloadSet(b.teams)
              }
              streamers   <- streamerCache get tour.id
              shieldOwner <- env.tournament.shieldApi currentOwner tour
            yield {
              env.tournament.lilaHttp.hit(tour)
              Ok(html.tournament.show(tour, verdicts, json, chat, streamers, shieldOwner)).noCache
            }
          }
          .monSuccess(_.tournament.apiShowPartial(partial = false, HTTPRequest clientName ctx.req)),
        api = _ =>
          tourOption
            .fold(notFoundJson("No such tournament")) { tour =>
              for
                playerInfoExt <- getUserStr("playerInfo").map(_.id).?? { api.playerInfo(tour, _) }
                socketVersion <- getBool("socketVersion").??(env.tournament version tour.id dmap some)
                partial = getBool("partial")
                json <- jsonView(
                  tour = tour,
                  page = page,
                  me = ctx.me,
                  getTeamName = env.team.getTeamName.apply,
                  playerInfoExt = playerInfoExt,
                  socketVersion = socketVersion,
                  partial = partial,
                  withScores = getBoolOpt("scores") | true
                )
                chat <- !partial ?? loadChat(tour, json)
              yield Ok(json.add("chat" -> chat.map { c =>
                lila.chat.JsonView.mobile(chat = c.chat)
              })).noCache
            }
            .monSuccess(_.tournament.apiShowPartial(getBool("partial"), HTTPRequest clientName ctx.req))
      )
    }

  def standing(id: TourId, page: Int) = Open:
    OptionFuResult(cachedTour(id)): tour =>
      JsonOk:
        env.tournament.standingApi(tour, page, withScores = getBoolOpt("scores") | true)

  def pageOf(id: TourId, userId: UserStr) = Open:
    OptionFuResult(cachedTour(id)): tour =>
      api
        .pageOf(tour, userId.id)
        .flatMapz: page =>
          JsonOk:
            env.tournament.standingApi(tour, page, withScores = getBoolOpt("scores") | true)

  def player(tourId: TourId, userId: UserStr) = Anon:
    cachedTour(tourId).flatMapz: tour =>
      JsonOk:
        api.playerInfo(tour, userId.id) flatMapz {
          jsonView.playerInfoExtended(tour, _)
        }

  def teamInfo(tourId: TourId, teamId: TeamId) = Open:
    cachedTour(tourId).flatMapz: tour =>
      env.team.teamRepo mini teamId flatMapz { team =>
        if HTTPRequest.isXhr(ctx.req)
        then jsonView.teamInfo(tour, teamId) mapz JsonOk
        else
          api.teamBattleTeamInfo(tour, teamId) mapz { info =>
            Ok(views.html.tournament.teamBattle.teamInfo(tour, team, info))
          }
      }

  private val JoinLimitPerUser = lila.memo.RateLimit[UserId](
    credits = 30,
    duration = 10 minutes,
    key = "tournament.user.join"
  )

  def join(id: TourId) = AuthBody(parse.json) { ctx ?=> me =>
    NoLameOrBot:
      NoPlayban:
        JoinLimitPerUser(me.id, rateLimitedJson.toFuccess):
          val data = TournamentForm.TournamentJoin(
            password = ctx.body.body.\("p").asOpt[String],
            team = ctx.body.body.\("team").asOpt[TeamId]
          )
          doJoin(id, data, me).dmap(_.error) map {
            case None        => jsonOkResult
            case Some(error) => BadRequest(Json.obj("joined" -> false, "error" -> error))
          }
  }

  def apiJoin(id: TourId) = ScopedBody(_.Tournament.Write) { req ?=> me =>
    if me.lame || me.isBot
    then Unauthorized(Json.obj("error" -> "This user cannot join tournaments")).toFuccess
    else
      NoPlayban(me.id.some):
        JoinLimitPerUser(me.id, rateLimitedJson.toFuccess):
          val data = TournamentForm.joinForm
            .bindFromRequest()
            .fold(_ => TournamentForm.TournamentJoin(none, none), identity)
          doJoin(id, data, me) map {
            _.error.fold(jsonOkResult): error =>
              BadRequest(Json.obj("error" -> error))
          }
  }

  private def doJoin(tourId: TourId, data: TournamentForm.TournamentJoin, me: UserModel) =
    data.team
      .?? { env.team.cached.isLeader(_, me.id) }
      .flatMap { isLeader =>
        api.joinWithResult(tourId, me, data = data, isLeader)
      }

  def pause(id: TourId) = Auth { ctx ?=> me =>
    OptionResult(cachedTour(id)) { tour =>
      api.selfPause(tour.id, me.id)
      if (HTTPRequest.isXhr(ctx.req)) jsonOkResult
      else Redirect(routes.Tournament.show(tour.id))
    }
  }

  def apiWithdraw(id: TourId) = ScopedBody(_.Tournament.Write) { _ ?=> me =>
    cachedTour(id) flatMapz { tour =>
      api.selfPause(tour.id, me.id) inject jsonOkResult
    }
  }

  def form = Auth { ctx ?=> me =>
    NoBot {
      env.team.api.lightsByLeader(me.id) map { teams =>
        Ok(html.tournament.form.create(forms.create(me, teams), teams))
      }
    }
  }

  def teamBattleForm(teamId: TeamId) = Auth { ctx ?=> me =>
    NoBot {
      env.team.api.lightsByLeader(me.id) flatMap { teams =>
        env.team.api.leads(teamId, me.id) mapz {
          Ok(html.tournament.form.create(forms.create(me, teams, teamId.some), Nil))
        }
      }
    }
  }

  private val CreateLimitPerUser = lila.memo.RateLimit[UserId](
    credits = 240,
    duration = 24.hour,
    key = "tournament.user"
  )

  private val CreateLimitPerIP = lila.memo.RateLimit[lila.common.IpAddress](
    credits = 400,
    duration = 24.hour,
    key = "tournament.ip"
  )

  private[controllers] def rateLimitCreation(
      me: UserModel,
      isPrivate: Boolean,
      req: RequestHeader,
      fail: => Result
  )(create: => Fu[Result]): Fu[Result] =
    val cost =
      if (isGranted(_.ManageTournament, me)) 2
      else if (
        me.hasTitle ||
        env.streamer.liveStreamApi.isStreaming(me.id) ||
        me.isVerified ||
        isPrivate
      ) 5
      else 20
    CreateLimitPerUser(me.id, fail.toFuccess, cost = cost):
      CreateLimitPerIP(req.ipAddress, fail.toFuccess, cost = cost, msg = me.username):
        create

  def create = AuthBody { ctx ?=> me =>
    NoBot:
      env.team.api
        .lightsByLeader(me.id)
        .flatMap: teams =>
          negotiate(
            html = forms
              .create(me, teams)
              .bindFromRequest()
              .fold(
                err => BadRequest(html.tournament.form.create(err, teams)).toFuccess,
                setup =>
                  rateLimitCreation(me, setup.isPrivate, ctx.req, Redirect(routes.Tournament.home)):
                    api
                      .createTournament(setup, me, teams)
                      .map: tour =>
                        Redirect {
                          if tour.isTeamBattle then routes.Tournament.teamBattleEdit(tour.id)
                          else routes.Tournament.show(tour.id)
                        }.flashSuccess
              ),
            api = _ => doApiCreate(me)
          )
  }

  def apiCreate = ScopedBody(_.Tournament.Write) { req ?=> me =>
    if me.isBot || me.lame
    then notFoundJson("This account cannot create tournaments")
    else doApiCreate(me)
  }

  private def doApiCreate(me: UserModel)(using req: Request[?]): Fu[Result] =
    env.team.api.lightsByLeader(me.id) flatMap { teams =>
      forms
        .create(me, teams)
        .bindFromRequest()
        .fold(
          jsonFormErrorDefaultLang,
          setup =>
            rateLimitCreation(me, setup.isPrivate, req, rateLimited) {
              env.team.api.lightsByLeader(me.id) flatMap { teams =>
                api.createTournament(setup, me, teams, andJoin = false) flatMap { tour =>
                  jsonView(
                    tour,
                    none,
                    none,
                    env.team.getTeamName.apply,
                    none,
                    none,
                    partial = false,
                    withScores = false
                  )(using reqLang, _ => fuccess(teams.map(_.id))) map { Ok(_) }
                }
              }
            }
        )
    }

  def apiUpdate(id: TourId) = ScopedBody(_.Tournament.Write) { req ?=> me =>
    given play.api.i18n.Lang = reqLang
    cachedTour(id) flatMap {
      _.filter(_.createdBy == me.id || isGranted(_.ManageTournament, me)) ?? { tour =>
        env.team.api.lightsByLeader(me.id) flatMap { teams =>
          forms
            .edit(me, teams, tour)
            .bindFromRequest()
            .fold(
              newJsonFormError,
              data =>
                api.apiUpdate(tour, data) flatMap { tour =>
                  jsonView(
                    tour,
                    none,
                    none,
                    env.team.getTeamName.apply,
                    none,
                    none,
                    partial = false,
                    withScores = true
                  )(using reqLang, _ => fuccess(teams.map(_.id))) map { Ok(_) }
                }
            )
        }
      }
    }
  }

  def apiTerminate(id: TourId) = ScopedBody(_.Tournament.Write) { _ ?=> me =>
    cachedTour(id) flatMapz {
      case tour if tour.createdBy == me.id || isGranted(_.ManageTournament, me) =>
        api.kill(tour).inject(jsonOkResult)
      case _ => BadRequest(jsonError("Can't terminate that tournament: Permission denied")).toFuccess
    }
  }

  def teamBattleEdit(id: TourId) = Auth { ctx ?=> me =>
    cachedTour(id) flatMapz {
      case tour if tour.createdBy == me.id || isGranted(_.ManageTournament) =>
        tour.teamBattle ?? { battle =>
          env.team.teamRepo.byOrderedIds(battle.sortedTeamIds) flatMap { teams =>
            env.user.lightUserApi.preloadMany(teams.map(_.createdBy)) >> {
              val form = lila.tournament.TeamBattle.DataForm.edit(
                teams.map { t =>
                  s"""${t.id} "${t.name}" by ${env.user.lightUserApi
                      .sync(t.createdBy)
                      .fold(t.createdBy)(_.name)}"""
                },
                battle.nbLeaders
              )
              Ok(html.tournament.teamBattle.edit(tour, form)).toFuccess
            }
          }
        }
      case tour => Redirect(routes.Tournament.show(tour.id)).toFuccess
    }
  }

  def teamBattleUpdate(id: TourId) = AuthBody { ctx ?=> me =>
    cachedTour(id).flatMapz:
      case tour if (tour.createdBy == me.id || isGranted(_.ManageTournament)) && !tour.isFinished =>
        given play.api.mvc.Request[?] = ctx.body
        lila.tournament.TeamBattle.DataForm.empty
          .bindFromRequest()
          .fold(
            err => BadRequest(html.tournament.teamBattle.edit(tour, err)).toFuccess,
            res =>
              api.teamBattleUpdate(tour, res, env.team.api.filterExistingIds) inject
                Redirect(routes.Tournament.show(tour.id))
          )
      case tour => Redirect(routes.Tournament.show(tour.id)).toFuccess
  }

  def apiTeamBattleUpdate(id: TourId) = ScopedBody(_.Tournament.Write) { req ?=> me =>
    given play.api.i18n.Lang = reqLang
    cachedTour(id) flatMapz {
      case tour if (tour.createdBy == me.id || isGranted(_.ManageTournament, me)) && !tour.isFinished =>
        lila.tournament.TeamBattle.DataForm.empty
          .bindFromRequest()
          .fold(
            newJsonFormError,
            res =>
              api.teamBattleUpdate(tour, res, env.team.api.filterExistingIds) >> {
                cachedTour(tour.id) map (_ | tour) flatMap { tour =>
                  jsonView(
                    tour,
                    none,
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
      case _ => BadRequest(jsonError("Can't update that tournament.")).toFuccess
    }
  }

  def featured = Open:
    negotiate(
      html = notFound,
      api = _ =>
        env.tournament.cached.onHomepage.getUnit.recoverDefault map {
          lila.tournament.Spotlight.select(_, ctx.me, 4)
        } flatMap env.tournament.apiJsonView.featured map { Ok(_) }
    )

  def shields = Open:
    for
      history <- env.tournament.shieldApi.history(5.some)
      _       <- env.user.lightUserApi preloadMany history.userIds
    yield html.tournament.shields(history)

  def categShields(k: String) = Open:
    OptionFuOk(env.tournament.shieldApi.byCategKey(k)) { (categ, awards) =>
      env.user.lightUserApi preloadMany awards.map(_.owner) inject
        html.tournament.shields.byCateg(categ, awards)
    }

  def calendar = Open:
    api.calendar map { tours =>
      Ok(html.tournament.calendar(env.tournament.apiJsonView calendar tours))
    }

  def history(freq: String, page: Int) = Open:
    lila.tournament.Schedule.Freq(freq) ?? { fr =>
      api.history(fr, page) flatMap { pager =>
        env.user.lightUserApi preloadMany pager.currentPageResults.flatMap(_.winnerId) inject
          Ok(html.tournament.history(fr, pager))
      }
    }

  def edit(id: TourId) = Auth { ctx ?=> me =>
    WithEditableTournament(id, me) { tour =>
      env.team.api.lightsByLeader(me.id) map { teams =>
        val form = forms.edit(me, teams, tour)
        Ok(html.tournament.form.edit(tour, form, teams))
      }
    }
  }

  def update(id: TourId) = AuthBody { ctx ?=> me =>
    WithEditableTournament(id, me): tour =>
      env.team.api.lightsByLeader(me.id) flatMap { teams =>
        forms
          .edit(me, teams, tour)
          .bindFromRequest()
          .fold(
            err => BadRequest(html.tournament.form.edit(tour, err, teams)).toFuccess,
            data => api.update(tour, data) inject Redirect(routes.Tournament.show(id)).flashSuccess
          )
      }
  }

  def terminate(id: TourId) = Auth { ctx ?=> me =>
    WithEditableTournament(id, me) { tour =>
      api kill tour inject {
        env.mod.logApi.terminateTournament(me.id into ModId, tour.name())
        Redirect(routes.Tournament.home)
      }
    }
  }

  def byTeam(id: TeamId) = Anon:
    given play.api.i18n.Lang = reqLang
    apiC.jsonDownload {
      repo
        .byTeamCursor(id)
        .documentSource(getInt("max", req) | 100)
        .mapAsync(1)(env.tournament.apiJsonView.fullJson)
        .throttle(20, 1.second)
    }.toFuccess

  def battleTeams(id: TourId) = Open:
    cachedTour(id).flatMap:
      _.filter(_.isTeamBattle) ?? { tour =>
        env.tournament.cached.battle.teamStanding.get(tour.id) map { standing =>
          Ok(views.html.tournament.teamBattle.standing(tour, standing))
        }
      }

  private def WithEditableTournament(id: TourId, me: UserModel)(
      f: Tour => Fu[Result]
  )(using Context): Fu[Result] =
    cachedTour(id) flatMap {
      case Some(t) if (t.createdBy == me.id && !t.isFinished) || isGranted(_.ManageTournament) =>
        f(t)
      case Some(t) => Redirect(routes.Tournament.show(t.id)).toFuccess
      case _       => notFound
    }

  private val streamerCache = env.memo.cacheApi[TourId, List[UserId]](64, "tournament.streamers") {
    _.refreshAfterWrite(15.seconds)
      .maximumSize(256)
      .buildAsyncFuture { tourId =>
        repo.isUnfinished(tourId) flatMapz {
          env.streamer.liveStreamApi.all.flatMap {
            _.streams
              .map { stream =>
                env.tournament.hasUser(tourId, stream.streamer.userId).dmap(_ option stream.streamer.userId)
              }
              .parallel
              .dmap(_.flatten)
          }
        }
      }
  }

  private given GetUserTeamIds = user => env.team.cached.teamIdsList(user.id)
