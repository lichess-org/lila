package controllers

import org.apache.pekko.stream.scaladsl.*
import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.common.Json.given
import lila.core.chess.MultiPv
import lila.core.net.IpAddress
import lila.core.{ LightUser, id }
import lila.security.{ Mobile, UserAgentParser }
import lila.web.ConcurrencyLimit

final class Api(env: Env, gameC: => Game) extends LilaController(env):

  import Api.*
  import env.api.{ userApi, gameApi }

  val status = Anon:
    Ok(Mobile.Api.lichobileStatusJson).publicCache(1.hour)

  val version = Anon:
    Ok(Json.obj("server" -> env.web.lilaVersion)).publicCache(1.minute)

  private val userShowApiRateLimit =
    env.security.ipTrust.rateLimit(8_000 * 2, 1.day, "user.show.api.ip", _.proxyMultiplier(4))

  def user(name: UserStr) = OpenOrScoped(): ctx ?=>
    val cost =
      if ctx.me.exists(_.isVerified) then 1
      else (if env.socket.isOnline.exec(name.id) then 2 else 4) + ctx.isAnon.so(3)
    userShowApiRateLimit(rateLimited, cost = cost):
      userApi
        .extended(
          name,
          lila.api.UserApi.Opts(
            withTrophies = getBool("trophies"),
            withCanChallenge = getBool("challenge"),
            withRelation = ctx.fullAuthOrScope(_.Follow.Read),
            withProfile = getBoolOpt("profile") | true,
            withRank = getBool("rank"),
            withFideId = getBool("fideId")
          )
        )
        .map(toApiResult)
        .map(toHttp)

  def usersByIds = AnonOrScopedBody(parse.tolerantText)(): ctx ?=>
    val usernames = ctx.body.body.replace("\n", "").split(',').take(300).flatMap(UserStr.read).toList
    val cost = usernames.size / {
      if ctx.me.exists(_.isVerified) then 20
      else if ctx.isAuth then 6
      else 3
    }
    val withRanks = getBool("rank")
    limit.apiUsers(req.ipAddress, rateLimited, cost = cost.atLeast(1)):
      lila.mon.api.users.increment(cost.toLong)
      env.user.api
        .listWithPerfs(usernames, includeClosed = true)
        .map:
          _.map: u =>
            env.user.jsonView.full(
              u.user,
              u.perfs.some,
              withProfile = getBoolOpt("profile") | true,
              rankMap = withRanks.option(env.user.rankingsOf(u.user.id))
            )
        .map(toApiResult)
        .map(toHttp)

  def usersStatus = ApiRequest:
    val ids = get("ids").so(_.split(',').take(100).toList.flatMap(UserStr.read).map(_.id))
    if ids.isEmpty then fuccess(ApiResult.ClientError("No user ids provided"))
    else
      val withSignal = getBool("withSignal")
      env.user.lightUserApi.asyncMany(ids).dmap(_.flatten).flatMap { users =>
        val streamingIds = env.streamer.liveApi.userIds
        def toJson(u: LightUser) =
          lila.common.Json.lightUser
            .write(u)
            .add("online", env.socket.isOnline.exec(u.id))
            .add("playing", env.round.playing(u.id))
            .add("streaming", streamingIds(u.id))
            .add("signal", withSignal.so(env.socket.getLagRating(u.id)))
        def gameIds: Fu[List[Option[id.GameId]]] = users.sequentially: u =>
          env.round.playing.lastPlayedPlayingId(u.id)
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

  def game(id: GameId) = ApiRequest:
    gameApi.one(id).map(toApiResult)

  def crosstable(name1: UserStr, name2: UserStr) = ApiRequest:
    limit.crosstable(req.ipAddress, fuccess(ApiResult.Limited)):
      val (u1, u2) = (name1.id, name2.id)
      import lila.game.JsonView.given
      for
        ct <- env.game.crosstableApi(u1, u2)
        matchup <- (ct.results.nonEmpty && getBool("matchup"))
          .so(env.game.crosstableApi.getMatchup(u1, u2))
        both = lila.game.Crosstable.WithMatchup(ct, matchup)
      yield toApiResult(Json.toJsObject(both).some)

  def currentTournaments = ApiRequest:
    env.tournament.api.fetchVisibleTournaments
      .flatMap(env.tournament.apiJsonView.apply)
      .map(ApiResult.Data.apply)

  def gamesByUsersStream = AnonOrScopedBody(parse.tolerantText)(): ctx ?=>
    val max = ctx.me.fold(300): u =>
      if u.is(UserId.lichess4545) then 900 else 500
    withIdsFromReqBody[UserId](ctx.body, max, id => UserStr.read(id).map(_.id)): ids =>
      limit.GlobalConcurrencyLimitPerIP.events(ctx.ip)(
        ndJson.addKeepAlive:
          env.game.gamesByUsersStream(userIds = ids, withCurrentGames = getBool("withCurrentGames"))
      )(jsOptToNdJson)

  def gamesByIdsStream(streamId: String) = AnonOrScopedBody(parse.tolerantText)(): ctx ?=>
    withIdsFromReqBody[GameId](ctx.body, gamesByIdsMax, GameId.from): ids =>
      limit.GlobalConcurrencyLimitPerIP.events(ctx.ip)(
        ndJson.addKeepAlive:
          env.game.gamesByIdsStream(
            streamId,
            initialIds = ids,
            maxGames = if ctx.me.isDefined then 5_000 else 1_000
          )
      )(jsOptToNdJson)

  def gamesByIdsStreamAddIds(streamId: String) = AnonOrScopedBody(parse.tolerantText)(): ctx ?=>
    withIdsFromReqBody[GameId](ctx.body, gamesByIdsMax, GameId.from): ids =>
      if env.game.gamesByIdsStream.exists(streamId) then
        env.game.gamesByIdsStream.addGameIds(streamId, ids)
        jsonOkResult
      else notFoundJson()

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

  def gamesByOauthOriginStream = Scoped():
    val extraUsers = get("extraUsers").so(_.split(',').view.flatMap(UserStr.read).map(_.id).toSet)
    env.api
      .gameStreamByOauthOrigin(getTimestamp("since"), extraUsers)
      .fold(
        error => JsonBadRequest(error).toFuccess,
        source => jsOptToNdJson(ndJson.addKeepAlive(source))
      )

  val cloudEval =
    val rateLimit = env.security.ipTrust.rateLimit(3_000, 1.day, "cloud-eval.api.ip", _.proxyMultiplier(3))
    AnonOrScoped():
      WithProxy: proxy ?=>
        limit.enumeration.cloudEval(rateLimited):
          val cost = if ctx.isAuth then 1 else if UserAgentParser.trust.isSuspicious then 5 else 2
          rateLimit.knownProxy(rateLimited, cost = cost):
            get("fen")
              .fold[Fu[Result]](notFoundJson("Missing FEN")): fen =>
                import chess.variant.Variant
                env.evalCache.api
                  .getEvalJson(
                    Variant.orDefault(getAs[Variant.LilaKey]("variant")),
                    chess.format.Fen.Full.clean(fen),
                    getIntAs[MultiPv]("multiPv") | MultiPv(1)
                  )
                  .map:
                    _.fold[Result](notFoundJson("No cloud evaluation available for that position"))(Ok(_))
              .map(_.publicCache(1.day))

  val eventStream =
    Scoped(_.Bot.Play, _.Board.Play, _.Challenge.Read) { _ ?=> me ?=>
      def limited = rateLimited:
        "Please don't poll this endpoint, it is intended to be streamed. See https://lichess.org/api#tag/board/GET/api/board/game/stream/{gameId}."
      HTTPRequest.bearer.so: (bearer, _) =>
        val fromWebsite = HTTPRequest.referer.exists(_.startsWith(env.net.baseUrl.value)) &&
          HTTPRequest.cookieStr.isDefined
        limit.eventStream(fromWebsite)(me.userId, limited):
          for
            povs <- env.round.proxyRepo.urgentGames(me)
            challenges <- env.challenge.api.createdByDestId(me)
            stream = env.api.eventStream(
              povs.value.map(_.game),
              challenges,
              bearer,
              fromWebsite = fromWebsite
            )
          yield jsOptToNdJson(stream)
    }

  def gameChat(gameId: GameId) = Anon:
    Found(env.chat.api.userChat.findOption(ChatId(s"$gameId/w"))): chat =>
      Ok(Json.obj("lines" -> env.chat.json.boardApi(chat)))

  def roomChat(roomId: RoomId) = SecuredScoped(_.ViewPrivateComms): _ ?=>
    Found(env.chat.api.userChat.findOption(roomId.into(ChatId))): chat =>
      JsonOk(env.chat.json.modApi(chat))

  def activity(name: UserStr) = ApiRequest:
    limit.userActivity(req.ipAddress, fuccess(ApiResult.Limited)):
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
      key = "round.apiMoveStream.ip",
      ttl = 20.minutes,
      maxConcurrency = 8
    )

  def moveStream(gameId: GameId) = AnonOrScoped():
    Found(env.round.proxyRepo.game(gameId)): game =>
      def source = ndJson.addKeepAlive(env.round.apiMoveStream(game, gameC.delayMovesFromReq))
      if ctx.is(UserId.t3) then jsOptToNdJson(source)
      else ApiMoveStreamGlobalConcurrencyLimitPerIP(req.ipAddress)(source)(jsOptToNdJson)

  def perfStat(username: UserStr, perfKey: PerfKey) = AnonOrScoped(): ctx ?=>
    env.perfStat.api
      .data(username, perfKey, computeIfNeeded = ctx.is(username))
      .map:
        _.fold[ApiResult](ApiResult.NoData)(data => ApiResult.Data(env.perfStat.jsonView(data)))
      .map(toHttp)

  def mobileGames = Scoped(_.Web.Mobile, _.Web.Takex3) { _ ?=> _ ?=>
    val ids = get("ids").so(_.split(',').take(50).toList).map(GameId.take)
    ids.nonEmpty.so:
      env.round.roundSocket.getMany(ids).flatMap(env.round.mobile.online).map(Ok(_))
  }

  /* aggregates, for the new mobile app:
   * /api/games/user/:user
   * /api/account?playban=1
   * /api/account/playing
   * /tournament/featured
   * /inbox/unread-count
   * /api/challenge
   * /api/mobile/following
   */
  def mobileHome = AnonOrScoped(_.Web.Mobile, _.Web.Takex3) { ctx ?=>
    limit.apiMobileHome(ctx.userId | ctx.ip, rateLimited):
      JsonOk(env.api.mobile.home(ctx.oauth))
  }

  /* aggregates, for the new mobile app:
   * /api/broadcast/top?page=1
   * /api/tv/channels
   * /api/streamer/live
   */
  def mobileWatch = Anon { _ ?=>
    JsonOk(env.api.mobile.watch)
  }

  /* aggregates, for the new mobile app:
   * /api/account?playban=1 or /api/user/:username?challenge=1
   * /api/user/status?ids=:username
   * /api/user/$id/activity
   * /api/games/user/:user
   * /api/user/:id/current-game
   * /api/crosstable/:id1/:id2
   */
  def mobileProfile(username: UserStr) = AnonOrScoped(_.Web.Mobile, _.Web.Takex3) { _ ?=>
    Found(meOrFetch(username)): user =>
      JsonOk(env.api.mobile.profile(user))
  }

  def ApiRequest(js: Context ?=> Fu[ApiResult]) = Anon:
    js.map(toHttp)

  def toApiResult(json: Option[JsValue]): ApiResult =
    json.fold[ApiResult](ApiResult.NoData)(ApiResult.Data.apply)
  def toApiResult(json: Seq[JsValue]): ApiResult = ApiResult.Data(JsArray(json))

  val toHttp: ApiResult => Result =
    case ApiResult.Limited => rateLimitedJson
    case ApiResult.ClientError(msg) => BadRequest(jsonError(msg))
    case ApiResult.NoData => notFoundJson()
    case ApiResult.Custom(result) => result
    case ApiResult.Done => jsonOkResult
    case ApiResult.Data(json) => Ok(json)

  def jsonDownload(makeSource: => Source[JsValue, ?])(using ctx: Context): Result =
    limit.GlobalConcurrencyLimitPerIP.download(ctx.req.ipAddress)(makeSource)(jsToNdJson)

  def csvDownload(makeSource: => Source[String, ?])(using ctx: Context): Result =
    limit.GlobalConcurrencyLimitPerIP.download(ctx.req.ipAddress)(makeSource)(sourceToCsv)

  private def sourceToCsv(source: Source[String, ?]): Result =
    Ok.chunked(source.map(_ + "\n")).as(CSV).noProxyBuffer

  private[controllers] val GlobalConcurrencyLimitUser = ConcurrencyLimit[UserId](2, "api.user")
  private[controllers] val GlobalConcurrencyLimitUserMobile = ConcurrencyLimit[UserId](3, "api.user.mobile")
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
      then limit.GlobalConcurrencyLimitPerIP.generous
      else limit.GlobalConcurrencyLimitPerIP.download
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
