package controllers

import akka.stream.scaladsl.*
import play.api.libs.json.*
import play.api.mvc.*

import lila.api.GameApiV2
import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.common.Json.given
import lila.core.chess.MultiPv
import lila.core.net.IpAddress
import lila.core.{ LightUser, id }
import lila.gathering.Condition.GetMyTeamIds
import lila.security.Mobile

final class Api(
    env: Env,
    gameC: => Game,
    userC: => User
) extends LilaController(env):

  import Api.*
  import env.api.{ userApi, gameApi }

  private lazy val apiStatusJson = Json.obj:
    "api" -> Json.obj(
      "current" -> Mobile.Api.currentVersion.value,
      "olds"    -> Json.arr()
    )

  private given lila.core.team.LightTeam.Api = env.team.lightTeamApi

  val status = Anon:
    val appVersion  = get("v")
    val mustUpgrade = appVersion.exists(Mobile.AppVersion.mustUpgrade)
    JsonOk(apiStatusJson.add("mustUpgrade", mustUpgrade))

  def index = Anon:
    Ok.snip(views.bits.api)

  def user(name: UserStr) = OpenOrScoped(): ctx ?=>
    userC.userShowRateLimit(rateLimited, cost = if env.socket.isOnline.exec(name.id) then 1 else 2):
      userApi
        .extended(
          name,
          withFollows = userWithFollows,
          withTrophies = getBool("trophies"),
          withCanChallenge = getBool("challenge")
        )
        .map(toApiResult)
        .map(toHttp)

  private[controllers] def userWithFollows(using req: RequestHeader) =
    HTTPRequest.apiVersion(req).exists(_.value < 6) && !getBool("noFollows")

  def usersByIds = AnonBodyOf(parse.tolerantText): body =>
    val usernames = body.replace("\n", "").split(',').take(300).flatMap(UserStr.read).toList
    val cost      = usernames.size / 4
    limit.apiUsers(req.ipAddress, rateLimited, cost = cost):
      lila.mon.api.users.increment(cost.toLong)
      env.user.api
        .listWithPerfs(usernames)
        .map:
          _.map { u => env.user.jsonView.full(u.user, u.perfs.some, withProfile = true) }
        .map(toApiResult)
        .map(toHttp)

  def usersStatus = ApiRequest:
    val ids = get("ids").so(_.split(',').take(100).toList.flatMap(UserStr.read).map(_.id))
    if ids.isEmpty then fuccess(ApiResult.ClientError("No user ids provided"))
    else
      val withSignal = getBool("withSignal")
      env.user.lightUserApi.asyncMany(ids).dmap(_.flatten).flatMap { users =>
        val streamingIds = env.streamer.liveStreamApi.userIds
        def toJson(u: LightUser) =
          lila.common.Json.lightUser
            .write(u)
            .add("online", env.socket.isOnline.exec(u.id))
            .add("playing", env.round.playing(u.id))
            .add("streaming", streamingIds(u.id))
            .add("signal", withSignal.so(env.socket.getLagRating(u.id)))
        def gameIds: Fu[List[Option[id.GameId]]] = users.sequentially: u =>
          env.round.playing(u.id).so(env.game.cached.lastPlayedPlayingId(u.id))
        val extensions: Option[Fu[List[Update[JsObject]]]] =
          if getBool("withGameIds")
          then
            gameIds
              .map:
                _.map: gameId =>
                  (_: JsObject).add("playingId", gameId)
              .some
          else if getBool("withGameMetas")
          then
            gameIds
              .flatMap:
                _.sequentially(_.so(env.round.proxyRepo.gameIfPresent))
              .map:
                _.map: game =>
                  (_: JsObject).add(
                    "playing",
                    game.map: g =>
                      Json
                        .obj("id" -> g.id)
                        .add("clock", g.clock.map(_.config.show))
                        .add("variant", g.variant.exotic.option(g.variant.key))
                  )
              .some
          else none
        extensions
          .fold(fuccess(users.map(toJson))):
            _.map: exts =>
              users
                .zip(exts)
                .map: (u, ext) =>
                  ext(toJson(u))
          .map(toApiResult)
      }

  private def gameFlagsFromRequest(using RequestHeader) =
    lila.api.GameApi.WithFlags(
      analysis = getBool("with_analysis"),
      moves = getBool("with_moves"),
      fens = getBool("with_fens"),
      opening = getBool("with_opening"),
      moveTimes = getBool("with_movetimes"),
      token = get("token")
    )

  def game(id: GameId) = ApiRequest:
    gameApi.one(id, gameFlagsFromRequest).map(toApiResult)

  def crosstable(name1: UserStr, name2: UserStr) = ApiRequest:
    limit.crosstable(req.ipAddress, fuccess(ApiResult.Limited), cost = 1):
      val (u1, u2) = (name1.id, name2.id)
      env.game.crosstableApi(u1, u2).flatMap { ct =>
        (ct.results.nonEmpty && getBool("matchup"))
          .so:
            env.game.crosstableApi.getMatchup(u1, u2)
          .map: matchup =>
            toApiResult:
              lila.game.JsonView.crosstable(ct, matchup).some
      }

  def currentTournaments = ApiRequest:
    env.tournament.api.fetchVisibleTournaments
      .flatMap(env.tournament.apiJsonView.apply)
      .map(ApiResult.Data.apply)

  def tournament(id: TourId) = AnonOrScoped(): _ ?=>
    env.tournament.tournamentRepo
      .byId(id)
      .flatMapz: tour =>
        val page           = (getInt("page") | 1).atLeast(1).atMost(200)
        given GetMyTeamIds = me => env.team.cached.teamIdsList(me.userId)
        env.tournament
          .jsonView(
            tour = tour,
            page = page.some,
            playerInfoExt = none,
            socketVersion = none,
            partial = false,
            withScores = true,
            withAllowList = true
          )
          .dmap(some)
      .map(toApiResult)
      .map(toHttp)

  def tournamentGames(id: TourId) = AnonOrScoped(): ctx ?=>
    env.tournament.tournamentRepo.byId(id).orNotFound { tour =>
      val onlyUserId = getUserStr("player").map(_.id)
      val config = GameApiV2.ByTournamentConfig(
        tour = tour,
        format = GameApiV2.Format.byRequest,
        flags = gameC.requestPgnFlags(extended = false),
        perSecond = gamesPerSecond(ctx.me)
      )
      GlobalConcurrencyLimitPerIP
        .download(ctx.req.ipAddress)(env.api.gameApiV2.exportByTournament(config, onlyUserId)): source =>
          Ok.chunked(source)
            .pipe(asAttachmentStream(env.api.gameApiV2.filename(tour, config.format)))
            .as(gameC.gameContentType(config))
    }

  def tournamentResults(id: TourId) = Anon:
    val csv = HTTPRequest.acceptsCsv(req) || get("as").has("csv")
    env.tournament.tournamentRepo.byId(id).orNotFound { tour =>
      val withSheet = getBool("sheet")
      val perSecond = MaxPerSecond:
        if withSheet
        then (20 - (tour.estimateNumberOfGamesOneCanPlay / 20).toInt).atLeast(10)
        else 50
      val source = env.tournament.api
        .resultStream(
          tour,
          perSecond,
          getInt("nb") | Int.MaxValue,
          withSheet = withSheet
        )
      val result =
        if csv then csvDownload(lila.tournament.TournamentCsv(source))
        else jsonDownload(source.map(lila.tournament.JsonView.playerResultWrites.writes))
      result.pipe(asAttachment(env.api.gameApiV2.filename(tour, if csv then "csv" else "ndjson")))
    }

  def tournamentTeams(id: TourId) = Anon:
    env.tournament.tournamentRepo.byId(id).orNotFound { tour =>
      env.tournament.jsonView.apiTeamStanding(tour).map { arr =>
        JsonOk:
          Json.obj(
            "id"    -> tour.id,
            "teams" -> arr
          )
      }
    }

  def swissGames(id: SwissId) = AnonOrScoped(): ctx ?=>
    Found(env.swiss.cache.swissCache.byId(id)): swiss =>
      val config = GameApiV2.BySwissConfig(
        swissId = swiss.id,
        format = GameApiV2.Format.byRequest,
        flags = gameC.requestPgnFlags(extended = false),
        perSecond = gamesPerSecond(ctx.me),
        player = getUserStr("player").map(_.id)
      )
      GlobalConcurrencyLimitPerIP
        .download(req.ipAddress)(env.api.gameApiV2.exportBySwiss(config)): source =>
          val filename = env.api.gameApiV2.filename(swiss, config.format)
          Ok.chunked(source)
            .pipe(asAttachmentStream(filename))
            .as(gameC.gameContentType(config))

  private def gamesPerSecond(me: Option[lila.user.User]) = MaxPerSecond:
    30 + me.isDefined.so(20) + me.exists(_.isVerified).so(40)

  def swissResults(id: SwissId) = Anon:
    val csv = HTTPRequest.acceptsCsv(req) || get("as").has("csv")
    env.swiss.cache.swissCache.byId(id).orNotFound { swiss =>
      val source = env.swiss.api
        .resultStream(swiss, MaxPerSecond(50), getInt("nb") | Int.MaxValue)
        .mapAsync(8): p =>
          env.user.lightUserApi.asyncFallback(p.player.userId).map(p.withUser)
      val result =
        if csv then csvDownload(lila.swiss.SwissCsv(source))
        else jsonDownload(source.map(env.swiss.json.playerResult))
      result.pipe(asAttachment(env.api.gameApiV2.filename(swiss, if csv then "csv" else "ndjson")))
    }

  def gamesByUsersStream = AnonOrScopedBody(parse.tolerantText)(): ctx ?=>
    val max = ctx.me.fold(300): u =>
      if u.is(UserId.lichess4545) then 900 else 500
    withIdsFromReqBody[UserId](ctx.body, max, id => UserStr.read(id).map(_.id)): ids =>
      GlobalConcurrencyLimitPerIP.events(ctx.ip)(
        ndJson.addKeepAlive:
          env.game.gamesByUsersStream(userIds = ids, withCurrentGames = getBool("withCurrentGames"))
      )(jsOptToNdJson)

  def gamesByIdsStream(streamId: String) = AnonOrScopedBody(parse.tolerantText)(): ctx ?=>
    withIdsFromReqBody[GameId](ctx.body, gamesByIdsMax, GameId.from): ids =>
      GlobalConcurrencyLimitPerIP.events(ctx.ip)(
        ndJson.addKeepAlive:
          env.game.gamesByIdsStream(
            streamId,
            initialIds = ids,
            maxGames = if ctx.me.isDefined then 5_000 else 1_000
          )
      )(jsOptToNdJson)

  def gamesByIdsStreamAddIds(streamId: String) = AnonOrScopedBody(parse.tolerantText)(): ctx ?=>
    withIdsFromReqBody[GameId](ctx.body, gamesByIdsMax, GameId.from): ids =>
      env.game.gamesByIdsStream.addGameIds(streamId, ids)
      jsonOkResult

  private def gamesByIdsMax(using ctx: Context) =
    ctx.me.fold(500): u =>
      if u == UserId.challengermode then 10_000 else 1000

  private def withIdsFromReqBody[Id](
      req: Request[String],
      max: Int,
      transform: String => Option[Id]
  )(f: Set[Id] => Result): Result =
    val ids = req.body.split(',').view.filter(_.nonEmpty).flatMap(s => transform(s.trim)).toSet
    if ids.size > max then JsonBadRequest(jsonError(s"Too many ids: ${ids.size}, expected up to $max"))
    else f(ids)

  val cloudEval =
    val rateLimit = env.security.ipTrust.rateLimit(3_000, 1.day, "cloud-eval.api.ip", _.proxyMultiplier(3))
    Anon:
      rateLimit(rateLimited):
        get("fen").fold[Fu[Result]](notFoundJson("Missing FEN")): fen =>
          import chess.variant.Variant
          env.evalCache.api
            .getEvalJson(
              Variant.orDefault(getAs[Variant.LilaKey]("variant")),
              chess.format.Fen.Full.clean(fen),
              getIntAs[MultiPv]("multiPv") | MultiPv(1)
            )
            .map:
              _.fold[Result](notFoundJson("No cloud evaluation available for that position"))(JsonOk)

  val eventStream =
    Scoped(_.Bot.Play, _.Board.Play, _.Challenge.Read) { _ ?=> me ?=>
      def limited = rateLimited:
        "Please don't poll this endpoint, it is intended to be streamed. See https://lichess.org/api#tag/Board/operation/apiStreamEvent."
      limit.eventStream(me, limited):
        env.round.proxyRepo
          .urgentGames(me)
          .flatMap: povs =>
            env.challenge.api
              .createdByDestId(me)
              .map: challenges =>
                jsOptToNdJson(env.api.eventStream(povs.map(_.game), challenges))
    }

  def activity(name: UserStr) = ApiRequest:
    limit.userActivity(req.ipAddress, fuccess(ApiResult.Limited), cost = 1):
      lila.mon.api.activity.increment(1)
      meOrFetch(name)
        .flatMapz { user =>
          env.activity.read
            .recentAndPreload(user)
            .flatMap:
              _.sequentially(env.activity.jsonView(_, user))
        }
        .map(toApiResult)

  private val ApiMoveStreamGlobalConcurrencyLimitPerIP =
    lila.web.ConcurrencyLimit[IpAddress](
      name = "API concurrency per IP",
      key = "round.apiMoveStream.ip",
      ttl = 20.minutes,
      maxConcurrency = 8
    )

  def moveStream(gameId: GameId) = Anon:
    Found(env.round.proxyRepo.game(gameId)): game =>
      ApiMoveStreamGlobalConcurrencyLimitPerIP(req.ipAddress)(
        ndJson.addKeepAlive(env.round.apiMoveStream(game, gameC.delayMovesFromReq))
      )(jsOptToNdJson)

  def perfStat(username: UserStr, perfKey: PerfKey) = ApiRequest:
    env.perfStat.api
      .data(username, perfKey, computeIfNeeded = true)
      .map:
        _.fold[ApiResult](ApiResult.NoData) { data => ApiResult.Data(env.perfStat.jsonView(data)) }

  def mobileGames = Scoped(_.Web.Mobile) { _ ?=> _ ?=>
    val ids = get("ids").so(_.split(',').take(50).toList).map(GameId.take)
    ids.nonEmpty.so:
      env.round.roundSocket.getMany(ids).flatMap(env.round.mobile.online).map(JsonOk)
  }

  def ApiRequest(js: Context ?=> Fu[ApiResult]) = Anon:
    js.map(toHttp)

  def toApiResult(json: Option[JsValue]): ApiResult =
    json.fold[ApiResult](ApiResult.NoData)(ApiResult.Data.apply)
  def toApiResult(json: Seq[JsValue]): ApiResult = ApiResult.Data(JsArray(json))

  val toHttp: ApiResult => Result =
    case ApiResult.Limited          => rateLimitedJson
    case ApiResult.ClientError(msg) => BadRequest(jsonError(msg))
    case ApiResult.NoData           => notFoundJson()
    case ApiResult.Custom(result)   => result
    case ApiResult.Done             => jsonOkResult
    case ApiResult.Data(json)       => JsonOk(json)

  def jsonDownload(makeSource: => Source[JsValue, ?])(using req: RequestHeader): Result =
    GlobalConcurrencyLimitPerIP.download(req.ipAddress)(makeSource)(jsToNdJson)

  def csvDownload(makeSource: => Source[String, ?])(using req: RequestHeader): Result =
    GlobalConcurrencyLimitPerIP.download(req.ipAddress)(makeSource)(sourceToCsv)

  private def sourceToCsv(source: Source[String, ?]): Result =
    Ok.chunked(source.map(_ + "\n")).as(csvContentType).pipe(noProxyBuffer)

  private[controllers] object GlobalConcurrencyLimitPerIP:
    val events = lila.web.ConcurrencyLimit[IpAddress](
      name = "API events concurrency per IP",
      key = "api.ip.events",
      ttl = 1.hour,
      maxConcurrency = 4
    )
    val eventsForVerifiedUser = lila.web.ConcurrencyLimit[IpAddress](
      name = "API verified events concurrency per IP",
      key = "api.ip.events.verified",
      ttl = 1.hour,
      maxConcurrency = 12
    )
    val download = lila.web.ConcurrencyLimit[IpAddress](
      name = "API download concurrency per IP",
      key = "api.ip.download",
      ttl = 1.hour,
      maxConcurrency = 2
    )
    val generous = lila.web.ConcurrencyLimit[IpAddress](
      name = "API generous concurrency per IP",
      key = "api.ip.generous",
      ttl = 1.hour,
      maxConcurrency = 20
    )

  private[controllers] val GlobalConcurrencyLimitUser = lila.web.ConcurrencyLimit[UserId](
    name = "API concurrency per user",
    key = "api.user",
    ttl = 1.hour,
    maxConcurrency = 2
  )
  private[controllers] val GlobalConcurrencyLimitUserMobile = lila.web.ConcurrencyLimit[UserId](
    name = "API concurrency per mobile user",
    key = "api.user.mobile",
    ttl = 1.hour,
    maxConcurrency = 3
  )
  private[controllers] def GlobalConcurrencyLimitPerUserOption[T](using
      ctx: Context
  ): Option[SourceIdentity[T]] =
    ctx.me.fold(some[SourceIdentity[T]](identity)): me =>
      val limiter = if ctx.isMobileOauth then GlobalConcurrencyLimitUserMobile else GlobalConcurrencyLimitUser
      limiter.compose[T](me.userId)

  private[controllers] def GlobalConcurrencyLimitPerIpAndUserOption[T, U: UserIdOf](
      about: Option[U]
  )(makeSource: => Source[T, ?])(makeResult: Source[T, ?] => Result)(using ctx: Context): Result =
    val ipLimiter =
      if ctx.me.exists(u => about.exists(u.is(_)))
      then GlobalConcurrencyLimitPerIP.generous
      else GlobalConcurrencyLimitPerIP.download
    ipLimiter
      .compose[T](req.ipAddress)
      .flatMap: limitIp =>
        GlobalConcurrencyLimitPerUserOption[T].map: limitUser =>
          makeResult(limitIp(limitUser(makeSource)))
      .getOrElse:
        lila.web.ConcurrencyLimit.limitedDefault(1)

  private type SourceIdentity[T] = Source[T, ?] => Source[T, ?]

private[controllers] object Api:

  enum ApiResult:
    case Data(json: JsValue)
    case ClientError(msg: String)
    case NoData
    case Done
    case Limited
    case Custom(result: Result)
