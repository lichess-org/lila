package controllers

import akka.stream.scaladsl._
import play.api.data.Form
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._
import scala.language.existentials
import scala.util.chaining._
import scalatags.Text.Frag
import views._

import lila.api.{ BodyContext, Context }
import lila.app._
import lila.app.mashup.{ GameFilter, GameFilterMenu }
import lila.common.paginator.Paginator
import lila.common.{ HTTPRequest, IpAddress }
import lila.game.{ Pov, Game => GameModel }
import lila.rating.PerfType
import lila.socket.UserLagCache
import lila.user.{ User => UserModel }
import lila.security.UserLogins

final class User(
    env: Env,
    roundC: => Round,
    gameC: => Game,
    modC: => Mod,
    puzzleC: => Puzzle
) extends LilaController(env) {

  private def relationApi    = env.relation.api
  private def userGameSearch = env.gameSearch.userGameSearch

  def tv(username: String) =
    Open { implicit ctx =>
      OptionFuResult(env.user.repo named username) { user =>
        currentlyPlaying(user) orElse lastPlayed(user) flatMap {
          _.fold(fuccess(Redirect(routes.User.show(username)))) { pov =>
            ctx.me ifFalse pov.game.bothPlayersHaveMoved flatMap { Pov(pov.game, _) } match {
              case Some(mine) => Redirect(routes.Round.player(mine.fullId)).fuccess
              case _          => roundC.watch(pov, userTv = user.some)
            }
          }
        }
      }
    }

  def tvExport(username: String) =
    Action.async { req =>
      val userId = UserModel normalize username
      env.game.cached.lastPlayedPlayingId(userId) orElse
        env.game.gameRepo.quickLastPlayedId(userId) flatMap {
          case None         => NotFound("No ongoing game").fuccess
          case Some(gameId) => gameC.exportGame(gameId, req)
        }
    }

  private def apiGames(u: UserModel, filter: String, page: Int)(implicit ctx: BodyContext[_]) = {
    userGames(u, filter, page) flatMap env.api.userGameApi.jsPaginator map { res =>
      Ok(res ++ Json.obj("filter" -> GameFilter.All.name))
    }
  }

  def show(username: String) =
    OpenBody { implicit ctx =>
      EnabledUser(username) { u =>
        negotiate(
          html = renderShow(u),
          api = _ => apiGames(u, GameFilter.All.name, 1)
        )
      }
    }
  private def renderShow(u: UserModel, status: Results.Status = Results.Ok)(implicit ctx: Context) =
    if (HTTPRequest.isSynchronousHttp(ctx.req)) {
      for {
        as     <- env.activity.read.recent(u)
        nbs    <- env.userNbGames(u, ctx, withCrosstable = false)
        info   <- env.userInfo(u, nbs, ctx)
        social <- env.socialInfo(u, ctx)
      } yield status {
        lila.mon.chronoSync(_.user segment "renderSync") {
          html.user.show.page.activity(u, as, info, social)
        }
      }
    } else
      env.activity.read.recent(u) map { as =>
        status(html.activity(u, as))
      }

  def gamesAll(username: String, page: Int) = games(username, GameFilter.All.name, page)

  def games(username: String, filter: String, page: Int) =
    OpenBody { implicit ctx =>
      Reasonable(page) {
        EnabledUser(username) { u =>
          if (filter == "search" && ctx.isAnon)
            negotiate(
              html = Unauthorized(html.search.login(u.count.game)).fuccess,
              api = _ => Unauthorized(jsonError("Login required")).fuccess
            )
          else
            negotiate(
              html = for {
                nbs <- env.userNbGames(u, ctx, withCrosstable = true)
                filters = GameFilterMenu(u, nbs, filter, ctx.isAuth)
                pag <- env.gamePaginator(
                  user = u,
                  nbs = nbs.some,
                  filter = filters.current,
                  me = ctx.me,
                  page = page
                )(ctx.body, formBinding)
                _ <- env.user.lightUserApi preloadMany pag.currentPageResults.flatMap(_.userIds)
                _ <- env.tournament.cached.nameCache preloadMany {
                  pag.currentPageResults.flatMap(_.tournamentId).map(_ -> ctxLang)
                }
                res <-
                  if (HTTPRequest isSynchronousHttp ctx.req) for {
                    info   <- env.userInfo(u, nbs, ctx)
                    _      <- env.team.cached.nameCache preloadMany info.teamIds
                    social <- env.socialInfo(u, ctx)
                    searchForm =
                      (filters.current == GameFilter.Search) option
                        GameFilterMenu.searchForm(userGameSearch, filters.current)(ctx.body, formBinding)
                  } yield html.user.show.page.games(u, info, pag, filters, searchForm, social)
                  else fuccess(html.user.show.gamesContent(u, nbs, pag, filters, filter))
              } yield res,
              api = _ => apiGames(u, filter, page)
            )
        }
      }
    }

  private def EnabledUser(username: String)(f: UserModel => Fu[Result])(implicit ctx: Context): Fu[Result] =
    env.user.repo named username flatMap {
      case None if isGranted(_.UserModView)                 => modC.searchTerm(username.trim)
      case None                                             => notFound
      case Some(u) if u.enabled || isGranted(_.UserModView) => f(u)
      case Some(u) =>
        negotiate(
          html = env.user.repo isErased u flatMap { erased =>
            if (erased.value) notFound
            else NotFound(html.user.show.page.disabled(u)).fuccess
          },
          api = _ => fuccess(NotFound(jsonError("No such user, or account closed")))
        )
    }

  def showMini(username: String) =
    Open { implicit ctx =>
      OptionFuResult(env.user.repo named username) { user =>
        if (user.enabled || isGranted(_.UserModView))
          ctx.userId.?? { relationApi.fetchBlocks(user.id, _) } zip
            ctx.userId.?? { env.game.crosstableApi.fetchOrEmpty(user.id, _) dmap some } zip
            ctx.isAuth.?? { env.pref.api.followable(user.id) } zip
            ctx.userId.?? { relationApi.fetchRelation(_, user.id) } flatMap {
              case blocked ~ crosstable ~ followable ~ relation =>
                val ping = env.socket.isOnline(user.id) ?? UserLagCache.getLagRating(user.id)
                negotiate(
                  html = !ctx.is(user) ?? currentlyPlaying(user) map { pov =>
                    Ok(html.user.mini(user, pov, blocked, followable, relation, ping, crosstable))
                      .withHeaders(CACHE_CONTROL -> "max-age=5")
                  },
                  api = _ => {
                    import lila.game.JsonView.crosstableWrites
                    fuccess(
                      Ok(
                        Json.obj(
                          "crosstable" -> crosstable,
                          "perfs"      -> lila.user.JsonView.perfs(user, user.best8Perfs)
                        )
                      )
                    )
                  }
                )
            }
        else fuccess(Ok(html.user.bits.miniClosed(user)))
      }
    }

  def online =
    Action.async { implicit req =>
      val max = 50
      negotiate(
        html = notFoundJson(),
        api = _ =>
          env.user.cached.getTop50Online map { users =>
            Ok(
              Json.toJson(
                users
                  .take(getInt("nb", req).fold(10)(_ min max))
                  .map(env.user.jsonView(_))
              )
            )
          }
      )
    }

  def ratingHistory(username: String) =
    OpenBody { implicit ctx =>
      EnabledUser(username) { u =>
        env.history
          .ratingChartApi(u)
          .dmap(_ | "[]") // send an empty JSON array if no history JSON is available
          .dmap(JsonOk(_))
      }
    }

  private def currentlyPlaying(user: UserModel): Fu[Option[Pov]] =
    env.game.cached.lastPlayedPlayingId(user.id) flatMap {
      _ ?? { env.round.proxyRepo.pov(_, user) }
    }

  private def lastPlayed(user: UserModel): Fu[Option[Pov]] =
    env.game.gameRepo
      .lastPlayed(user)
      .flatMap(_ ?? { p =>
        env.round.proxyRepo.upgradeIfPresent(p) dmap some
      })

  private val UserGamesRateLimitPerIP = new lila.memo.RateLimit[IpAddress](
    credits = 500,
    duration = 10.minutes,
    key = "user_games.web.ip"
  )

  private def userGames(
      u: UserModel,
      filterName: String,
      page: Int
  )(implicit ctx: BodyContext[_]): Fu[Paginator[GameModel]] = {
    UserGamesRateLimitPerIP(HTTPRequest ipAddress ctx.req, cost = page, msg = s"on ${u.username}") {
      lila.mon.http.userGamesCost.increment(page.toLong)
      for {
        pagFromDb <- env.gamePaginator(
          user = u,
          nbs = none,
          filter = GameFilterMenu.currentOf(GameFilterMenu.all, filterName),
          me = ctx.me,
          page = page
        )(ctx.body, formBinding)
        pag <- pagFromDb.mapFutureResults(env.round.proxyRepo.upgradeIfPresent)
        _ <- env.tournament.cached.nameCache preloadMany {
          pag.currentPageResults.flatMap(_.tournamentId).map(_ -> ctxLang)
        }
        _ <- env.user.lightUserApi preloadMany pag.currentPageResults.flatMap(_.userIds)
      } yield pag
    }(fuccess(Paginator.empty[GameModel]))
  }

  def list =
    Open { implicit ctx =>
      env.user.cached.top10.get {} flatMap { leaderboards =>
        negotiate(
          html =
            for {
              nbAllTime      <- env.user.cached.top10NbGame.get {}
              tourneyWinners <- env.tournament.winners.all.map(_.top)
              topOnline      <- env.user.cached.getTop50Online
              _              <- env.user.lightUserApi preloadMany tourneyWinners.map(_.userId)
            } yield Ok(
              html.user.list(
                tourneyWinners = tourneyWinners,
                online = topOnline,
                leaderboards = leaderboards,
                nbAllTime = nbAllTime
              )
            ),
          api = _ =>
            fuccess {
              implicit val lpWrites = OWrites[UserModel.LightPerf](env.user.jsonView.lightPerfIsOnline)
              Ok(
                Json.obj(
                  "bullet"        -> leaderboards.bullet,
                  "blitz"         -> leaderboards.blitz,
                  "rapid"         -> leaderboards.rapid,
                  "classical"     -> leaderboards.classical,
                  "ultraBullet"   -> leaderboards.ultraBullet,
                  "crazyhouse"    -> leaderboards.crazyhouse,
                  "chess960"      -> leaderboards.chess960,
                  "kingOfTheHill" -> leaderboards.kingOfTheHill,
                  "threeCheck"    -> leaderboards.threeCheck,
                  "antichess"     -> leaderboards.antichess,
                  "atomic"        -> leaderboards.atomic,
                  "horde"         -> leaderboards.horde,
                  "racingKings"   -> leaderboards.racingKings
                )
              )
            }
        )
      }
    }

  def topNb(nb: Int, perfKey: String) =
    Open { implicit ctx =>
      PerfType(perfKey) ?? { perfType =>
        env.user.cached.top200Perf get perfType.id dmap { _ take (nb atLeast 1 atMost 200) } flatMap {
          users =>
            negotiate(
              html = Ok(html.user.top(perfType, users)).fuccess,
              api = _ =>
                fuccess {
                  implicit val lpWrites = OWrites[UserModel.LightPerf](env.user.jsonView.lightPerfIsOnline)
                  Ok(Json.obj("users" -> users))
                }
            )
        }
      }
    }

  def topWeek =
    Open { implicit ctx =>
      negotiate(
        html = notFound,
        api = _ =>
          env.user.cached.topWeek.map { users =>
            Ok(Json toJson users.map(env.user.jsonView.lightPerfIsOnline))
          }
      )
    }

  def mod(username: String) =
    Secure(_.UserModView) { implicit ctx => _ =>
      modZoneOrRedirect(username)
    }

  protected[controllers] def modZoneOrRedirect(username: String)(implicit ctx: Context): Fu[Result] =
    if (HTTPRequest isEventSource ctx.req) renderModZone(username)
    else fuccess(modC.redirect(username))

  private def modZoneSegment(fu: Fu[Frag], name: String, user: UserModel): Source[Frag, _] =
    Source futureSource {
      fu.monSuccess(_.mod zoneSegment name)
        .logFailure(lila.log("modZoneSegment").branch(s"$name ${user.id}"))
        .map(Source.single)
    }

  protected[controllers] def loginsTableData(user: UserModel, userLogins: UserLogins, max: Int)(implicit
      ctx: Context
  ): Fu[UserLogins.TableData] = {
    val familyUserIds = user.id :: userLogins.otherUserIds
    (isGranted(_.ModNote) ?? env.user.noteApi
      .forMod(familyUserIds)
      .logTimeIfGt(s"${user.username} noteApi.forMod", 2 seconds)) zip
      env.playban.api.bans(familyUserIds).logTimeIfGt(s"${user.username} playban.bans", 2 seconds) zip
      lila.security.UserLogins.withMeSortedWithEmails(env.user.repo, user, userLogins) map {
        case notes ~ bans ~ othersWithEmail =>
          UserLogins.TableData(userLogins, othersWithEmail, notes, bans, max)
      }
  }

  protected[controllers] def renderModZone(username: String)(implicit ctx: Context): Fu[Result] = {
    env.user.repo withEmails username orFail s"No such user $username" map {
      case UserModel.WithEmails(user, emails) =>
        import html.user.{ mod => view }
        import lila.app.ui.ScalatagsExtensions.LilaFragZero

        val nbOthers = getInt("nbOthers") | 100

        val modLog = for {
          history <- env.mod.logApi.userHistory(user.id)
          appeal  <- isGranted(_.Appeals) ?? env.appeal.api.get(user)
        } yield view.modLog(history, appeal)

        val plan = isGranted(_.Admin) ?? env.plan.api.recentChargesOf(user).map(view.plan).dmap(~_)

        val student = env.clas.api.student.findManaged(user).map2(view.student).dmap(~_)

        val reportLog = isGranted(_.SeeReport) ?? env.report.api
          .byAndAbout(user, 20)
          .flatMap { rs =>
            env.user.lightUserApi.preloadMany(rs.userIds) inject rs
          }
          .map(view.reportLog(user))

        val prefs = env.pref.api.getPref(user) map view.prefs(user)

        val rageSit = env.playban.api.getRageSit(user.id).map(view.showRageSit)

        val actions = env.user.repo.isErased(user) map { erased =>
          html.user.mod.actions(user, emails, erased, env.mod.presets.pmPresets.get())
        }
        val userLoginsFu = env.security.userLogins(user, nbOthers)
        val others = userLoginsFu flatMap { userLogins =>
          loginsTableData(user, userLogins, nbOthers) map {
            html.user.mod.otherUsers(user, _)
          }
        }
        val identification = userLoginsFu map { spy =>
          (isGranted(_.Doxing) || (user.lameOrAlt && !user.hasTitle)) ??
            html.user.mod.identification(spy)
        }
        val irwin = isGranted(_.MarkEngine) ?? env.irwin.api.reports.withPovs(user).map {
          _ ?? { reps =>
            html.irwin.report(reps)
          }
        }
        val assess = isGranted(_.MarkEngine) ?? env.mod.assessApi.getPlayerAggregateAssessmentWithGames(
          user.id
        ) flatMap {
          _ ?? { as =>
            env.user.lightUserApi
              .preloadMany(as.games.flatMap(_.userIds)) inject html.user.mod.assessments(user, as)
          }
        }
        implicit val extractor = EventSource.EventDataExtractor[Frag](_.render)
        Ok.chunked {
          Source.single(html.user.mod.menu) merge
            modZoneSegment(actions, "actions", user) merge
            modZoneSegment(modLog, "modLog", user) merge
            modZoneSegment(plan, "plan", user) merge
            modZoneSegment(student, "student", user) merge
            modZoneSegment(reportLog, "reportLog", user) merge
            modZoneSegment(prefs, "prefs", user) merge
            modZoneSegment(rageSit, "rageSit", user) merge
            modZoneSegment(others, "others", user) merge
            modZoneSegment(identification, "identification", user) merge
            modZoneSegment(irwin, "irwin", user) merge
            modZoneSegment(assess, "assess", user) via
            EventSource.flow
        }.as(ContentTypes.EVENT_STREAM) pipe noProxyBuffer
    }
  }

  protected[controllers] def renderModZoneActions(username: String)(implicit ctx: Context) =
    env.user.repo withEmails username orFail s"No such user $username" flatMap {
      case UserModel.WithEmails(user, emails) =>
        env.user.repo.isErased(user) map { erased =>
          Ok(html.user.mod.actions(user, emails, erased, env.mod.presets.pmPresets.get()))
        }
    }

  def writeNote(username: String) =
    AuthBody { implicit ctx => me =>
      doWriteNote(username, me)(
        err = _ => user => renderShow(user, Results.BadRequest),
        suc = Redirect(routes.User.show(username).url + "?note").flashSuccess
      )(ctx.body)
    }

  def apiWriteNote(username: String) =
    ScopedBody() { implicit req => me =>
      doWriteNote(username, me)(
        err = err => _ => jsonFormErrorDefaultLang(err),
        suc = jsonOkResult
      )
    }

  private def doWriteNote(
      username: String,
      me: UserModel
  )(err: Form[_] => UserModel => Fu[Result], suc: => Result)(implicit req: Request[_]) =
    env.user.repo named username flatMap {
      _ ?? { user =>
        env.user.forms.note
          .bindFromRequest()
          .fold(
            e => err(e)(user),
            data =>
              {
                val isMod = data.mod && isGranted(_.ModNote, me)
                env.user.noteApi.write(user, data.text, me, isMod, isMod && ~data.dox)
              } inject suc
          )
      }
    }

  def deleteNote(id: String) =
    Auth { implicit ctx => me =>
      OptionFuResult(env.user.noteApi.byId(id)) { note =>
        (note.isFrom(me) && !note.mod) ?? {
          env.user.noteApi.delete(note._id) inject Redirect(routes.User.show(note.to).url + "?note")
        }
      }
    }

  def opponents =
    Auth { implicit ctx => me =>
      for {
        ops         <- env.game.favoriteOpponents(me.id)
        followables <- env.pref.api.followables(ops map (_._1.id))
        relateds <-
          ops
            .zip(followables)
            .map { case ((u, nb), followable) =>
              relationApi.fetchRelation(me.id, u.id) map {
                lila.relation.Related(u, nb.some, followable, _)
              }
            }
            .sequenceFu
      } yield html.user.opponents(me, relateds)
    }

  def perfStat(username: String, perfKey: String) =
    Open { implicit ctx =>
      OptionFuResult(env.user.repo named username) { u =>
        if ((u.disabled || (u.lame && !ctx.is(u))) && !isGranted(_.UserModView)) notFound
        else
          PerfType(perfKey).fold(notFound) { perfType =>
            for {
              ranks       <- env.user.cached rankingsOf u.id
              oldPerfStat <- env.perfStat.get(u, perfType)
              perfStat = oldPerfStat.copy(playStreak = oldPerfStat.playStreak.checkCurrent)
              distribution <- u.perfs(perfType).established ?? {
                env.user.rankingApi.weeklyRatingDistribution(perfType) dmap some
              }
              percentile = distribution.map { distrib =>
                lila.user.Stat.percentile(distrib, u.perfs(perfType).intRating) match {
                  case (under, sum) => Math.round(under * 1000.0 / sum) / 10.0
                }
              }
              ratingChart <- env.history.ratingChartApi(u)
              _           <- env.user.lightUserApi preloadMany { u.id :: perfStat.userIds.map(_.value) }
              response <- negotiate(
                html = Ok(html.user.perfStat(u, ranks, perfType, percentile, perfStat, ratingChart)).fuccess,
                api = _ =>
                  getBool("graph").?? {
                    env.history.ratingChartApi.singlePerf(u, perfType).map(_.some)
                  } map {
                    val data = env.perfStat.jsonView(u, perfStat, ranks get perfType, percentile)
                    _.fold(data) { graph =>
                      data + ("graph" -> graph)
                    }
                  } map { Ok(_) }
              )
            } yield response
          }
      }
    }

  def autocomplete =
    Open { implicit ctx =>
      get("term", ctx.req).filter(_.nonEmpty).filter(lila.user.User.couldBeUsername) match {
        case None => BadRequest("No search term provided").fuccess
        case Some(term) if getBool("exists") =>
          env.user.repo nameExists term map { r =>
            Ok(JsBoolean(r))
          }
        case Some(term) =>
          {
            (get("tour"), get("swiss")) match {
              case (Some(tourId), _)  => env.tournament.playerRepo.searchPlayers(tourId, term, 10)
              case (_, Some(swissId)) => env.swiss.api.searchPlayers(lila.swiss.Swiss.Id(swissId), term, 10)
              case _ =>
                ctx.me.ifTrue(getBool("friend")) match {
                  case Some(follower) =>
                    env.relation.api.searchFollowedBy(follower, term, 10) flatMap {
                      case Nil     => env.user.cached userIdsLike term
                      case userIds => fuccess(userIds)
                    }
                  case None if getBool("teacher") =>
                    env.user.repo.userIdsLikeWithRole(term, lila.security.Permission.Teacher.dbKey)
                  case None => env.user.cached userIdsLike term
                }
            }
          } flatMap { userIds =>
            if (getBool("names")) env.user.lightUserApi.asyncMany(userIds) map { users =>
              Json toJson users.flatMap(_.map(_.name))
            }
            else if (getBool("object")) env.user.lightUserApi.asyncMany(userIds) map { users =>
              Json.obj(
                "result" -> JsArray(users.flatten.map { u =>
                  lila.common.LightUser.lightUserWrites.writes(u).add("online" -> env.socket.isOnline(u.id))
                })
              )
            }
            else fuccess(Json toJson userIds)
          } map JsonOk
      }
    }

  def myself =
    Auth { _ => me =>
      fuccess(Redirect(routes.User.show(me.username)))
    }

  def redirect(username: String) =
    Open { implicit ctx =>
      tryRedirect(username) getOrElse notFound
    }

  def tryRedirect(username: String)(implicit ctx: Context): Fu[Option[Result]] =
    env.user.repo enabledNamed username map2 { user =>
      Redirect(routes.User.show(user.username))
    }
}
