package controllers

import akka.stream.scaladsl.*
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.libs.json.*
import play.api.mvc.*
import scala.language.existentials
import scala.util.chaining.*
import scalatags.Text.Frag
import views.*

import lila.app.{ given, * }
import lila.app.mashup.{ GameFilter, GameFilterMenu }
import lila.common.paginator.Paginator
import lila.common.{ HTTPRequest, IpAddress }
import lila.game.{ Game as GameModel, Pov }
import lila.rating.{ Perf, PerfType }
import lila.socket.UserLagCache
import lila.user.{ User as UserModel }
import lila.security.{ Granter, UserLogins }
import lila.mod.UserWithModlog

final class User(
    override val env: Env,
    roundC: => Round,
    gameC: => Game,
    modC: => Mod
) extends LilaController(env):

  import env.relation.{ api as relationApi }
  import env.gameSearch.userGameSearch
  import env.user.lightUserApi

  def tv(username: UserStr) = Open:
    Found(env.user.repo byId username): user =>
      currentlyPlaying(user) orElse lastPlayed(user) flatMap {
        _.fold(fuccess(Redirect(routes.User.show(username.value)))): pov =>
          ctx.me.filterNot(_ => pov.game.bothPlayersHaveMoved).flatMap { Pov(pov.game, _) } match
            case Some(mine) => Redirect(routes.Round.player(mine.fullId))
            case _          => roundC.watch(pov, userTv = user.some)
      }

  def tvExport(username: UserStr) = Anon:
    env.game.cached.lastPlayedPlayingId(username.id) orElse
      env.game.gameRepo.quickLastPlayedId(username.id) flatMap {
        case None         => NotFound("No ongoing game")
        case Some(gameId) => gameC.exportGame(gameId)
      }

  private def apiGames(u: UserModel, filter: String, page: Int)(using BodyContext[?]) =
    userGames(u, filter, page) flatMap env.api.userGameApi.jsPaginator map { res =>
      Ok(res ++ Json.obj("filter" -> GameFilter.All.name))
    }

  private[controllers] val userShowRateLimit =
    env.security.ipTrust.rateLimit(10_000, 1.day, "user.show.ip", _.proxyMultiplier(2))

  def show(username: UserStr) = OpenBody:
    EnabledUser(username): u =>
      negotiate(
        renderShow(u),
        apiGames(u, GameFilter.All.name, 1)
      )

  private def renderShow(u: UserModel, status: Results.Status = Results.Ok)(using Context): Fu[Result] =
    if HTTPRequest isSynchronousHttp ctx.req
    then
      userShowRateLimit(rateLimited, cost = if env.socket.isOnline(u.id) then 2 else 3):
        for
          as     <- env.activity.read.recentAndPreload(u)
          nbs    <- env.userNbGames(u, withCrosstable = false)
          info   <- env.userInfo(u, nbs)
          _      <- env.userInfo.preloadTeams(info)
          social <- env.socialInfo(u)
          page <- renderPage:
            lila.mon.chronoSync(_.user segment "renderSync"):
              html.user.show.page.activity(as, info, social)
        yield status(page).withCanonical(routes.User.show(u.username))
    else
      for
        withPerfs <- env.user.perfsRepo.withPerfs(u)
        as        <- env.activity.read.recentAndPreload(u)
        page      <- renderPage(html.activity(withPerfs, as)).map(status(_))
      yield page

  def download(username: UserStr) = OpenBody:
    val userOption = if username.value == "me" then fuccess(ctx.user) else env.user.repo byId username
    FoundPage(userOption.dmap(_.filter(u => u.enabled.yes || ctx.is(u) || isGrantedOpt(_.GamesModView)))):
      html.user.download(_)

  def gamesAll(username: UserStr, page: Int) = games(username, GameFilter.All.name, page)

  def games(username: UserStr, filter: String, page: Int) = OpenBody:
    Reasonable(page):
      EnabledUser(username): u =>
        if filter == "search" && ctx.isAnon
        then
          negotiate(
            Unauthorized.page(html.search.login(u.count.game)),
            Unauthorized(jsonError("Login required"))
          )
        else
          negotiate(
            html = for
              nbs <- env.userNbGames(u, withCrosstable = true)
              filters = GameFilterMenu(u, nbs, filter, ctx.isAuth)
              pag <- env.gamePaginator(
                user = u,
                nbs = nbs.some,
                filter = filters.current,
                me = ctx.me,
                page = page
              )
              _ <- lightUserApi preloadMany pag.currentPageResults.flatMap(_.userIds)
              _ <- env.tournament.cached.nameCache preloadMany {
                pag.currentPageResults.flatMap(_.tournamentId).map(_ -> ctx.lang)
              }
              notes <- ctx.me.so: me =>
                env.round.noteApi.byGameIds(pag.currentPageResults.map(_.id), me)
              res <-
                if HTTPRequest.isSynchronousHttp(ctx.req) then
                  for
                    info   <- env.userInfo(u, nbs, withUblog = false)
                    _      <- env.team.cached.nameCache preloadMany info.teamIds
                    social <- env.socialInfo(u)
                    searchForm = (filters.current == GameFilter.Search) option
                      GameFilterMenu
                        .searchForm(userGameSearch, filters.current)
                    page <- renderPage:
                      html.user.show.page.games(info, pag, filters, searchForm, social, notes)
                  yield Ok(page)
                else Ok.page(html.user.show.gamesContent(u, nbs, pag, filters, filter, notes))
            yield res.withCanonical(routes.User.games(u.username, filters.current.name)),
            json = apiGames(u, filter, page)
          )

  private def EnabledUser(username: UserStr)(f: UserModel => Fu[Result])(using ctx: Context): Fu[Result] =
    if UserModel.isGhost(username.id)
    then
      negotiate(
        Ok.page(html.site.bits.ghost),
        notFoundJson("Deleted user")
      )
    else
      env.user.repo byId username flatMap {
        case None if isGrantedOpt(_.UserModView) =>
          ctx.me.soUse(modC.searchTerm(username.value))
        case None                                                    => notFound
        case Some(u) if u.enabled.yes || isGrantedOpt(_.UserModView) => f(u)
        case Some(u) =>
          negotiate(
            env.user.repo isErased u flatMap { erased =>
              if erased.yes then notFound
              else NotFound.page(html.user.show.page.disabled(u))
            },
            NotFound(jsonError("No such user, or account closed"))
          )
      }
  def showMini(username: UserStr) = Open:
    Found(env.user.api withPerfs username): user =>
      if user.enabled.yes || isGrantedOpt(_.UserModView)
      then
        ctx.userId.so(relationApi.fetchBlocks(user.id, _)) zip
          ctx.userId.soFu(env.game.crosstableApi(user.id, _)) zip
          ctx.isAuth.so(env.pref.api.followable(user.id)) zip
          ctx.userId.so(relationApi.fetchRelation(_, user.id)) flatMap {
            case (((blocked, crosstable), followable), relation) =>
              val ping = env.socket.isOnline(user.id) so UserLagCache.getLagRating(user.id)
              negotiate(
                html = !ctx.is(user) so currentlyPlaying(user.user) flatMap { pov =>
                  Ok.page(html.user.mini(user, pov, blocked, followable, relation, ping, crosstable))
                    .map(_.withHeaders(CACHE_CONTROL -> "max-age=5"))
                },
                json =
                  import lila.game.JsonView.given
                  Ok:
                    Json.obj(
                      "crosstable" -> crosstable,
                      "perfs"      -> lila.user.JsonView.perfsJson(user.perfs, user.perfs.best8Perfs)
                    )
              )
          }
      else Ok.page(html.user.bits.miniClosed(user.user))

  def online = Anon:
    val max = 50
    negotiateJson:
      env.user.cached.getTop50Online.map: users =>
        Ok:
          Json.toJson:
            users
              .take(getInt("nb").fold(10)(_ min max))
              .map: u =>
                env.user.jsonView.full(u.user, u.perfs.some, withProfile = true)

  def ratingHistory(username: UserStr) = OpenBody:
    EnabledUser(username): u =>
      env.history
        .ratingChartApi(u)
        .dmap(_ | "[]") // send an empty JSON array if no history JSON is available
        .dmap(jsonStr => Ok(jsonStr) as JSON)

  private def currentlyPlaying(user: UserModel): Fu[Option[Pov]] =
    env.game.cached.lastPlayedPlayingId(user.id) flatMapz {
      env.round.proxyRepo.pov(_, user)
    }

  private def lastPlayed(user: UserModel): Fu[Option[Pov]] =
    env.game.gameRepo
      .lastPlayed(user)
      .flatMap(_.soFu(env.round.proxyRepo.upgradeIfPresent))

  private val UserGamesRateLimitPerIP = lila.memo.RateLimit[IpAddress](
    credits = 500,
    duration = 10.minutes,
    key = "user_games.web.ip"
  )

  private def userGames(
      u: UserModel,
      filterName: String,
      page: Int
  )(using ctx: BodyContext[?]): Fu[Paginator[GameModel]] =
    UserGamesRateLimitPerIP(
      ctx.ip,
      fuccess(Paginator.empty[GameModel]),
      cost = page,
      msg = s"on ${u.username}"
    ):
      lila.mon.http.userGamesCost.increment(page.toLong)
      for
        pagFromDb <- env.gamePaginator(
          user = u,
          nbs = none,
          filter = GameFilterMenu.currentOf(GameFilterMenu.all, filterName),
          me = ctx.me,
          page = page
        )
        pag <- pagFromDb.mapFutureResults(env.round.proxyRepo.upgradeIfPresent)
        _ <- env.tournament.cached.nameCache.preloadMany:
          pag.currentPageResults.flatMap(_.tournamentId).map(_ -> ctx.lang)
        _ <- lightUserApi preloadMany pag.currentPageResults.flatMap(_.userIds)
      yield pag

  def list = Open:
    env.user.cached.top10.get {} flatMap { leaderboards =>
      negotiate(
        html = for
          nbAllTime      <- env.user.cached.top10NbGame.get {}
          tourneyWinners <- env.tournament.winners.all.map(_.top)
          topOnline      <- env.user.cached.getTop50Online
          _              <- lightUserApi preloadMany tourneyWinners.map(_.userId)
          page <- renderPage:
            html.user.list(tourneyWinners, topOnline, leaderboards, nbAllTime)
        yield Ok(page),
        json =
          given OWrites[UserModel.LightPerf] = OWrites(env.user.jsonView.lightPerfIsOnline)
          import lila.user.JsonView.leaderboardsWrites
          JsonOk(leaderboards)
      )
    }

  def apiList = Anon:
    env.user.cached.top10.get {} map { leaderboards =>
      import env.user.jsonView.lightPerfIsOnlineWrites
      import lila.user.JsonView.leaderboardsWrites
      JsonOk(leaderboards)
    }

  def topNb(nb: Int, perfKey: Perf.Key) = Open:
    Found(topNbUsers(nb, perfKey)): (users, perfType) =>
      negotiate(
        (nb == 200) so Ok.page(html.user.top(perfType, users)),
        topNbJson(users)
      )

  def topNbApi(nb: Int, perfKey: Perf.Key) = Anon:
    if nb == 1 && perfKey == Perf.Key("standard") then
      env.user.cached.top10.get {} map { leaderboards =>
        import env.user.jsonView.lightPerfIsOnlineWrites
        import lila.user.JsonView.leaderboardStandardTopOneWrites
        JsonOk(leaderboards)
      }
    else Found(topNbUsers(nb, perfKey)) { users => topNbJson(users._1) }

  private def topNbUsers(nb: Int, perfKey: Perf.Key) =
    PerfType(perfKey).soFu: perfType =>
      env.user.cached.top200Perf get perfType.id dmap {
        _.take(nb atLeast 1 atMost 200) -> perfType
      }

  private def topNbJson(users: List[UserModel.LightPerf]) =
    given OWrites[UserModel.LightPerf] = OWrites(env.user.jsonView.lightPerfIsOnline)
    Ok(Json.obj("users" -> users))

  def topWeek = Open:
    negotiateJson:
      env.user.cached.topWeek.map: users =>
        Ok(Json toJson users.map(env.user.jsonView.lightPerfIsOnline))

  def mod(username: UserStr) = Secure(_.UserModView) { ctx ?=> _ ?=>
    modZoneOrRedirect(username)
  }

  protected[controllers] def modZoneOrRedirect(username: UserStr)(using
      ctx: Context,
      me: Me
  ): Fu[Result] =
    if HTTPRequest isEventSource ctx.req then renderModZone(username)
    else modC.redirect(username)

  private def modZoneSegment(fu: Fu[Frag], name: String, user: UserModel): Source[Frag, ?] =
    Source.futureSource:
      fu.monSuccess(_.mod zoneSegment name)
        .logFailure(lila.log("modZoneSegment").branch(s"$name ${user.id}"))
        .map(Source.single)

  protected[controllers] def loginsTableData(
      user: UserModel,
      userLogins: UserLogins,
      max: Int
  )(using Context): Fu[UserLogins.TableData[UserWithModlog]] =
    val familyUserIds = user.id :: userLogins.otherUserIds
    for
      ((notes, bans), othersWithEmail) <-
        (isGrantedOpt(_.ModNote) so env.user.noteApi
          .byUsersForMod(familyUserIds)
          .logTimeIfGt(s"${user.username} noteApi.forMod", 2 seconds)) zip
          env.playban.api.bans(familyUserIds).logTimeIfGt(s"${user.username} playban.bans", 2 seconds) zip
          lila.security.UserLogins.withMeSortedWithEmails(env.user.repo, user, userLogins)
      otherUsers <- env.user.perfsRepo.withPerfs(othersWithEmail.others.map(_.user))
      otherUsers <- env.mod.logApi.addModlog(otherUsers)
      others = othersWithEmail.withUsers(otherUsers)
    yield UserLogins.TableData(userLogins, others, notes, bans, max)

  protected[controllers] def renderModZone(username: UserStr)(using
      ctx: Context,
      me: Me
  ): Fu[Result] =
    env.user.api withEmails username orFail s"No such user $username" flatMap {
      case UserModel.WithEmails(user, emails) =>
        withPageContext:
          import html.user.{ mod as view }
          import lila.app.ui.ScalatagsExtensions.{ emptyFrag, given }
          given lila.mod.IpRender.RenderIp = env.mod.ipRender.apply

          val nbOthers = getInt("nbOthers") | 100

          val modLog = for
            history <- env.mod.logApi.userHistory(user.id)
            appeal  <- isGranted(_.Appeals) so env.appeal.api.byId(user)
          yield view.modLog(history, appeal)

          val plan =
            isGranted(_.Admin) so env.plan.api
              .recentChargesOf(user)
              .map(view.plan(user))
              .dmap(_ | emptyFrag): Fu[Frag]

          val student = env.clas.api.student.findManaged(user).map2(view.student).dmap(~_)

          val reportLog = isGranted(_.SeeReport) so env.report.api
            .byAndAbout(user, 20)
            .flatMap: rs =>
              lightUserApi.preloadMany(rs.userIds) inject rs
            .map(view.reportLog(user))

          val prefs = isGranted(_.CheatHunter) so env.pref.api.get(user).map(view.prefs(user))

          val rageSit = isGranted(_.CheatHunter) so env.playban.api
            .getRageSit(user.id)
            .zip(env.playban.api.bans(user.id))
            .map(view.showRageSitAndPlaybans)

          val actions = env.user.repo.isErased(user) map { erased =>
            html.user.mod.actions(
              user,
              emails,
              erased,
              env.mod.presets.getPmPresets
            )
          }

          val userLoginsFu = env.security.userLogins(user, nbOthers)
          val others = for
            userLogins <- userLoginsFu
            appeals    <- env.appeal.api.byUserIds(user.id :: userLogins.otherUserIds)
            data       <- loginsTableData(user, userLogins, nbOthers)
          yield html.user.mod.otherUsers(me, user, data, appeals)

          val identification = userLoginsFu.map: logins =>
            Granter(_.ViewPrintNoIP) so html.user.mod.identification(logins)

          val kaladin = isGranted(_.MarkEngine) so env.irwin.kaladinApi.get(user).map {
            _.flatMap(_.response) so html.kaladin.report
          }

          val irwin =
            isGranted(_.MarkEngine) so env.irwin.irwinApi.reports.withPovs(user).mapz(html.irwin.report)
          val assess = isGranted(_.MarkEngine) so
            env.mod.assessApi.getPlayerAggregateAssessmentWithGames(user.id) flatMapz { as =>
              lightUserApi.preloadMany(as.games.flatMap(_.userIds)) inject html.user.mod.assessments(user, as)
            }

          val boardTokens = env.oAuth.tokenApi.usedBoardApi(user).map(html.user.mod.boardTokens)

          val teacher = env.clas.api.clas.countOf(user).map(html.user.mod.teacher(user))

          given EventSource.EventDataExtractor[Frag] = EventSource.EventDataExtractor[Frag](_.render)
          Ok.chunked:
            Source.single(html.user.mod.menu) merge
              modZoneSegment(actions, "actions", user) merge
              modZoneSegment(modLog, "modLog", user) merge
              modZoneSegment(plan, "plan", user) merge
              modZoneSegment(student, "student", user) merge
              modZoneSegment(teacher, "teacher", user) merge
              modZoneSegment(reportLog, "reportLog", user) merge
              modZoneSegment(prefs, "prefs", user) merge
              modZoneSegment(rageSit, "rageSit", user) merge
              modZoneSegment(others, "others", user) merge
              modZoneSegment(identification, "identification", user) merge
              modZoneSegment(kaladin, "kaladin", user) merge
              modZoneSegment(irwin, "irwin", user) merge
              modZoneSegment(assess, "assess", user) merge
              modZoneSegment(boardTokens, "boardTokens", user) via
              EventSource.flow log "User.renderModZone"
          .as(ContentTypes.EVENT_STREAM) pipe noProxyBuffer
    }

  protected[controllers] def renderModZoneActions(username: UserStr)(using ctx: Context) =
    env.user.api withEmails username orFail s"No such user $username" flatMap {
      case UserModel.WithEmails(user, emails) =>
        env.user.repo.isErased(user).flatMap { erased =>
          Ok.page:
            html.user.mod.actions(
              user,
              emails,
              erased,
              env.mod.presets.getPmPresetsOpt
            )
        }
    }

  def writeNote(username: UserStr) = AuthBody { ctx ?=> me ?=>
    lila.user.UserForm.note
      .bindFromRequest()
      .fold(
        err => BadRequest(err.errors.toString).toFuccess,
        data =>
          doWriteNote(username, data): user =>
            if getBool("inquiry") then
              Ok.pageAsync:
                env.user.noteApi.byUserForMod(user.id).map {
                  views.html.mod.inquiry.noteZone(user, _)
                }
            else
              Ok.pageAsync:
                env.socialInfo.fetchNotes(user) map {
                  views.html.user.show.header.noteZone(user, _)
                }
      )
  }

  def apiReadNote(username: UserStr) = Scoped() { _ ?=> me ?=>
    Found(env.user.repo byId username):
      env.socialInfo.fetchNotes(_) flatMap {
        lila.user.JsonView.notes(_)(using lightUserApi)
      } map JsonOk
  }

  def apiWriteNote(username: UserStr) = ScopedBody() { ctx ?=> me ?=>
    lila.user.UserForm.apiNote
      .bindFromRequest()
      .fold(doubleJsonFormError, data => doWriteNote(username, data)(_ => jsonOkResult))
  }

  private def doWriteNote(
      username: UserStr,
      data: lila.user.UserForm.NoteData
  )(f: UserModel => Fu[Result])(using Context, Me) =
    Found(env.user.repo byId username): user =>
      val isMod = data.mod && isGranted(_.ModNote)
      env.user.noteApi.write(user, data.text, isMod, isMod && data.dox) >> f(user)

  def deleteNote(id: String) = Auth { ctx ?=> me ?=>
    Found(env.user.noteApi.byId(id)): note =>
      (note.isFrom(me) && !note.mod) so {
        env.user.noteApi.delete(note._id) inject Redirect(routes.User.show(note.to).url + "?note")
      }
  }

  def setDoxNote(id: String, dox: Boolean) = Secure(_.Admin) { ctx ?=> _ ?=>
    Found(env.user.noteApi.byId(id)): note =>
      note.mod so {
        env.user.noteApi.setDox(note._id, dox) inject Redirect(routes.User.show(note.to).url + "?note")
      }
  }

  def opponents = Auth { ctx ?=> me ?=>
    getUserStr("u")
      .ifTrue(isGranted(_.BoostHunter))
      .so(env.user.repo.byId)
      .map(_ | me.value)
      .flatMap: user =>
        for
          usersAndGames <- env.game.favoriteOpponents(user.id)
          withPerfs     <- env.user.perfsRepo.withPerfs(usersAndGames.map(_._1))
          ops = withPerfs.toList zip usersAndGames.map(_._2)
          followables <- env.pref.api.followables(ops.map(_._1.id))
          relateds <-
            ops
              .zip(followables)
              .map { case ((u, nb), followable) =>
                relationApi.fetchRelation(user.id, u.id) map {
                  lila.relation.Related(u, nb.some, followable, _)
                }
              }
              .parallel
          page <- renderPage(html.relation.bits.opponents(user, relateds))
        yield Ok(page)
  }

  def perfStat(username: UserStr, perfKey: Perf.Key) = Open:
    Found(env.perfStat.api.data(username, perfKey)): data =>
      negotiate(
        Ok.pageAsync:
          env.history.ratingChartApi(data.user.user) map {
            html.user.perfStat(data, _)
          }
        ,
        JsonOk:
          getBool("graph")
            .soFu:
              env.history.ratingChartApi.singlePerf(data.user.user, data.stat.perfType)
            .map: graph =>
              env.perfStat.jsonView(data).add("graph", graph)
      )

  def autocomplete = OpenOrScoped(): ctx ?=>
    get("term").flatMap(UserSearch.read) match
      case None => BadRequest("No search term provided")
      case Some(term) if getBool("exists") =>
        UserModel.validateId(term into UserStr).so(env.user.repo.exists) map JsonOk
      case Some(term) =>
        {
          (get("tour"), get("swiss"), get("team")) match
            case (Some(tourId), _, _) => env.tournament.playerRepo.searchPlayers(TourId(tourId), term, 10)
            case (_, Some(swissId), _) =>
              env.swiss.api.searchPlayers(SwissId(swissId), term, 10)
            case (_, _, Some(teamId)) => env.team.api.searchMembersAs(TeamId(teamId), term, 10)
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
          if getBool("names") then
            lightUserApi.asyncMany(userIds) map { users =>
              Json toJson users.flatMap(_.map(_.name))
            }
          else if getBool("object") then
            lightUserApi.asyncMany(userIds) map { users =>
              Json.obj(
                "result" -> JsArray(users collect { case Some(u) =>
                  lila.common.LightUser.lightUserWrites
                    .writes(u)
                    .add("online" -> env.socket.isOnline(u.id))
                })
              )
            }
          else fuccess(Json toJson userIds)
        } map JsonOk

  def ratingDistribution(perfKey: Perf.Key, username: Option[UserStr] = None) = Open:
    Found(PerfType(perfKey).filter(PerfType.isLeaderboardable)): perfType =>
      env.user.rankingApi.weeklyRatingDistribution(perfType) flatMap: data =>
        WithMyPerfs:
          username match
            case Some(name) =>
              EnabledUser(name): u =>
                env.user.perfsRepo
                  .withPerfs(u)
                  .flatMap: u =>
                    Ok.page(html.stat.ratingDistribution(perfType, data, u.some))
            case _ => Ok.page(html.stat.ratingDistribution(perfType, data, none))

  def myself = Auth { _ ?=> me ?=>
    Redirect(routes.User.show(me.username))
  }

  def redirect(username: UserStr) = Open:
    staticRedirect(username.value) | {
      tryRedirect(username) getOrElse notFound
    }

  def tryRedirect(username: UserStr)(using Context): Fu[Option[Result]] =
    env.user.repo byId username map {
      _.filter(_.enabled.yes || isGrantedOpt(_.SeeReport)) map { user =>
        Redirect(routes.User.show(user.username))
      }
    }
