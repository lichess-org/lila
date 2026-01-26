package controllers

import akka.stream.scaladsl.*
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.libs.json.*
import play.api.mvc.*
import scalalib.paginator.Paginator

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.common.Json.given
import lila.core.user.LightPerf
import lila.core.userId.UserSearch
import lila.game.GameFilter
import lila.mod.UserWithModlog
import lila.rating.PerfType
import lila.rating.UserPerfsExt.best8Perfs
import lila.security.UserLogins
import lila.user.WithPerfsAndEmails

final class User(
    override val env: Env,
    roundC: => Round,
    gameC: => Game,
    modC: => Mod
) extends LilaController(env):

  import env.relation.api as relationApi
  import env.gameSearch.userGameSearch
  import env.user.lightUserApi

  def tv(username: UserStr) = Open:
    Found(meOrFetch(username)): user =>
      env.round.currentlyPlaying
        .exec(user.id)
        .orElse(env.round.lastPlayed(user.id))
        .flatMap:
          _.fold(fuccess(Redirect(routes.User.show(username)))): pov =>
            ctx.me.filterNot(_ => pov.game.bothPlayersHaveMoved).flatMap { Pov(pov.game, _) } match
              case Some(mine) => Redirect(routes.Round.player(mine.fullId))
              case _ => roundC.watch(pov, userTv = user.some)

  def tvExport(username: UserStr) = Anon:
    env.game.cached
      .lastPlayedPlayingId(username.id)
      .orElse(env.game.gameRepo.quickLastPlayedId(username.id))
      .flatMap:
        case None => NotFound("No ongoing game")
        case Some(gameId) => gameC.exportGame(gameId)

  private def apiGames(u: UserModel, filter: String, page: Int)(using BodyContext[?]) =
    userGames(u, filter, page).flatMap(env.game.userGameApi.jsPaginator).map { res =>
      Ok(res ++ Json.obj("filter" -> GameFilter.All.name))
    }

  private val userShowHtmlRateLimit =
    env.security.ipTrust.rateLimit(3_000, 1.day, "user.show.html.ip", _.antiScraping(dch = 10, others = 4))

  def show(username: UserStr) = OpenBody:
    EnabledUser(username): u =>
      negotiate(
        renderShow(u),
        apiGames(u, GameFilter.All.name, 1)
      )

  def search(term: String) = Open: _ ?=>
    UserStr.read(term) match
      case Some(username) => Redirect(routes.User.show(username)).toFuccess
      case _ if isGrantedOpt(_.UserSearch) => Redirect(s"${routes.Mod.search}?q=$term").toFuccess
      case _ => notFound

  private def renderShow(u: UserModel, status: Results.Status = Results.Ok)(using Context): Fu[Result] =
    WithProxy: proxy ?=>
      limit.enumeration.userProfile(rateLimited):
        def fetchActivity = (ctx.isAuth || !proxy.isFloodish).so(env.activity.read.recentAndPreload(u))
        if HTTPRequest.isSynchronousHttp(ctx.req)
        then
          val cost =
            if isGrantedOpt(_.UserModView) then 0
            else if env.socket.isOnline.exec(u.id) then 1
            else 2
          userShowHtmlRateLimit(rateLimited, cost = cost):
            for
              as <- fetchActivity
              nbs <- env.userNbGames(u, withCrosstable = false)
              info <- env.userInfo.fetch(u, nbs)
              _ <- env.userInfo.preloadTeams(info)
              social <- env.socialInfo(u)
              page <- renderPage:
                lila.mon.chronoSync(_.user.segment("renderSync")):
                  views.user.show.page.activity(as, info, social)
            yield status(page).withCanonical(routes.User.show(u.username))
        else
          for
            withPerfs <- env.user.perfsRepo.withPerfs(u)
            as <- fetchActivity
            snip = lila.ui.Snippet(views.activity(withPerfs, as))
          yield status(snip)

  def download(username: UserStr) = OpenBody:
    val user =
      meOrFetch(username).dmap(_.filter(u => u.enabled.yes || ctx.is(u) || isGrantedOpt(_.GamesModView)))
    FoundPage(user):
      views.user.download(_)

  def gamesAll(username: UserStr, page: Int) = games(username, GameFilter.All.name, page)

  def games(username: UserStr, filter: String, page: Int) = OpenBody:
    Reasonable(page):
      WithProxy: proxy ?=>
        limit.enumeration.userProfile(rateLimited):
          EnabledUser(username): u =>
            if filter == "search" && ctx.isAnon
            then
              negotiate(
                Unauthorized.page(views.gameSearch.login(u.count.game)),
                Unauthorized(jsonError("Login required"))
              )
            else
              negotiate(
                html = for
                  nbs <- env.userNbGames(u, withCrosstable = true)
                  filters = lila.app.mashup.GameFilterMenu(u, nbs, filter, ctx.isAuth)
                  pag <- env.gamePaginator(
                    user = u,
                    nbs = nbs.some,
                    filter = filters.current,
                    me = ctx.me,
                    page = page
                  )
                  _ <- lightUserApi.preloadMany(pag.currentPageResults.flatMap(_.userIds))
                  _ <- env.tournament.cached.nameCache.preloadMany {
                    pag.currentPageResults.flatMap((_: GameModel).tournamentId).map(tid => tid -> ctx.lang)
                  }
                  notes <- ctx.me.so: me =>
                    env.round.noteApi.byGameIds(pag.currentPageResults.map(_.id), me)
                  res <-
                    if HTTPRequest.isSynchronousHttp(ctx.req) then
                      for
                        info <- env.userInfo.fetch(u, nbs, withUblog = true)
                        _ <- env.team.cached.lightCache.preloadMany(info.teamIds)
                        social <- env.socialInfo(u)
                        searchForm = (filters.current == GameFilter.Search).option(
                          lila.app.mashup.GameFilterMenu.searchForm(userGameSearch, filters.current)
                        )
                        res <- Ok.page:
                          views.user.show.page.games(info, pag, filters, searchForm, social, notes)
                      yield res
                    else Ok.snip(views.user.show.gamesContent(u, nbs, pag, filters, filter, notes)).toFuccess
                yield res.withCanonical(routes.User.games(u.username, filters.current.name)),
                json = apiGames(u, filter, page)
              )

  private def EnabledUser(username: UserStr)(f: UserModel => Fu[Result])(using ctx: Context): Fu[Result] =
    if username.id.isGhost
    then
      negotiate(
        Ok.page(views.user.show.page.deleted(false)),
        notFoundJson("Deleted user")
      )
    else
      def notFound(canCreate: Boolean) = negotiate(
        NotFound.page(views.user.show.page.deleted(canCreate)),
        NotFound(jsonError("No such player, or account closed"))
      )
      meOrFetch(username).flatMap:
        case None =>
          env.api.anySearch
            .redirect(username.value)
            .flatMap:
              case Some(url) => Redirect(url).toFuccess
              case None if isGrantedOpt(_.UserModView) => ctx.me.soUse(modC.searchTerm(username.value))
              case None => notFound(true)
        case Some(u) if u.enabled.yes || isGrantedOpt(_.UserModView) => f(u)
        case u => notFound(u.isEmpty)

  def showMini(username: UserStr) = Open:
    Found(env.user.api.withPerfs(username)): user =>
      ctx.userId
        .so(relationApi.fetchRelation(_, user.id))
        .flatMap: relation =>
          if user.enabled.yes || isGrantedOpt(_.UserModView)
          then
            (
              ctx.userId.so(relationApi.fetchBlocks(user.id, _)),
              ctx.userId.traverse(env.game.crosstableApi(user.id, _)),
              ctx.isAuth.so(env.pref.api.followable(user.id)),
              ctx.me.soUse(env.clas.api.clas.realName(user.id))
            ).flatMapN: (blocked, crosstable, followable, realName) =>
              negotiate(
                html = for
                  pov <- ctx.isnt(user).so(env.round.currentlyPlaying.exec(user.user.id))
                  ping = env.socket.isOnline.exec(user.id).so(env.socket.getLagRating(user.id))
                  snip <- Ok.snip:
                    views.user.mini(user, pov, blocked, followable, relation, ping, crosstable, realName)
                yield snip.headerCacheSeconds(5),
                json =
                  import lila.game.JsonView.given
                  Ok:
                    Json.obj(
                      "crosstable" -> crosstable,
                      "perfs" -> lila.user.JsonView.perfsJson(user.perfs, user.perfs.best8Perfs)
                    )
              )
          else Ok(views.user.bits.miniClosed(user.user, relation))

  def online = Anon:
    val max = 50
    negotiateJson:
      env.user.cached.getTop50Online.map: users =>
        Ok:
          Json.toJson:
            users
              .take(getInt("nb").fold(10)(_.min(max)))
              .map: u =>
                env.user.jsonView.full(u.user, u.perfs.some, withProfile = true)

  def ratingHistory(username: UserStr) = Open:
    EnabledUser(username): u =>
      env.history
        .ratingChartApi(u)
        .dmap: // send an empty JSON array if no history JSON is available
          _ | lila.core.data.SafeJsonStr("[]")
        .dmap(jsonStr => Ok(jsonStr).as(JSON))

  private def userGames(
      u: UserModel,
      filterName: String,
      page: Int
  )(using ctx: BodyContext[?]): Fu[Paginator[GameModel]] =
    limit.userGames(
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
          filter = lila.app.mashup.GameFilterMenu.currentOf(GameFilter.all, filterName),
          me = ctx.me,
          page = page
        )
        pag <- pagFromDb.mapFutureResults(env.round.proxyRepo.upgradeIfPresent)
        _ <- env.tournament.cached.nameCache.preloadMany:
          pag.currentPageResults.flatMap(_.tournamentId).map(_ -> ctx.lang)
        _ <- lightUserApi.preloadMany(pag.currentPageResults.flatMap(_.userIds))
      yield pag

  def list = Open:
    env.user.cached.top10.get {}.flatMap { leaderboards =>
      negotiate(
        html = for
          nbAllTime <- env.user.cached.top10NbGame.get {}
          tourneyWinners <- env.tournament.winners.all.map(_.top)
          topOnline <- env.user.cached.getTop50Online
          _ <- lightUserApi.preloadMany(tourneyWinners.map(_.userId))
          page <- renderPage:
            views.user.list(tourneyWinners, topOnline, leaderboards, nbAllTime)
        yield Ok(page),
        json =
          given OWrites[LightPerf] = OWrites(env.user.jsonView.lightPerfIsOnline)
          import lila.user.JsonView.leaderboardsWrites
          JsonOk(leaderboards)
      )
    }

  def apiList = Anon:
    env.user.cached.top10.get {}.map { leaderboards =>
      import env.user.jsonView.lightPerfIsOnlineWrites
      import lila.user.JsonView.leaderboardsWrites
      JsonOk(leaderboards)
    }

  // redirect /player/top/200/:perfKey to /user/top/:perfKey
  // TODO move to a NotFound general handler?
  // to avoid adding (yet another) route
  def topBcRedirect(@annotation.unused nb: Int, perfKey: PerfKey) = Anon:
    Redirect(routes.User.top(perfKey))

  def top(perfKey: PerfKey, page: Int) = Open:
    Reasonable(page, Max(20)):
      env.user.cached
        .topPerfPager(perfKey, page)
        .flatMap: pager =>
          negotiate(
            Ok.page(views.user.list.top(perfKey, pager)),
            topNbJson(pager.currentPageResults)
          )

  def topNbApi(nb: Int, perfKey: PerfKey) = Anon:
    if nb == 1 && perfKey == PerfKey.standard then
      env.user.cached.top10.get {}.map { leaderboards =>
        import env.user.jsonView.lightPerfIsOnlineWrites
        import lila.user.JsonView.leaderboardStandardTopOneWrites
        JsonOk(leaderboards)
      }
    else env.user.cached.firstPageOf(perfKey).dmap(_.take(nb)).map(topNbJson)

  private def topNbJson(users: Seq[LightPerf]) =
    given OWrites[LightPerf] = OWrites(env.user.jsonView.lightPerfIsOnline)
    Ok(Json.obj("users" -> users))

  def mod(username: UserStr) = Secure(_.UserModView) { ctx ?=> _ ?=>
    modZoneOrRedirect(username)
  }

  protected[controllers] def modZoneOrRedirect(username: UserStr)(using
      ctx: Context,
      me: Me
  ): Fu[Result] =
    if HTTPRequest.isEventSource(ctx.req) then renderModZone(username)
    else modC.redirect(username)

  private def modZoneSegment(fu: Fu[Frag], name: String, user: UserModel): Source[Frag, ?] =
    Source.futureSource:
      fu.monSuccess(_.mod.zoneSegment(name))
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
        isGrantedOpt(_.ModNote)
          .so(
            env.user.noteApi
              .byUsersForMod(familyUserIds)
              .logTimeIfGt(s"${user.username} noteApi.forMod", 2.seconds)
          )
          .zip(env.playban.api.bansOf(familyUserIds).logTimeIfGt(s"${user.username} playban.bans", 2.seconds))
          .zip(lila.security.UserLogins.withMeSortedWithEmails(env.user.repo, user, userLogins))
      otherUsers <- env.user.perfsRepo.withPerfs(othersWithEmail.others.map(_.user))
      otherUsers <- env.mod.logApi.withModlogs(otherUsers)
      others = othersWithEmail.withUsers(otherUsers)
    yield UserLogins.TableData(userLogins, others, notes, bans, max)

  protected[controllers] def renderModZone(username: UserStr)(using ctx: Context, me: Me): Fu[Result] =
    env.report.api.inquiries
      .ofModId(me)
      .zip(env.user.api.withPerfsAndEmails(username).orFail(s"No such user $username"))
      .flatMap { case (inquiry, WithPerfsAndEmails(user, emails)) =>
        import views.mod.user as ui
        import lila.ui.ScalatagsExtensions.{ emptyFrag, given }
        given lila.mod.IpRender.RenderIp = env.mod.ipRender.apply

        val nbOthers = getInt("nbOthers") | 100

        val timeline = isGranted(_.AccountInfo).so[Fu[Frag]]:
          env.api.modTimeline
            .load(user, withPlayBans = true)
            .map: tl =>
              if inquiry.exists(_.isPlay)
              then views.mod.timeline.renderPlay(tl)
              else views.mod.timeline.renderGeneral(tl)
            .map(lila.mod.ui.mzSection("timeline")(_))

        val plan =
          isGranted(_.Admin).so(
            env.plan.api
              .recentChargesOf(user)
              .map(views.user.mod.plan(user))
              .dmap(_ | emptyFrag)
          ): Fu[Frag]

        val student = isGranted(_.AccountInfo).so:
          env.clas.api.student.findManaged(user).map2(views.user.mod.student).dmap(~_)

        val reportLog = isGranted(_.SeeReport).so:
          for
            reports <- env.report.api.by(user, Max(30))
            _ <- lightUserApi.preloadMany(reports.flatMap(_.userIds))
          yield ui.reportLog(user, reports)

        val prefs = isGranted(_.CheatHunter).so:
          env.pref.api
            .get(user)
            .map: prefs =>
              ui.prefs(user, prefs.hasKeyboardMove, prefs.hasVoice, prefs.botCompatible)

        val appeal = isGranted(_.Appeals).so:
          env.appeal.api.byId(user).mapz(views.appeal.ui.modSection(lila.mod.ui.mzSection("appeal")))

        val rageSit = isGranted(_.CheatHunter).so:
          env.playban.api
            .rageSitOf(user.id)
            .zip(env.playban.api.bans(user.id))
            .map(ui.showRageSitAndPlaybans)

        val actions = env.user.repo.isDeleted(user).map { deleted =>
          ui.actions(user, emails, deleted, env.mod.presets.getPmPresets)
        }

        val userLoginsFu = env.security.userLogins(user, nbOthers)
        val othersAndLogins = for
          userLogins <- userLoginsFu
          appeals <- env.appeal.api.byUserIds(user.id :: userLogins.otherUserIds)
          data <- loginsTableData(user, userLogins, nbOthers)
          render = () => views.user.mod.otherUsers(user, data, appeals)
        yield (render, data)

        val otherUsers = isGranted(_.ViewPrintNoIP).so[Fu[Frag]]:
          othersAndLogins.map(_._1())

        val identification = (isGranted(_.AccountInfo) || isGranted(_.ViewPrintNoIP)).so:
          for
            logins <- userLoginsFu
            others <- othersAndLogins
          yield views.user.mod.identification(logins, others._2.othersPartiallyLoaded)

        val kaladin = isGranted(_.MarkEngine).so:
          env.irwin.kaladinApi.get(user).map(_.flatMap(_.response).so(views.irwin.kaladin.report))

        val irwin = isGranted(_.MarkEngine).so:
          env.irwin.irwinApi.reports.withPovs(user).mapz(views.irwin.report)

        val assess = isGranted(_.MarkEngine)
          .so(env.mod.assessApi.getPlayerAggregateAssessmentWithGames(user.id))
          .flatMapz: as =>
            lightUserApi
              .preloadMany(as.games.flatMap(_.userIds))
              .inject(ui.assessments(user, as))

        val boardTokens = env.oAuth.tokenApi.usedBoardApi(user.id).map(views.user.mod.boardTokens)

        val teacher = isGranted(_.AccountInfo).so:
          env.clas.api.clas.countOf(user).map(ui.teacher(user))

        given EventSource.EventDataExtractor[Frag] = EventSource.EventDataExtractor[Frag](_.render)
        Ok.chunked:
          Source
            .single(ui.menu)
            .merge(modZoneSegment(actions, "actions", user))
            .merge(modZoneSegment(reportLog, "reportLog", user))
            .merge(modZoneSegment(timeline, "timeline", user))
            .merge(modZoneSegment(plan, "plan", user))
            .merge(modZoneSegment(student, "student", user))
            .merge(modZoneSegment(teacher, "teacher", user))
            .merge(modZoneSegment(prefs, "prefs", user))
            .merge(modZoneSegment(appeal, "appeal", user))
            .merge(modZoneSegment(rageSit, "rageSit", user))
            .merge(modZoneSegment(otherUsers, "others", user))
            .merge(modZoneSegment(identification, "identification", user))
            .merge(modZoneSegment(kaladin, "kaladin", user))
            .merge(modZoneSegment(irwin, "irwin", user))
            .merge(modZoneSegment(assess, "assess", user))
            .merge(modZoneSegment(boardTokens, "boardTokens", user))
            .via(EventSource.flow)
            .log("User.renderModZone")
        .as(ContentTypes.EVENT_STREAM)
          .noProxyBuffer
      }

  protected[controllers] def renderModZoneActions(username: UserStr)(using Context, Me) =
    env.user.api
      .withPerfsAndEmails(username)
      .orFail(s"No such user $username")
      .flatMap:
        case WithPerfsAndEmails(user, emails) =>
          env.user.repo.isDeleted(user).flatMap { deleted =>
            Ok.snip:
              views.mod.user.actions(
                user,
                emails,
                deleted,
                env.mod.presets.getPmPresets
              )
          }

  def writeNote(username: UserStr) = AuthBody { ctx ?=> me ?=>
    bindForm(lila.user.UserForm.note)(
      err => BadRequest(err.errors.toString).toFuccess,
      data =>
        doWriteNote(username, data): user =>
          if getBool("inquiry") then
            Ok.snipAsync:
              env.user.noteApi.toUserForMod(user.id).map {
                views.mod.inquiryUi.noteZone(user, _)
              }
          else
            Ok.snipAsync:
              env.user.noteApi.getForMyPermissions(user).map {
                views.user.noteUi.zone(user, _)
              }
    )
  }

  def apiReadNote(username: UserStr) = Scoped() { _ ?=> me ?=>
    Found(meOrFetch(username)):
      env.user.noteApi
        .getForMyPermissions(_)
        .flatMap:
          lila.user.JsonView.notes(_)(using lightUserApi)
        .map(JsonOk)
  }

  def apiWriteNote(username: UserStr) = ScopedBody() { ctx ?=> me ?=>
    bindForm(lila.user.UserForm.apiNote)(
      doubleJsonFormError,
      data => doWriteNote(username, data)(_ => jsonOkResult)
    )
  }

  private def doWriteNote(
      username: UserStr,
      data: lila.user.UserForm.NoteData
  )(f: UserModel => Fu[Result])(using Context, Me) =
    Found(meOrFetch(username)): user =>
      val isMod = data.mod && isGranted(_.ModNote)
      for
        _ <- env.user.noteApi.write(user.id, data.text, isMod, dox = isMod && data.dox)
        result <- f(user)
      yield result

  def deleteNote(id: String) = Auth { ctx ?=> me ?=>
    Found(env.user.noteApi.byId(id)): note =>
      (note.isFrom(me) && !note.mod).so:
        env.user.noteApi.delete(note._id).inject(Redirect(routes.User.show(note.to).url + "?note"))
  }

  def setDoxNote(id: String, dox: Boolean) = Secure(_.Admin) { ctx ?=> _ ?=>
    Found(env.user.noteApi.byId(id)): note =>
      note.mod.so:
        env.user.noteApi.setDox(note._id, dox).inject(Redirect(routes.User.show(note.to).url + "?note"))
  }

  def opponents = Auth { ctx ?=> me ?=>
    val user = meOrFetch(getUserStr("u")).map(_.filter(u => ctx.is(u) || isGrantedOpt(_.BoostHunter)))
    Found(user): user =>
      for
        usersAndGames <- env.game.favoriteOpponents(user.id)
        withPerfs <- env.user.api.listWithPerfs(usersAndGames._1F, includeClosed = false)
        ops = withPerfs.toList.zip(usersAndGames._2F)
        followables <- env.pref.api.followables(ops.map(_._1.id))
        relateds <-
          ops
            .zip(followables)
            .sequentially { case ((u, nb), followable) =>
              relationApi
                .fetchRelation(user.id, u.id)
                .map:
                  lila.relation.Related(u, nb.some, followable, _)
            }
        page <- renderPage(views.relation.opponents(user, relateds))
      yield Ok(page)
  }

  def perfStat(username: UserStr, perfKey: PerfKey) = Open:
    Found(env.perfStat.api.data(username, perfKey, computeIfNeeded = HTTPRequest.isCrawler(req).no)): data =>
      negotiate(
        Ok.async:
          env.history
            .ratingChartApi(data.user.user)
            .map:
              views.user.perfStatPage(data, _)
        ,
        JsonOk:
          getBool("graph")
            .optionFu:
              env.history.ratingChartApi.singlePerf(data.user.user, data.stat.perfType.key)
            .map: graph =>
              env.perfStat.jsonView(data).add("graph", graph)
      )

  def autocomplete = OpenOrScoped(): ctx ?=>
    NoTor:
      get("term").flatMap(UserSearch.read) match
        case None => BadRequest("No search term provided")
        case Some(term) if getBool("exists") =>
          term.into(UserStr).validateId.so(env.user.repo.existsSec).map(JsonOk)
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
                    env.relation.api.searchFollowedBy(follower, term, 10).flatMap { userIds =>
                      val remaining = 10 - userIds.length
                      if remaining > 0 then
                        env.user.cached.userIdsLike(term).map { extraUserIds =>
                          userIds ++ (extraUserIds.diff(userIds)).take(remaining)
                        }
                      else fuccess(userIds)
                    }
                  case None if getBool("teacher") =>
                    env.user.repo.userIdsLikeWithRole(term, lila.core.perm.Permission.Teacher.dbKey)
                  case None =>
                    for
                      found <- env.user.cached.userIdsLike(term)
                      closed <- isGrantedOpt(_.AccountInfo).so(env.user.repo.userIdsLikeClosed(term))
                    yield found ::: closed
          }.flatMap { userIds =>
            if getBool("names") then
              lightUserApi
                .asyncMany(userIds)
                .map: users =>
                  Json.toJson(users.flatMap(_.map(_.name)))
            else if getBool("object") then
              lightUserApi
                .asyncMany(userIds)
                .map: users =>
                  Json.obj:
                    "result" -> JsArray(users.collect { case Some(u) =>
                      lila.common.Json.lightUser
                        .write(u)
                        .add("online" -> env.socket.isOnline.exec(u.id))
                    })
            else fuccess(Json.toJson(userIds))
          }.map(JsonOk)

  def ratingDistribution(perfKey: PerfKey, username: Option[UserStr] = None) = Open:
    Found(perfKey.some.filter(lila.rating.PerfType.isLeaderboardable)): perfKey =>
      env.perfStat.api
        .weeklyRatingDistribution(perfKey)
        .flatMap: data =>
          WithMyPerfs:
            username match
              case Some(name) =>
                EnabledUser(name): u =>
                  env.user.perfsRepo
                    .withPerfs(u)
                    .flatMap: u =>
                      Ok.page(views.user.perfStat.ratingDistribution(perfKey, data, u.some))
              case _ => Ok.page(views.user.perfStat.ratingDistribution(perfKey, data, none))

  def myself = Auth { _ ?=> me ?=>
    Redirect(routes.User.show(me.username))
  }

  def redirect(path: String) = Open:
    staticRedirect(path) |
      UserStr.read(path).so(tryRedirect).getOrElse(notFound)

  def tryRedirect(username: UserStr)(using Context): Fu[Option[Result]] =
    meOrFetch(username).map:
      _.filter(_.enabled.yes || isGrantedOpt(_.SeeReport)).map: user =>
        Redirect(routes.User.show(user.username))
