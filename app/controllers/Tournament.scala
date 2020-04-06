package controllers

import com.github.ghik.silencer.silent
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.api.Context
import lila.app._
import lila.chat.Chat
import lila.common.config.MaxPerPage
import lila.common.HTTPRequest
import lila.hub.LightTeam._
import lila.memo.CacheApi._
import lila.tournament.{ VisibleTournaments, Tournament => Tour }
import lila.user.{ User => UserModel }
import views._

final class Tournament(
    env: Env,
    teamC: => Team
) extends LilaController(env) {

  private def repo     = env.tournament.tournamentRepo
  private def api      = env.tournament.api
  private def jsonView = env.tournament.jsonView
  private def forms    = env.tournament.forms

  private def tournamentNotFound(implicit ctx: Context) = NotFound(html.tournament.bits.notFound())

  private[controllers] val upcomingCache = env.memo.cacheApi.unit[(VisibleTournaments, List[Tour])] {
    _.refreshAfterWrite(3 seconds)
      .buildAsyncFuture { _ =>
        for {
          visible   <- api.fetchVisibleTournaments
          scheduled <- repo.scheduledDedup
        } yield (visible, scheduled)
      }
  }

  def home(page: Int) = Open { implicit ctx =>
    negotiate(
      html = Reasonable(page, 20) {
        pageHit
        val finishedPaginator = repo.finishedPaginator(MaxPerPage(15), page = page)
        if (HTTPRequest isXhr ctx.req) for {
          pag <- finishedPaginator
          _   <- env.user.lightUserApi preloadMany pag.currentPageResults.flatMap(_.winnerId)
        } yield Ok(html.tournament.finishedPaginator(pag))
        else
          for {
            (visible, scheduled) <- upcomingCache.getUnit
            finished             <- finishedPaginator
            winners              <- env.tournament.winners.all
            _ <- env.user.lightUserApi preloadMany {
              finished.currentPageResults.flatMap(_.winnerId).toList :::
                scheduled.flatMap(_.winnerId) ::: winners.userIds
            }
            scheduleJson <- env.tournament apiJsonView visible
          } yield NoCache {
            Ok(html.tournament.home(scheduled, finished, winners, scheduleJson))
          }
      },
      api = _ =>
        for {
          (visible, _) <- upcomingCache.getUnit
          scheduleJson <- env.tournament apiJsonView visible
        } yield Ok(scheduleJson)
    )
  }

  def help(@silent sysStr: Option[String]) = Open { implicit ctx =>
    Ok(html.tournament.faq.page).fuccess
  }

  def leaderboard = Open { implicit ctx =>
    for {
      winners <- env.tournament.winners.all
      _       <- env.user.lightUserApi preloadMany winners.userIds
    } yield Ok(html.tournament.leaderboard(winners))
  }

  private[controllers] def canHaveChat(tour: Tour, json: Option[JsObject])(implicit ctx: Context): Boolean =
    !ctx.kid &&                                                                        // no public chats for kids
      ctx.me.fold(!tour.isPrivate) { u =>                                              // anon can see public chats, except for private tournaments
        (!tour.isPrivate || json.fold(true)(jsonHasMe) || isGranted(_.ChatTimeout)) && // private tournament that I joined or has ChatTimeout
        env.chat.panic.allowed(u, tighter = tour.variant == chess.variant.Antichess)
      }

  private def jsonHasMe(js: JsObject): Boolean = (js \ "me").toOption.isDefined

  def show(id: String) = Open { implicit ctx =>
    val page = getInt("page")
    repo byId id flatMap { tourOption =>
      negotiate(
        html = tourOption
          .fold(tournamentNotFound.fuccess) { tour =>
            (for {
              verdicts <- api.verdicts(tour, ctx.me, getUserTeamIds)
              version  <- env.tournament.version(tour.id)
              json <- jsonView(
                tour = tour,
                page = page,
                me = ctx.me,
                getUserTeamIds = getUserTeamIds,
                getTeamName = env.team.getTeamName,
                playerInfoExt = none,
                socketVersion = version.some,
                partial = false
              )
              chat <- canHaveChat(tour, json.some) ?? env.chat.api.userChat.cached
                .findMine(Chat.Id(tour.id), ctx.me)
                .map(some)
              _ <- chat ?? { c =>
                env.user.lightUserApi.preloadMany(c.chat.userIds)
              }
              _ <- tour.teamBattle ?? { b =>
                env.team.cached.preloadSet(b.teams)
              }
              streamers   <- streamerCache get tour.id
              shieldOwner <- env.tournament.shieldApi currentOwner tour
            } yield Ok(html.tournament.show(tour, verdicts, json, chat, streamers, shieldOwner)))
          }
          .monSuccess(_.tournament.apiShowPartial(false, HTTPRequest clientName ctx.req)),
        api = _ =>
          tourOption
            .fold(notFoundJson("No such tournament")) { tour =>
              get("playerInfo").?? { api.playerInfo(tour, _) } zip
                getBool("socketVersion").??(env.tournament version tour.id map some) flatMap {
                case (playerInfoExt, socketVersion) =>
                  val partial = getBool("partial")
                  jsonView(
                    tour = tour,
                    page = page,
                    me = ctx.me,
                    getUserTeamIds = getUserTeamIds,
                    getTeamName = env.team.getTeamName,
                    playerInfoExt = playerInfoExt,
                    socketVersion = socketVersion,
                    partial = partial
                  )
              } dmap { Ok(_) }
            }
            .monSuccess(_.tournament.apiShowPartial(getBool("partial"), HTTPRequest clientName ctx.req))
      ) dmap NoCache
    }
  }

  def standing(id: String, page: Int) = Open { implicit ctx =>
    OptionFuResult(repo byId id) { tour =>
      env.tournament.standingApi(tour, page) map { data =>
        Ok(data) as JSON
      }
    }
  }

  def pageOf(id: String, userId: String) = Open { implicit ctx =>
    OptionFuResult(repo byId id) { tour =>
      api.pageOf(tour, UserModel normalize userId) flatMap {
        _ ?? { page =>
          env.tournament.standingApi(tour, page) map { data =>
            Ok(data) as JSON
          }
        }
      }
    }
  }

  def player(tourId: String, userId: String) = Open { _ =>
    env.tournament.tournamentRepo byId tourId flatMap {
      _ ?? { tour =>
        JsonOk {
          api.playerInfo(tour, userId) flatMap {
            _ ?? { jsonView.playerInfoExtended(tour, _) }
          }
        }
      }
    }
  }

  def teamInfo(tourId: String, teamId: TeamID) = Open { _ =>
    env.tournament.tournamentRepo byId tourId flatMap {
      _ ?? { tour =>
        jsonView.teamInfo(tour, teamId) map {
          _ ?? { json =>
            Ok(json) as JSON
          }
        }
      }
    }
  }

  def join(id: String) = AuthBody(parse.json) { implicit ctx => implicit me =>
    NoLameOrBot {
      NoPlayban {
        val password = ctx.body.body.\("p").asOpt[String]
        val teamId   = ctx.body.body.\("team").asOpt[String]
        api.joinWithResult(id, me, password, teamId, getUserTeamIds) flatMap { result =>
          negotiate(
            html = Redirect(routes.Tournament.show(id)).fuccess,
            api = _ =>
              fuccess {
                if (result) jsonOkResult
                else BadRequest(Json.obj("joined" -> false))
              }
          )
        }
      }
    }
  }

  def pause(id: String) = Auth { implicit ctx => me =>
    OptionResult(repo byId id) { tour =>
      api.selfPause(tour.id, me.id)
      if (HTTPRequest.isXhr(ctx.req)) jsonOkResult
      else Redirect(routes.Tournament.show(tour.id))
    }
  }

  def form = Auth { implicit ctx => me =>
    NoLameOrBot {
      teamC.teamsIBelongTo(me) map { teams =>
        Ok(html.tournament.form.create(forms.create(me), teams))
      }
    }
  }

  def teamBattleForm(teamId: TeamID) = Auth { implicit ctx => me =>
    NoLameOrBot {
      env.team.api.owns(teamId, me.id) map {
        _ ?? {
          Ok(html.tournament.form.create(forms.create(me, teamId.some), Nil))
        }
      }
    }
  }

  private val CreateLimitPerUser = new lila.memo.RateLimit[lila.user.User.ID](
    credits = 20,
    duration = 24 hour,
    name = "tournament per user",
    key = "tournament.user"
  )

  private val CreateLimitPerIP = new lila.memo.RateLimit[lila.common.IpAddress](
    credits = 30,
    duration = 24 hour,
    name = "tournament per IP",
    key = "tournament.ip"
  )

  private val rateLimited = ornicar.scalalib.Zero.instance[Fu[Result]] {
    fuccess(Redirect(routes.Tournament.home(1)))
  }

  private def rateLimitCreation(me: UserModel, isPrivate: Boolean, req: RequestHeader)(
      create: => Fu[Result]
  ): Fu[Result] = {
    val cost =
      if (me.hasTitle ||
          env.streamer.liveStreamApi.isStreaming(me.id) ||
          isGranted(_.ManageTournament, me) ||
          me.isVerified ||
          isPrivate) 1
      else 2
    CreateLimitPerUser(me.id, cost = cost) {
      CreateLimitPerIP(HTTPRequest lastRemoteAddress req, cost = cost) {
        create
      }(rateLimited)
    }(rateLimited)
  }

  def create = AuthBody { implicit ctx => me =>
    NoLameOrBot {
      teamC.teamsIBelongTo(me) flatMap { teams =>
        implicit val req = ctx.body
        negotiate(
          html = forms
            .create(me)
            .bindFromRequest
            .fold(
              err => BadRequest(html.tournament.form.create(err, teams)).fuccess,
              setup =>
                rateLimitCreation(me, setup.isPrivate, ctx.req) {
                  api.createTournament(setup, me, teams, getUserTeamIds) map { tour =>
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

  def apiCreate = ScopedBody() { implicit req => me =>
    if (me.isBot || me.lame) notFoundJson("This account cannot create tournaments")
    else doApiCreate(me)
  }

  private def doApiCreate(me: lila.user.User)(implicit req: Request[_]): Fu[Result] =
    forms
      .create(me)
      .bindFromRequest
      .fold(
        jsonFormErrorDefaultLang,
        setup =>
          rateLimitCreation(me, setup.isPrivate, req) {
            teamC.teamsIBelongTo(me) flatMap { teams =>
              api.createTournament(setup, me, teams, getUserTeamIds) flatMap { tour =>
                jsonView(
                  tour,
                  none,
                  none,
                  getUserTeamIds,
                  env.team.getTeamName,
                  none,
                  none,
                  partial = false
                )(reqLang) map { Ok(_) }
              }
            }
          }
      )

  def teamBattleEdit(id: String) = Auth { implicit ctx => me =>
    repo byId id flatMap {
      _ ?? {
        case tour if tour.createdBy == me.id =>
          tour.teamBattle ?? { battle =>
            env.team.teamRepo.byOrderedIds(battle.sortedTeamIds) flatMap { teams =>
              env.user.lightUserApi.preloadMany(teams.map(_.createdBy)) >> {
                val form = lila.tournament.TeamBattle.DataForm.edit(teams.map { t =>
                  s"""${t.id} "${t.name}" by ${env.user.lightUserApi
                    .sync(t.createdBy)
                    .fold(t.createdBy)(_.name)}"""
                }, battle.nbLeaders)
                Ok(html.tournament.teamBattle.edit(tour, form)).fuccess
              }
            }
          }
        case tour => Redirect(routes.Tournament.show(tour.id)).fuccess
      }
    }
  }

  def teamBattleUpdate(id: String) = AuthBody { implicit ctx => me =>
    repo byId id flatMap {
      _ ?? {
        case tour if tour.createdBy == me.id && !tour.isFinished =>
          implicit val req = ctx.body
          lila.tournament.TeamBattle.DataForm.empty.bindFromRequest.fold(
            err => BadRequest(html.tournament.teamBattle.edit(tour, err)).fuccess,
            res =>
              api.teamBattleUpdate(tour, res, env.team.api.filterExistingIds) inject
                Redirect(routes.Tournament.show(tour.id))
          )
        case tour => Redirect(routes.Tournament.show(tour.id)).fuccess
      }
    }
  }

  def featured = Open { implicit ctx =>
    negotiate(
      html = notFound,
      api = _ =>
        env.tournament.cached.promotable.getUnit.nevermind map {
          lila.tournament.Spotlight.select(_, ctx.me, 4)
        } flatMap env.tournament.apiJsonView.featured map { Ok(_) }
    )
  }

  def shields = Open { implicit ctx =>
    for {
      history <- env.tournament.shieldApi.history(5.some)
      _       <- env.user.lightUserApi preloadMany history.userIds
    } yield html.tournament.shields(history)
  }

  def categShields(k: String) = Open { implicit ctx =>
    OptionFuOk(env.tournament.shieldApi.byCategKey(k)) {
      case (categ, awards) =>
        env.user.lightUserApi preloadMany awards.map(_.owner.value) inject
          html.tournament.shields.byCateg(categ, awards)
    }
  }

  def calendar = Open { implicit ctx =>
    api.calendar map { tours =>
      Ok(html.tournament.calendar(env.tournament.apiJsonView calendar tours))
    }
  }

  def edit(id: String) = Auth { implicit ctx => me =>
    WithEditableTournament(id, me) { tour =>
      teamC.teamsIBelongTo(me) map { teams =>
        val form = forms.edit(me, tour)
        Ok(html.tournament.form.edit(tour, form, teams))
      }
    }
  }

  def update(id: String) = AuthBody { implicit ctx => me =>
    WithEditableTournament(id, me) { tour =>
      implicit val req = ctx.body
      teamC.teamsIBelongTo(me) flatMap { teams =>
        forms
          .edit(me, tour)
          .bindFromRequest
          .fold(
            err => BadRequest(html.tournament.form.edit(tour, err, teams)).fuccess,
            data => api.update(tour, data, teams) inject Redirect(routes.Tournament.show(id)).flashSuccess
          )
      }
    }
  }

  def terminate(id: String) = Auth { implicit ctx => me =>
    WithEditableTournament(id, me) { tour =>
      api kill tour inject {
        env.mod.logApi.terminateTournament(me.id, tour.name())
        Redirect(routes.Tournament.home(1))
      }
    }
  }

  private def WithEditableTournament(id: String, me: UserModel)(
      f: Tour => Fu[Result]
  )(implicit ctx: Context): Fu[Result] =
    repo byId id flatMap {
      case Some(t) if (t.createdBy == me.id && !t.isFinished) || isGranted(_.ManageTournament) =>
        f(t)
      case Some(t) => Redirect(routes.Tournament.show(t.id)).fuccess
      case _       => notFound
    }

  private val streamerCache = env.memo.cacheApi[Tour.ID, Set[UserModel.ID]](64, "tournament.streamers") {
    _.refreshAfterWrite(15.seconds)
      .maximumSize(64)
      .buildAsyncFuture { tourId =>
        env.tournament.tournamentRepo.isUnfinished(tourId) flatMap {
          _ ?? {
            env.streamer.liveStreamApi.all.flatMap {
              _.streams
                .map { stream =>
                  env.tournament.hasUser(tourId, stream.streamer.userId).dmap(_ option stream.streamer.userId)
                }
                .sequenceFu
                .dmap(_.flatten.toSet)
            }
          }
        }
      }
  }

  private def getUserTeamIds(user: lila.user.User): Fu[List[TeamID]] =
    env.team.cached.teamIdsList(user.id)
}
