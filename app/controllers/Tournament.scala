package controllers

import play.api.libs.json._
import play.api.mvc._
import scala.annotation.nowarn
import scala.concurrent.duration._
import views._

import lila.api.Context
import lila.app._
import lila.common.{ HTTPRequest, Preload }
import lila.hub.LightTeam._
import lila.memo.CacheApi._
import lila.tournament.{ Tournament => Tour, TournamentForm, VisibleTournaments }
import lila.user.{ User => UserModel }

final class Tournament(
    env: Env,
    apiC: => Api
)(implicit
    mat: akka.stream.Materializer
) extends LilaController(env) {

  private def repo       = env.tournament.tournamentRepo
  private def api        = env.tournament.api
  private def jsonView   = env.tournament.jsonView
  private def forms      = env.tournament.forms
  private def cachedTour = env.tournament.cached.tourCache byId _

  private def tournamentNotFound(implicit ctx: Context) = NotFound(html.tournament.bits.notFound())

  private[controllers] val upcomingCache = env.memo.cacheApi.unit[(VisibleTournaments, List[Tour])] {
    _.refreshAfterWrite(3.seconds)
      .buildAsyncFuture { _ =>
        for {
          visible   <- api.fetchVisibleTournaments
          scheduled <- repo.allScheduledDedup
        } yield (visible, scheduled)
      }
  }

  def home =
    Open { implicit ctx =>
      for {
        (visible, scheduled) <- upcomingCache.getUnit
        teamIds              <- ctx.userId.??(env.team.cached.teamIdsList)
        allTeamIds = (env.featuredTeamsSetting.get().value ++ teamIds).distinct
        teamVisible  <- repo.visibleForTeams(allTeamIds, 5 * 60)
        scheduleJson <- env.tournament.apiJsonView(visible add teamVisible)
        response <- negotiate(
          html = for {
            finished <- api.notableFinished
            winners  <- env.tournament.winners.all
            teamIds  <- ctx.userId.??(env.team.cached.teamIdsList)
          } yield NoCache {
            pageHit
            Ok(html.tournament.home(scheduled, finished, winners, scheduleJson))
          },
          api = _ => Ok(scheduleJson).fuccess
        )
      } yield response
    }

  def help(@nowarn("cat=unused") sysStr: Option[String]) =
    Open { implicit ctx =>
      Ok(html.tournament.faq.page).fuccess
    }

  def leaderboard =
    Open { implicit ctx =>
      for {
        winners <- env.tournament.winners.all
        _       <- env.user.lightUserApi preloadMany winners.userIds
      } yield Ok(html.tournament.leaderboard(winners))
    }

  private[controllers] def canHaveChat(tour: Tour, json: Option[JsObject])(implicit ctx: Context): Boolean =
    tour.hasChat && ctx.noKid && ctx.noBot && // no public chats for kids
      ctx.me.fold(!tour.isPrivate && HTTPRequest.isHuman(ctx.req)) {
        u => // anon can see public chats, except for private tournaments
          (!tour.isPrivate || json.fold(true)(jsonHasMe) || ctx.userId.has(tour.createdBy) || isGranted(
            _.ChatTimeout
          )) && // private tournament that I joined or has ChatTimeout
          (env.chat.panic.allowed(u) || isGranted(_.ChatTimeout))
      }

  private def jsonHasMe(js: JsObject): Boolean = (js \ "me").toOption.isDefined

  def show(id: String) =
    Open { implicit ctx =>
      val page = getInt("page")
      cachedTour(id) flatMap { tourOption =>
        def loadChat(tour: Tour, json: JsObject) =
          canHaveChat(tour, json.some) ?? env.chat.api.userChat.cached
            .findMine(lila.chat.Chat.Id(tour.id), ctx.me)
            .flatMap { c =>
              env.user.lightUserApi.preloadMany(c.chat.userIds) inject c.some
            }
        negotiate(
          html = tourOption
            .fold(tournamentNotFound.fuccess) { tour =>
              for {
                myInfo   <- ctx.me.?? { jsonView.fetchMyInfo(tour, _) }
                verdicts <- api.getVerdicts(tour, ctx.me, getUserTeamIds, myInfo.isDefined)
                version  <- env.tournament.version(tour.id)
                json <- jsonView(
                  tour = tour,
                  page = page,
                  me = ctx.me,
                  getUserTeamIds = getUserTeamIds,
                  getTeamName = env.team.getTeamName,
                  playerInfoExt = none,
                  socketVersion = version.some,
                  partial = false,
                  withScores = true,
                  myInfo = Preload(myInfo)
                ).map(jsonView.addReloadEndpoint(_, tour, env.tournament.lilaHttp.handles))
                chat <- loadChat(tour, json)
                _ <- tour.teamBattle ?? { b =>
                  env.team.cached.preloadSet(b.teams)
                }
                streamers   <- streamerCache get tour.id
                shieldOwner <- env.tournament.shieldApi currentOwner tour
              } yield {
                env.tournament.lilaHttp.hit(tour)
                Ok(html.tournament.show(tour, verdicts, json, chat, streamers, shieldOwner))
              }
            }
            .monSuccess(_.tournament.apiShowPartial(partial = false, HTTPRequest clientName ctx.req)),
          api = _ =>
            tourOption
              .fold(notFoundJson("No such tournament")) { tour =>
                for {
                  playerInfoExt <- get("playerInfo").?? { api.playerInfo(tour, _) }
                  socketVersion <- getBool("socketVersion").??(env.tournament version tour.id dmap some)
                  partial = getBool("partial")
                  json <- jsonView(
                    tour = tour,
                    page = page,
                    me = ctx.me,
                    getUserTeamIds = getUserTeamIds,
                    getTeamName = env.team.getTeamName,
                    playerInfoExt = playerInfoExt,
                    socketVersion = socketVersion,
                    partial = partial,
                    withScores = getBoolOpt("scores") | true
                  )
                  chat <- !partial ?? loadChat(tour, json)
                } yield Ok(json.add("chat" -> chat.map { c =>
                  lila.chat.JsonView.mobile(chat = c.chat)
                }))
              }
              .monSuccess(_.tournament.apiShowPartial(getBool("partial"), HTTPRequest clientName ctx.req))
        ) dmap NoCache
      }
    }

  def standing(id: String, page: Int) =
    Open { implicit ctx =>
      OptionFuResult(cachedTour(id)) { tour =>
        JsonOk {
          env.tournament.standingApi(tour, page, withScores = getBoolOpt("scores") | true)
        }
      }
    }

  def pageOf(id: String, userId: String) =
    Open { implicit ctx =>
      OptionFuResult(cachedTour(id)) { tour =>
        api.pageOf(tour, UserModel normalize userId) flatMap {
          _ ?? { page =>
            JsonOk {
              env.tournament.standingApi(tour, page, withScores = getBoolOpt("scores") | true)
            }
          }
        }
      }
    }

  def player(tourId: String, userId: String) =
    Action.async {
      cachedTour(tourId) flatMap {
        _ ?? { tour =>
          JsonOk {
            api.playerInfo(tour, userId) flatMap {
              _ ?? { jsonView.playerInfoExtended(tour, _) }
            }
          }
        }
      }
    }

  def teamInfo(tourId: String, teamId: TeamID) =
    Open { implicit ctx =>
      cachedTour(tourId) flatMap {
        _ ?? { tour =>
          env.team.teamRepo mini teamId flatMap {
            _ ?? { team =>
              if (HTTPRequest isXhr ctx.req)
                jsonView.teamInfo(tour, teamId) map { _ ?? JsonOk }
              else
                api.teamBattleTeamInfo(tour, teamId) map {
                  _ ?? { info =>
                    Ok(views.html.tournament.teamBattle.teamInfo(tour, team, info))
                  }
                }
            }
          }
        }
      }
    }

  private val JoinLimitPerUser = new lila.memo.RateLimit[UserModel.ID](
    credits = 30,
    duration = 10 minutes,
    key = "tournament.user.join"
  )

  def join(id: String) =
    AuthBody(parse.json) { implicit ctx => me =>
      NoLameOrBot {
        NoPlayban {
          JoinLimitPerUser(me.id) {
            val data = TournamentForm.TournamentJoin(
              password = ctx.body.body.\("p").asOpt[String],
              team = ctx.body.body.\("team").asOpt[String]
            )
            doJoin(id, data, me).dmap(_.error) map {
              case None        => jsonOkResult
              case Some(error) => BadRequest(Json.obj("joined" -> false, "error" -> error))
            }
          }(rateLimitedJson.fuccess)
        }
      }
    }

  def apiJoin(id: String) =
    ScopedBody(_.Tournament.Write) { implicit req => me =>
      if (me.lame || me.isBot) Unauthorized(Json.obj("error" -> "This user cannot join tournaments")).fuccess
      else
        JoinLimitPerUser(me.id) {
          val data = TournamentForm.joinForm
            .bindFromRequest()
            .fold(_ => TournamentForm.TournamentJoin(none, none), identity)
          doJoin(id, data, me) map { result =>
            result.error match {
              case None        => jsonOkResult
              case Some(error) => BadRequest(Json.obj("error" -> error))
            }
          }
        }(rateLimitedJson.fuccess)
    }

  private def doJoin(tourId: Tour.ID, data: TournamentForm.TournamentJoin, me: UserModel) =
    data.team
      .?? { env.team.cached.isLeader(_, me.id) }
      .flatMap { isLeader =>
        api.joinWithResult(
          tourId,
          me,
          password = data.password,
          teamId = data.team,
          getUserTeamIds,
          isLeader
        )
      }

  def pause(id: String) =
    Auth { implicit ctx => me =>
      OptionResult(cachedTour(id)) { tour =>
        api.selfPause(tour.id, me.id)
        if (HTTPRequest.isXhr(ctx.req)) jsonOkResult
        else Redirect(routes.Tournament.show(tour.id))
      }
    }

  def apiWithdraw(id: String) =
    ScopedBody(_.Tournament.Write) { _ => me =>
      cachedTour(id) flatMap {
        _ ?? { tour =>
          api.selfPause(tour.id, me.id) inject jsonOkResult
        }
      }
    }

  def form =
    Auth { implicit ctx => me =>
      NoLameOrBot {
        env.team.api.lightsByLeader(me.id) map { teams =>
          Ok(html.tournament.form.create(forms.create(me, teams), teams))
        }
      }
    }

  def teamBattleForm(teamId: TeamID) =
    Auth { implicit ctx => me =>
      NoLameOrBot {
        env.team.api.lightsByLeader(me.id) flatMap { teams =>
          env.team.api.leads(teamId, me.id) map {
            _ ?? {
              Ok(html.tournament.form.create(forms.create(me, teams, teamId.some), Nil))
            }
          }
        }
      }
    }

  private val CreateLimitPerUser = new lila.memo.RateLimit[UserModel.ID](
    credits = 240,
    duration = 24.hour,
    key = "tournament.user"
  )

  private val CreateLimitPerIP = new lila.memo.RateLimit[lila.common.IpAddress](
    credits = 400,
    duration = 24.hour,
    key = "tournament.ip"
  )

  private[controllers] def rateLimitCreation(
      me: UserModel,
      isPrivate: Boolean,
      req: RequestHeader,
      fail: => Result
  )(
      create: => Fu[Result]
  ): Fu[Result] = {
    val cost =
      if (isGranted(_.ManageTournament, me)) 2
      else if (
        me.hasTitle ||
        env.streamer.liveStreamApi.isStreaming(me.id) ||
        me.isVerified ||
        isPrivate
      ) 5
      else 20
    CreateLimitPerUser(me.id, cost = cost) {
      CreateLimitPerIP(HTTPRequest ipAddress req, cost = cost, msg = me.username) {
        create
      }(fail.fuccess)
    }(fail.fuccess)
  }

  def create =
    AuthBody { implicit ctx => me =>
      NoLameOrBot {
        env.team.api.lightsByLeader(me.id) flatMap { teams =>
          implicit val req = ctx.body
          negotiate(
            html = forms
              .create(me, teams)
              .bindFromRequest()
              .fold(
                err => BadRequest(html.tournament.form.create(err, teams)).fuccess,
                setup =>
                  rateLimitCreation(me, setup.isPrivate, ctx.req, Redirect(routes.Tournament.home)) {
                    api.createTournament(setup, me, teams) map { tour =>
                      Redirect {
                        if (tour.isTeamBattle) routes.Tournament.teamBattleEdit(tour.id)
                        else routes.Tournament.show(tour.id)
                      }.flashSuccess
                    }
                  }
              ),
            api = _ => doApiCreate(me)
          )
        }
      }
    }

  def apiCreate =
    ScopedBody(_.Tournament.Write) { implicit req => me =>
      if (me.isBot || me.lame) notFoundJson("This account cannot create tournaments")
      else doApiCreate(me)
    }

  private def doApiCreate(me: UserModel)(implicit req: Request[_]): Fu[Result] =
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
                    getUserTeamIds = _ => fuccess(teams.map(_.id)),
                    env.team.getTeamName,
                    none,
                    none,
                    partial = false,
                    withScores = false
                  )(reqLang) map { Ok(_) }
                }
              }
            }
        )
    }

  def apiUpdate(id: String) =
    ScopedBody(_.Tournament.Write) { implicit req => me =>
      implicit def lang = reqLang
      cachedTour(id) flatMap {
        _.filter(_.createdBy == me.id || isGranted(_.ManageTournament, me)) ?? { tour =>
          env.team.api.lightsByLeader(me.id) flatMap { teams =>
            forms
              .edit(me, teams, tour)
              .bindFromRequest()
              .fold(
                newJsonFormError,
                data =>
                  api.apiUpdate(tour, data, teams) flatMap { tour =>
                    jsonView(
                      tour,
                      none,
                      none,
                      getUserTeamIds = _ => fuccess(teams.map(_.id)),
                      env.team.getTeamName,
                      none,
                      none,
                      partial = false,
                      withScores = true
                    )(reqLang) map { Ok(_) }
                  }
              )
          }
        }
      }
    }

  def apiTerminate(id: String) =
    ScopedBody(_.Tournament.Write) { implicit req => me =>
      cachedTour(id) flatMap {
        _ ?? {
          case tour if tour.createdBy == me.id || isGranted(_.ManageTournament, me) =>
            api
              .kill(tour)
              .map(_ => jsonOkResult)
          case _ => BadRequest(jsonError("Can't terminate that tournament: Permission denied")).fuccess
        }
      }
    }

  def teamBattleEdit(id: String) =
    Auth { implicit ctx => me =>
      cachedTour(id) flatMap {
        _ ?? {
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
                  Ok(html.tournament.teamBattle.edit(tour, form)).fuccess
                }
              }
            }
          case tour => Redirect(routes.Tournament.show(tour.id)).fuccess
        }
      }
    }

  def teamBattleUpdate(id: String) =
    AuthBody { implicit ctx => me =>
      cachedTour(id) flatMap {
        _ ?? {
          case tour if (tour.createdBy == me.id || isGranted(_.ManageTournament)) && !tour.isFinished =>
            implicit val req = ctx.body
            lila.tournament.TeamBattle.DataForm.empty
              .bindFromRequest()
              .fold(
                err => BadRequest(html.tournament.teamBattle.edit(tour, err)).fuccess,
                res =>
                  api.teamBattleUpdate(tour, res, env.team.api.filterExistingIds) inject
                    Redirect(routes.Tournament.show(tour.id))
              )
          case tour => Redirect(routes.Tournament.show(tour.id)).fuccess
        }
      }
    }

  def apiTeamBattleUpdate(id: String) =
    ScopedBody(_.Tournament.Write) { implicit req => me =>
      implicit def lang = reqLang
      cachedTour(id) flatMap {
        _ ?? {
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
                        getUserTeamIds = getUserTeamIds,
                        env.team.getTeamName,
                        none,
                        none,
                        partial = false,
                        withScores = true
                      )
                    } map { Ok(_) }
                  }
              )
          case _ => BadRequest(jsonError("Can't update that tournament.")).fuccess
        }
      }
    }

  def featured =
    Open { implicit ctx =>
      negotiate(
        html = notFound,
        api = _ =>
          env.tournament.cached.onHomepage.getUnit.recoverDefault map {
            lila.tournament.Spotlight.select(_, ctx.me, 4)
          } flatMap env.tournament.apiJsonView.featured map { Ok(_) }
      )
    }

  def shields =
    Open { implicit ctx =>
      for {
        history <- env.tournament.shieldApi.history(5.some)
        _       <- env.user.lightUserApi preloadMany history.userIds
      } yield html.tournament.shields(history)
    }

  def categShields(k: String) =
    Open { implicit ctx =>
      OptionFuOk(env.tournament.shieldApi.byCategKey(k)) { case (categ, awards) =>
        env.user.lightUserApi preloadMany awards.map(_.owner.value) inject
          html.tournament.shields.byCateg(categ, awards)
      }
    }

  def calendar =
    Open { implicit ctx =>
      api.calendar map { tours =>
        Ok(html.tournament.calendar(env.tournament.apiJsonView calendar tours))
      }
    }

  def history(freq: String, page: Int) =
    Open { implicit ctx =>
      lila.tournament.Schedule.Freq(freq) ?? { fr =>
        api.history(fr, page) flatMap { pager =>
          env.user.lightUserApi preloadMany pager.currentPageResults.flatMap(_.winnerId) inject
            Ok(html.tournament.history(fr, pager))
        }
      }
    }

  def edit(id: String) =
    Auth { implicit ctx => me =>
      WithEditableTournament(id, me) { tour =>
        env.team.api.lightsByLeader(me.id) map { teams =>
          val form = forms.edit(me, teams, tour)
          Ok(html.tournament.form.edit(tour, form, teams))
        }
      }
    }

  def update(id: String) =
    AuthBody { implicit ctx => me =>
      WithEditableTournament(id, me) { tour =>
        implicit val req = ctx.body
        env.team.api.lightsByLeader(me.id) flatMap { teams =>
          forms
            .edit(me, teams, tour)
            .bindFromRequest()
            .fold(
              err => BadRequest(html.tournament.form.edit(tour, err, teams)).fuccess,
              data => api.update(tour, data, teams) inject Redirect(routes.Tournament.show(id)).flashSuccess
            )
        }
      }
    }

  def terminate(id: String) =
    Auth { implicit ctx => me =>
      WithEditableTournament(id, me) { tour =>
        api kill tour inject {
          env.mod.logApi.terminateTournament(me.id, tour.name())
          Redirect(routes.Tournament.home)
        }
      }
    }

  def byTeam(id: String) =
    Action.async { implicit req =>
      implicit val lang = reqLang
      apiC.jsonStream {
        repo
          .byTeamCursor(id)
          .documentSource(getInt("max", req) | 100)
          .mapAsync(1)(env.tournament.apiJsonView.fullJson)
          .throttle(20, 1.second)
      }.fuccess
    }

  def battleTeams(id: String) =
    Open { implicit ctx =>
      cachedTour(id) flatMap {
        _.filter(_.isTeamBattle) ?? { tour =>
          env.tournament.cached.battle.teamStanding.get(tour.id) map { standing =>
            Ok(views.html.tournament.teamBattle.standing(tour, standing))
          }
        }
      }
    }

  private def WithEditableTournament(id: String, me: UserModel)(
      f: Tour => Fu[Result]
  )(implicit ctx: Context): Fu[Result] =
    cachedTour(id) flatMap {
      case Some(t) if (t.createdBy == me.id && !t.isFinished) || isGranted(_.ManageTournament) =>
        f(t)
      case Some(t) => Redirect(routes.Tournament.show(t.id)).fuccess
      case _       => notFound
    }

  private val streamerCache = env.memo.cacheApi[Tour.ID, List[UserModel.ID]](64, "tournament.streamers") {
    _.refreshAfterWrite(15.seconds)
      .maximumSize(256)
      .buildAsyncFuture { tourId =>
        repo.isUnfinished(tourId) flatMap {
          _ ?? {
            env.streamer.liveStreamApi.all.flatMap {
              _.streams
                .map { stream =>
                  env.tournament.hasUser(tourId, stream.streamer.userId).dmap(_ option stream.streamer.userId)
                }
                .sequenceFu
                .dmap(_.flatten)
            }
          }
        }
      }
  }

  private def getUserTeamIds(user: UserModel): Fu[List[TeamID]] =
    env.team.cached.teamIdsList(user.id)
}
