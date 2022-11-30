package controllers

import akka.stream.scaladsl.*
import play.api.data.Form
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.libs.json.*
import play.api.mvc.*
import scala.concurrent.duration.*
import scala.language.existentials
import scala.util.chaining.*
import scalatags.Text.Frag
import views.*

import lila.api.{ BodyContext, Context }
import lila.app.{ given, * }
import lila.app.mashup.{ GameFilter, GameFilterMenu }
import lila.common.paginator.Paginator
import lila.common.{ HTTPRequest, IpAddress }
import lila.game.{ Game as GameModel, Pov }
import lila.rating.{ Perf, PerfType }
import lila.security.UserLogins
import lila.socket.UserLagCache
import lila.user.{ Holder, User as UserModel }
import lila.security.Granter

final class User(
    env: Env,
    roundC: => Round,
    gameC: => Game,
    modC: => Mod,
    puzzleC: => Puzzle
) extends LilaController(env):

  private def relationApi    = env.relation.api
  private def userGameSearch = env.gameSearch.userGameSearch

  def tv(username: UserStr) =
    Open { implicit ctx =>
      OptionFuResult(env.user.repo byId username) { user =>
        currentlyPlaying(user) orElse lastPlayed(user) flatMap {
          _.fold(fuccess(Redirect(routes.User.show(username.value)))) { pov =>
            ctx.me ifFalse pov.game.bothPlayersHaveMoved flatMap { Pov(pov.game, _) } match
              case Some(mine) => Redirect(routes.Round.player(mine.fullId)).toFuccess
              case _          => roundC.watch(pov, userTv = user.some)
          }
        }
      }
    }

  def tvExport(username: UserStr) =
    Action.async { req =>
      env.game.cached.lastPlayedPlayingId(username.id) orElse
        env.game.gameRepo.quickLastPlayedId(username.id) flatMap {
          case None         => NotFound("No ongoing game").toFuccess
          case Some(gameId) => gameC.exportGame(gameId, req)
        }
    }

  private def apiGames(u: UserModel, filter: String, page: Int)(implicit ctx: BodyContext[?]) =
    userGames(u, filter, page) flatMap env.api.userGameApi.jsPaginator map { res =>
      Ok(res ++ Json.obj("filter" -> GameFilter.All.name))
    }

  def show(username: UserStr) =
    OpenBody { implicit ctx =>
      EnabledUser(username) { u =>
        negotiate(
          html = renderShow(u),
          api = _ => apiGames(u, GameFilter.All.name, 1)
        )
      }
    }
  private def renderShow(u: UserModel, status: Results.Status = Results.Ok)(implicit ctx: Context) =
    if (HTTPRequest isSynchronousHttp ctx.req)
      for {
        as     <- env.activity.read.recentAndPreload(u)
        nbs    <- env.userNbGames(u, ctx, withCrosstable = false)
        info   <- env.userInfo(u, nbs, ctx)
        _      <- env.userInfo.preloadTeams(info)
        social <- env.socialInfo(u, ctx)
      } yield status {
        lila.mon.chronoSync(_.user segment "renderSync") {
          html.user.show.page.activity(u, as, info, social)
        }
      }.withCanonical(routes.User.show(u.username))
    else
      env.activity.read.recentAndPreload(u) map { as =>
        status(html.activity(u, as))
      }

  def download(username: UserStr) = OpenBody { implicit ctx =>
    val userOption = if (username.value == "me") fuccess(ctx.me) else env.user.repo byId username
    OptionOk(userOption.dmap(_.filter(u => u.enabled || ctx.is(u) || isGranted(_.GamesModView)))) { user =>
      html.user.download(user)
    }
  }

  def gamesAll(username: UserStr, page: Int) = games(username, GameFilter.All.name, page)

  def games(username: UserStr, filter: String, page: Int) =
    OpenBody { implicit ctx =>
      Reasonable(page) {
        EnabledUser(username) { u =>
          if (filter == "search" && ctx.isAnon)
            negotiate(
              html = Unauthorized(html.search.login(u.count.game)).toFuccess,
              api = _ => Unauthorized(jsonError("Login required")).toFuccess
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
                )(ctx.body, formBinding, reqLang)
                _ <- env.user.lightUserApi preloadMany pag.currentPageResults.flatMap(_.userIds)
                _ <- env.tournament.cached.nameCache preloadMany {
                  pag.currentPageResults.flatMap(_.tournamentId).map(_ -> ctxLang)
                }
                notes <- ctx.me ?? { me =>
                  env.round.noteApi.byGameIds(pag.currentPageResults.map(_.id), me.id)
                }
                res <-
                  if (HTTPRequest isSynchronousHttp ctx.req) for {
                    info   <- env.userInfo(u, nbs, ctx, withUblog = false)
                    _      <- env.team.cached.nameCache preloadMany info.teamIds
                    social <- env.socialInfo(u, ctx)
                    searchForm = (filters.current == GameFilter.Search) option
                      GameFilterMenu
                        .searchForm(userGameSearch, filters.current)(ctx.body, formBinding, reqLang)
                  } yield html.user.show.page.games(u, info, pag, filters, searchForm, social, notes)
                  else fuccess(html.user.show.gamesContent(u, nbs, pag, filters, filter, notes))
              } yield Ok(res).withCanonical(routes.User.games(u.username, filters.current.name)),
              api = _ => apiGames(u, filter, page)
            )
        }
      }
    }

  private def EnabledUser(username: UserStr)(f: UserModel => Fu[Result])(implicit ctx: Context): Fu[Result] =
    if (UserModel.isGhost(username.id))
      negotiate(
        html = Ok(html.site.bits.ghost).toFuccess,
        api = _ => notFoundJson("Deleted user")
      )
    else
      env.user.repo byId username flatMap {
        case None if isGranted(_.UserModView) =>
          ctx.me.map(Holder.apply) ?? { modC.searchTerm(_, username.value) }
        case None                                             => notFound
        case Some(u) if u.enabled || isGranted(_.UserModView) => f(u)
        case Some(u) =>
          negotiate(
            html = env.user.repo isErased u flatMap { erased =>
              if (erased.value) notFound
              else NotFound(html.user.show.page.disabled(u)).toFuccess
            },
            api = _ => fuccess(NotFound(jsonError("No such user, or account closed")))
          )
      }
  def showMini(username: UserStr) =
    Open { implicit ctx =>
      OptionFuResult(env.user.repo byId username) { user =>
        if (user.enabled || isGranted(_.UserModView))
          ctx.userId.?? { relationApi.fetchBlocks(user.id, _) } zip
            ctx.userId.?? { env.game.crosstableApi(user.id, _) dmap some } zip
            ctx.isAuth.?? { env.pref.api.followable(user.id) } zip
            ctx.userId.?? { relationApi.fetchRelation(_, user.id) } flatMap {
              case (((blocked, crosstable), followable), relation) =>
                val ping = env.socket.isOnline.value(user.id) ?? UserLagCache.getLagRating(user.id)
                negotiate(
                  html = !ctx.is(user) ?? currentlyPlaying(user) map { pov =>
                    Ok(html.user.mini(user, pov, blocked, followable, relation, ping, crosstable))
                      .withHeaders(CACHE_CONTROL -> "max-age=5")
                  },
                  api = _ => {
                    import lila.game.JsonView.given
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
                  .map(env.user.jsonView.full(_, withRating = true, withProfile = true))
              )
            )
          }
      )
    }

  def ratingHistory(username: UserStr) =
    OpenBody { implicit ctx =>
      EnabledUser(username) { u =>
        env.history
          .ratingChartApi(u)
          .dmap(_ | "[]") // send an empty JSON array if no history JSON is available
          .dmap(jsonStr => Ok(jsonStr) as JSON)
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
  )(implicit ctx: BodyContext[?]): Fu[Paginator[GameModel]] =
    UserGamesRateLimitPerIP(ctx.ip, cost = page, msg = s"on ${u.username}") {
      lila.mon.http.userGamesCost.increment(page.toLong)
      for {
        pagFromDb <- env.gamePaginator(
          user = u,
          nbs = none,
          filter = GameFilterMenu.currentOf(GameFilterMenu.all, filterName),
          me = ctx.me,
          page = page
        )(ctx.body, formBinding, reqLang)
        pag <- pagFromDb.mapFutureResults(env.round.proxyRepo.upgradeIfPresent)
        _ <- env.tournament.cached.nameCache preloadMany {
          pag.currentPageResults.flatMap(_.tournamentId).map(_ -> ctxLang)
        }
        _ <- env.user.lightUserApi preloadMany pag.currentPageResults.flatMap(_.userIds)
      } yield pag
    }(fuccess(Paginator.empty[GameModel]))

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
              given OWrites[UserModel.LightPerf] = OWrites(env.user.jsonView.lightPerfIsOnline)
              import lila.user.JsonView.leaderboardsWrites
              JsonOk(leaderboards)
            }
        )
      }
    }

  def apiList = Action.async {
    env.user.cached.top10.get {} map { leaderboards =>
      given OWrites[UserModel.LightPerf] = OWrites(env.user.jsonView.lightPerfIsOnline)
      import lila.user.JsonView.leaderboardsWrites
      JsonOk(leaderboards)
    }
  }

  def topNb(nb: Int, perfKey: Perf.Key) =
    Open { implicit ctx =>
      topNbUsers(nb, perfKey) flatMap {
        _ ?? { case (users, perfType) =>
          negotiate(
            html = (nb == 200) ?? Ok(html.user.top(perfType, users)).toFuccess,
            api = _ => fuccess(topNbJson(users))
          )
        }
      }
    }

  def topNbApi(nb: Int, perfKey: Perf.Key) =
    Action.async {
      topNbUsers(nb, perfKey) map { _ ?? { users => topNbJson(users._1) } }
    }

  private def topNbUsers(nb: Int, perfKey: Perf.Key) =
    PerfType(perfKey) ?? { perfType =>
      env.user.cached.top200Perf get perfType.id dmap {
        _.take(nb atLeast 1 atMost 200) -> perfType
      } dmap some
    }

  private def topNbJson(users: List[UserModel.LightPerf]) =
    given OWrites[UserModel.LightPerf] = OWrites(env.user.jsonView.lightPerfIsOnline)
    Ok(Json.obj("users" -> users))

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

  def mod(username: UserStr) =
    Secure(_.UserModView) { implicit ctx => holder =>
      modZoneOrRedirect(holder, username)
    }

  protected[controllers] def modZoneOrRedirect(holder: Holder, username: UserStr)(using
      ctx: Context
  ): Fu[Result] =
    if (HTTPRequest isEventSource ctx.req) renderModZone(holder, username)
    else fuccess(modC.redirect(username))

  private def modZoneSegment(fu: Fu[Frag], name: String, user: UserModel): Source[Frag, ?] =
    Source futureSource {
      fu.monSuccess(_.mod zoneSegment name)
        .logFailure(lila.log("modZoneSegment").branch(s"$name ${user.id}"))
        .map(Source.single)
    }

  protected[controllers] def loginsTableData(user: UserModel, userLogins: UserLogins, max: Int)(using
      ctx: Context
  ): Fu[UserLogins.TableData] =
    val familyUserIds = user.id :: userLogins.otherUserIds
    (isGranted(_.ModNote) ?? env.user.noteApi
      .byUsersForMod(familyUserIds)
      .logTimeIfGt(s"${user.username} noteApi.forMod", 2 seconds)) zip
      env.playban.api.bans(familyUserIds).logTimeIfGt(s"${user.username} playban.bans", 2 seconds) zip
      lila.security.UserLogins.withMeSortedWithEmails(env.user.repo, user, userLogins) map {
        case ((notes, bans), othersWithEmail) =>
          UserLogins.TableData(userLogins, othersWithEmail, notes, bans, max)
      }

  protected[controllers] def renderModZone(holder: Holder, username: UserStr)(using
      ctx: Context
  ): Fu[Result] =
    env.user.repo withEmails username orFail s"No such user $username" map {
      case UserModel.WithEmails(user, emails) =>
        import html.user.{ mod as view }
        import lila.app.ui.ScalatagsExtensions.{ emptyFrag, given }
        implicit val renderIp = env.mod.ipRender(holder)

        val nbOthers = getInt("nbOthers") | 100

        val modLog = for {
          history <- env.mod.logApi.userHistory(user.id)
          appeal  <- isGranted(_.Appeals) ?? env.appeal.api.get(user)
        } yield view.modLog(history, appeal)

        val plan =
          isGranted(_.Admin) ?? env.plan.api
            .recentChargesOf(user)
            .map(view.plan(user))
            .dmap(_ | emptyFrag): Fu[Frag]

        val student = env.clas.api.student.findManaged(user).map2(view.student).dmap(~_)

        val reportLog = isGranted(_.SeeReport) ?? env.report.api
          .byAndAbout(user, 20, holder)
          .flatMap { rs =>
            env.user.lightUserApi.preloadMany(rs.userIds) inject rs
          }
          .map(view.reportLog(user))

        val prefs = isGranted(_.CheatHunter) ?? env.pref.api.getPref(user).map(view.prefs(user))

        val rageSit = isGranted(_.CheatHunter) ?? env.playban.api
          .getRageSit(user.id)
          .zip(env.playban.api.bans(user.id))
          .map { case (r, p) => view.showRageSitAndPlaybans(r, p) }

        val actions = env.user.repo.isErased(user) map { erased =>
          html.user.mod.actions(user, emails, erased, env.mod.presets.getPmPresets(holder.user))
        }
        val userLoginsFu = env.security.userLogins(user, nbOthers)
        val others = for {
          userLogins <- userLoginsFu
          appeals    <- env.appeal.api.byUserIds(user.id :: userLogins.otherUserIds)
          data       <- loginsTableData(user, userLogins, nbOthers)
        } yield html.user.mod.otherUsers(holder, user, data, appeals)

        val identification = userLoginsFu map { logins =>
          Granter.is(_.ViewPrintNoIP)(holder) ??
            html.user.mod.identification(holder, user, logins)
        }

        val kaladin = isGranted(_.MarkEngine) ?? env.irwin.kaladinApi.get(user).map {
          _.flatMap(_.response) ?? html.kaladin.report
        }

        val irwin = isGranted(_.MarkEngine) ?? env.irwin.irwinApi.reports.withPovs(user).map {
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
            modZoneSegment(kaladin, "kaladin", user) merge
            modZoneSegment(irwin, "irwin", user) merge
            modZoneSegment(assess, "assess", user) via
            EventSource.flow
        }.as(ContentTypes.EVENT_STREAM) pipe noProxyBuffer
    }

  protected[controllers] def renderModZoneActions(username: UserStr)(implicit ctx: Context) =
    env.user.repo withEmails username orFail s"No such user $username" flatMap {
      case UserModel.WithEmails(user, emails) =>
        env.user.repo.isErased(user) map { erased =>
          Ok(html.user.mod.actions(user, emails, erased, env.mod.presets.getPmPresets(ctx.me)))
        }
    }

  def writeNote(username: UserStr) =
    AuthBody { implicit ctx => me =>
      doWriteNote(username, me)(
        err => BadRequest(err.errors.toString).toFuccess,
        user =>
          if (getBool("inquiry")) env.user.noteApi.byUserForMod(user.id) map { notes =>
            Ok(views.html.mod.inquiry.noteZone(user, notes))
          }
          else
            env.socialInfo.fetchNotes(user, me) map { notes =>
              Ok(views.html.user.show.header.noteZone(user, notes))
            }
      )(ctx.body)
    }

  def apiReadNote(username: UserStr) =
    Scoped() { implicit req => me =>
      env.user.repo byId username flatMap {
        _ ?? {
          env.socialInfo.fetchNotes(_, me) flatMap {
            lila.user.JsonView.notes(_)(env.user.lightUserApi)
          } map JsonOk
        }
      }
    }

  def apiWriteNote(username: UserStr) =
    ScopedBody() { implicit req => me =>
      doWriteNote(username, me)(
        jsonFormErrorDefaultLang,
        suc = _ => jsonOkResult.toFuccess
      )
    }

  private def doWriteNote(
      username: UserStr,
      me: UserModel
  )(err: Form[?] => Fu[Result], suc: UserModel => Fu[Result])(implicit req: Request[?]) =
    env.user.repo byId username flatMap {
      _ ?? { user =>
        lila.user.UserForm.note
          .bindFromRequest()
          .fold(
            err,
            data =>
              {
                val isMod = data.mod && isGranted(_.ModNote, me)
                env.user.noteApi.write(user, data.text, me, isMod, isMod && ~data.dox)
              } >> suc(user)
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
      getUserStr("u")
        .ifTrue(isGranted(_.BoostHunter))
        .??(env.user.repo.byId)
        .map(_ | me)
        .flatMap { user =>
          for {
            ops         <- env.game.favoriteOpponents(user.id)
            followables <- env.pref.api.followables(ops map (_._1.id))
            relateds <-
              ops
                .zip(followables)
                .map { case ((u, nb), followable) =>
                  relationApi.fetchRelation(user.id, u.id) map {
                    lila.relation.Related(u, nb.some, followable, _)
                  }
                }
                .sequenceFu
          } yield html.relation.bits.opponents(user, relateds)
        }
    }

  def perfStat(username: UserStr, perfKey: Perf.Key) =
    Open { implicit ctx =>
      env.perfStat.api.data(username, perfKey, ctx.me) flatMap {
        _ ?? { data =>
          negotiate(
            html = env.history.ratingChartApi(data.user) map { chart =>
              Ok(html.user.perfStat(data, chart))
            },
            api = _ =>
              JsonOk {
                getBool("graph").?? {
                  env.history.ratingChartApi.singlePerf(data.user, data.stat.perfType) map some
                } map { graph =>
                  env.perfStat.jsonView(data).add("graph", graph)
                }
              }
          )
        }
      }
    }

  def autocomplete =
    Open { implicit ctx =>
      getUserStr("term", ctx.req).flatMap(UserModel.validateId) match
        case None => BadRequest("No search term provided").toFuccess
        case Some(id) if getBool("exists") =>
          env.user.repo exists id map { r =>
            Ok(JsBoolean(r))
          }
        case Some(term) =>
          {
            (get("tour"), get("swiss"), get("team")) match
              case (Some(tourId), _, _) => env.tournament.playerRepo.searchPlayers(TourId(tourId), term, 10)
              case (_, Some(swissId), _) =>
                env.swiss.api.searchPlayers(SwissId(swissId), term, 10)
              case (_, _, Some(teamId)) => env.team.api.searchMembers(TeamId(teamId), term, 10)
              case _ =>
                ctx.me.ifTrue(getBool("friend")) match
                  case Some(follower) =>
                    env.relation.api.searchFollowedBy(follower, term, 10) flatMap {
                      case Nil     => env.user.cached userIdsLike term
                      case userIds => fuccess(userIds)
                    }
                  case None if getBool("teacher") =>
                    env.user.repo.userIdsLikeWithRole(term, lila.security.Permission.Teacher.dbKey)
                  case None => env.user.cached userIdsLike term
          } flatMap { userIds =>
            if (getBool("names")) env.user.lightUserApi.asyncMany(userIds) map { users =>
              Json toJson users.flatMap(_.map(_.name))
            }
            else if (getBool("object")) env.user.lightUserApi.asyncMany(userIds) map { users =>
              Json.obj(
                "result" -> JsArray(users collect { case Some(u) =>
                  lila.common.LightUser.lightUserWrites
                    .writes(u)
                    .add("online" -> env.socket.isOnline.value(u.id))
                })
              )
            }
            else fuccess(Json toJson userIds)
          } map JsonOk
    }

  def ratingDistribution(perfKey: lila.rating.Perf.Key, username: Option[UserStr] = None) =
    Open { implicit ctx =>
      lila.rating.PerfType(perfKey).filter(lila.rating.PerfType.leaderboardable.has) match
        case Some(perfType) =>
          env.user.rankingApi.weeklyRatingDistribution(perfType) flatMap { data =>
            username match
              case Some(name) =>
                EnabledUser(name) { u =>
                  fuccess(html.stat.ratingDistribution(perfType, data, Some(u)))
                }
              case _ => fuccess(html.stat.ratingDistribution(perfType, data, None))
          }
        case _ => notFound
    }

  def myself =
    Auth { _ => me =>
      fuccess(Redirect(routes.User.show(me.username)))
    }

  def redirect(username: UserStr) =
    Open { implicit ctx =>
      staticRedirect(username.value) | {
        tryRedirect(username) getOrElse notFound
      }
    }

  def tryRedirect(username: UserStr)(implicit ctx: Context): Fu[Option[Result]] =
    env.user.repo byId username map {
      _.filter(_.enabled || isGranted(_.SeeReport)) map { user =>
        Redirect(routes.User.show(user.username))
      }
    }
