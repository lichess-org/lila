package controllers
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.core.team.LightTeam
import lila.swiss.Swiss.ChatFor
import lila.swiss.{ Swiss as SwissModel, SwissForm }

final class Swiss(
    env: Env,
    tourC: Tournament,
    apiC: Api
)(using akka.stream.Materializer)
    extends LilaController(env):

  private def swissNotFound(using Context) = NotFound.page(views.swiss.ui.notFound)

  def home = Open(serveHome)
  def homeLang = LangPage(routes.Swiss.home)(serveHome)

  private def serveHome(using Context) = NoBot:
    for
      teamIds <- ctx.userId.so(env.team.cached.teamIdsList)
      swiss <- env.swiss.feature.get(teamIds)
      _ <- env.team.lightTeamApi.preload(swiss.teamIds)
      page <- renderPage(views.swiss.home.page(swiss))
    yield Ok(page)

  def show(id: SwissId) = Open:
    cachedSwissAndTeam(id).flatMap: swissOption =>
      val page = getInt("page").filter(0.<)
      swissOption.foreach((s, _) => env.swiss.api.maybeRecompute(s))
      negotiate(
        html = swissOption.fold(swissNotFound): (swiss, team) =>
          for
            verdicts <- env.swiss.api.verdicts(swiss)
            version <- env.swiss.version(swiss.id)
            isInTeam <- isUserInTheTeam(swiss.teamId)
            json <- env.swiss.json(
              swiss = swiss,
              me = ctx.me,
              verdicts = verdicts,
              reqPage = page,
              socketVersion = version.some,
              playerInfo = none,
              isInTeam = isInTeam
            )
            canChat <- canHaveChat(swiss.roundInfo)
            chat <- canChat.optionFu:
              env.chat.api.userChat.cached
                .findMine(swiss.id.into(ChatId))
                .map:
                  _.copy(locked = !env.api.chatFreshness.of(swiss))
            streamers <- streamerCache.get(swiss.id)
            isLocalMod <- ctx.useMe(env.team.api.hasCommPerm(swiss.teamId))
            page <- renderPage(views.swiss.show(swiss, team, verdicts, json, chat, streamers, isLocalMod))
          yield Ok(page),
        json = swissOption.fold[Fu[Result]](notFoundJson("No such Swiss tournament")): (swiss, _) =>
          for
            isInTeam <- isUserInTheTeam(swiss.teamId)
            verdicts <- env.swiss.api.verdicts(swiss)
            socketVersion <- getBool("socketVersion").optionFu(env.swiss.version(swiss.id))
            playerInfo <- getUserStr("playerInfo").so: u =>
              env.swiss.api.playerInfo(swiss, u.id)
            json <- env.swiss.json(
              swiss = swiss,
              me = ctx.me,
              verdicts = verdicts,
              reqPage = page,
              socketVersion = socketVersion,
              playerInfo = playerInfo,
              isInTeam = isInTeam
            )
          yield JsonOk(json)
      )

  def apiShow(id: SwissId) = Anon:
    env.swiss.cache.swissCache
      .byId(id)
      .flatMap:
        case Some(swiss) => JsonOk(apiJson(swiss))
        case _ => notFoundJson()

  private def apiJson(swiss: SwissModel)(using Context) =
    for
      verdicts <- env.swiss.api.verdicts(swiss)
      json <- env.swiss.json.api(swiss, verdicts)
    yield json

  private def isUserInTheTeam(teamId: lila.team.TeamId)(using me: Option[MyId]) =
    me.soUse(env.team.cached.isMember(teamId))

  private def cachedSwissAndTeam(id: SwissId): Fu[Option[(SwissModel, LightTeam)]] =
    env.swiss.cache.swissCache
      .byId(id)
      .flatMap:
        _.so: swiss =>
          env.team.lightTeam(swiss.teamId).map2(swiss -> _)

  def round(id: SwissId, round: Int) = Open:
    Found(cachedSwissAndTeam(id)): (swiss, team) =>
      (round > 0 && round <= swiss.round.value)
        .option(lila.swiss.SwissRoundNumber(round))
        .so: r =>
          val page = getInt("page").filter(0.<)
          env.swiss
            .roundPager(swiss, r, page | 0)
            .flatMap: pager =>
              Ok.page(views.swiss.showUi.round(swiss, r, team, pager))

  private def CheckTeamLeader(teamId: TeamId)(f: => Fu[Result])(using ctx: Context): Fu[Result] =
    ctx.useMe(env.team.api.isGranted(teamId, _.Tour)).elseNotFound(f)

  def form(teamId: TeamId) = Auth { ctx ?=> me ?=>
    NoLameOrBot:
      CheckTeamLeader(teamId):
        Ok.page(views.swiss.form.create(env.swiss.forms.create(me), teamId))
  }

  def create(teamId: TeamId) = AuthBody { ctx ?=> me ?=>
    NoLameOrBot:
      CheckTeamLeader(teamId):
        bindForm(env.swiss.forms.create(me))(
          err => BadRequest.page(views.swiss.form.create(err, teamId)),
          data =>
            tourC.rateLimitCreation(isPrivate = true, Redirect(routes.Team.show(teamId))):
              for
                swiss <- env.swiss.api.create(data, teamId)
                _ <- env.api.clas.onSwissCreate(swiss)
              yield Redirect(routes.Swiss.show(swiss.id))
        )
  }

  def apiCreate(teamId: TeamId) = ScopedBody(_.Tournament.Write) { ctx ?=> me ?=>
    if me.isBot || me.lame then notFoundJson("This account cannot create tournaments")
    else
      env.team.api
        .isGranted(teamId, _.Tour)
        .flatMap:
          if _ then
            bindForm(env.swiss.forms.create(me))(
              doubleJsonFormError,
              data =>
                tourC.rateLimitCreation(isPrivate = true, rateLimited):
                  JsonOk(env.swiss.api.create(data, teamId).flatMap(apiJson))
            )
          else notFoundJson("You're not a leader of that team")

  }

  def apiTerminate(id: SwissId) = ScopedBody(_.Tournament.Write) { _ ?=> me ?=>
    Found(env.swiss.cache.swissCache.byId(id)):
      case swiss if swiss.createdBy.is(me) || isGranted(_.ManageTournament) =>
        env.swiss.api
          .kill(swiss)
          .map(_ => jsonOkResult)
      case _ => BadRequest(jsonError("Can't terminate that tournament: Permission denied"))
  }

  def join(id: SwissId) = AuthOrScopedBody(_.Tournament.Write) { _ ?=> _ ?=>
    NoLameOrBot:
      doJoin(id, bodyPassword)
  }

  private def bodyPassword(using Request[?]) =
    bindForm(SwissForm.joinForm)(_ => none, identity)

  private def doJoin(id: SwissId, password: Option[String])(using ctx: Context, me: Me) =
    limit.tourJoinOrResume(me, rateLimited):
      for
        teamIds <- env.team.cached.teamIds(me)
        error <- env.swiss.api.join(id, teamIds.contains, password)
      yield error.fold(jsonOkResult)(err => JsonBadRequest(jsonError(err)))

  def withdraw(id: SwissId) = AuthOrScoped(_.Tournament.Write) { ctx ?=> me ?=>
    env.swiss.api.withdraw(id, me) >>
      negotiate(Redirect(routes.Swiss.show(id)), jsonOkResult)
  }

  def edit(id: SwissId) = Auth { ctx ?=> me ?=>
    WithEditableSwiss(id): swiss =>
      Ok.page(views.swiss.form.edit(swiss, env.swiss.forms.edit(me, swiss)))
  }

  def update(id: SwissId) = AuthBody { ctx ?=> me ?=>
    WithEditableSwiss(id): swiss =>
      bindForm(env.swiss.forms.edit(me, swiss))(
        err => BadRequest.page(views.swiss.form.edit(swiss, err)),
        data => for _ <- env.swiss.api.update(swiss.id, data) yield Redirect(routes.Swiss.show(id))
      )
  }

  def apiUpdate(id: SwissId) = ScopedBody(_.Tournament.Write) { req ?=> me ?=>
    WithEditableSwiss(id): swiss =>
      bindForm(env.swiss.forms.edit(me, swiss))(
        err => jsonFormError(err),
        data =>
          env.swiss.api.update(swiss.id, data) >>
            FoundOk(env.swiss.api.update(swiss.id, data))(apiJson)
      )
  }

  def scheduleNextRound(id: SwissId) =
    AuthOrScopedBody(_.Tournament.Write) { ctx ?=> me ?=>
      WithEditableSwiss(id): swiss =>
        bindForm(env.swiss.forms.nextRound)(
          err => negotiate(Redirect(routes.Swiss.show(id)), jsonFormError(err)),
          date =>
            env.swiss.api.scheduleNextRound(swiss, date) >>
              negotiate(Redirect(routes.Swiss.show(id)), NoContent)
        )
    }

  def terminate(id: SwissId) = Auth { _ ?=> me ?=>
    WithEditableSwiss(id): swiss =>
      env.swiss.api.kill(swiss).inject(Redirect(routes.Team.show(swiss.teamId)))
  }

  def standing(id: SwissId, page: Int) = Anon:
    WithSwiss(id): swiss =>
      JsonOk:
        env.swiss.standingApi(swiss, page)

  def pageOf(id: SwissId, userId: UserStr) = Anon:
    WithSwiss(id): swiss =>
      Found(env.swiss.api.pageOf(swiss, userId.id)): page =>
        JsonOk:
          env.swiss.standingApi(swiss, page)

  def player(id: SwissId, userId: UserStr) = Anon:
    WithSwiss(id): swiss =>
      Found(env.swiss.api.playerInfo(swiss, userId.id)): player =>
        JsonOk(lila.swiss.SwissJson.playerJsonExt(swiss, player))

  def exportTrf(id: SwissId) = Anon:
    env.swiss.cache.swissCache
      .byId(id)
      .map:
        case None => NotFound("Tournament not found")
        case Some(swiss) =>
          Ok.chunked(env.swiss.trf(swiss, sorted = true).intersperse("\n"))
            .asAttachmentStream(env.api.gameApiV2.filename(swiss, "trf"))

  def byTeam(id: TeamId) = Anon:
    apiC.jsonDownload:
      val status = get("status").flatMap(lila.swiss.Swiss.status)
      env.swiss.api
        .byTeamCursor(id, status, getAs[UserStr]("createdBy"), get("name"))
        .documentSource(getInt("max") | 100)
        .mapAsync(4)(apiJson)
        .throttle(20, 1.second)

  private def WithSwiss(id: SwissId)(f: SwissModel => Fu[Result])(using Context): Fu[Result] =
    env.swiss.cache.swissCache.byId(id).orNotFound(f)

  private def WithEditableSwiss(
      id: SwissId,
      fallback: SwissModel => Context ?=> Fu[Result] = swiss =>
        negotiate(
          Redirect(routes.Swiss.show(swiss.id)),
          Unauthorized(jsonError("This user cannot edit this swiss"))
        )
  )(f: SwissModel => Fu[Result])(using me: Me)(using Context): Fu[Result] =
    WithSwiss(id): swiss =>
      if swiss.createdBy.is(me) && !swiss.isFinished then f(swiss)
      else if isGranted(_.ManageTournament) then f(swiss)
      else fallback(swiss)

  private[controllers] def canHaveChat(swiss: SwissModel.RoundInfo)(using ctx: Context): Fu[Boolean] =
    (ctx.kid.no && ctx.noBot && HTTPRequest.isHuman(ctx.req)).so:
      swiss.chatFor match
        case ChatFor.NONE => fuFalse
        case _ if isGrantedOpt(_.ChatTimeout) => fuTrue
        case ChatFor.LEADERS => ctx.useMe(env.team.api.isLeader(swiss.teamId))
        case ChatFor.MEMBERS => ctx.useMe(env.team.api.isMember(swiss.teamId))
        case _ => fuTrue

  private val streamerCache =
    env.memo.cacheApi[SwissId, List[UserId]](64, "swiss.streamers"):
      _.refreshAfterWrite(15.seconds)
        .maximumSize(64)
        .buildAsyncFuture: id =>
          env.streamer.liveApi.all.flatMap: streams =>
            env.swiss.api.filterPlaying(id, streams.streams.map(_.streamer.userId))
