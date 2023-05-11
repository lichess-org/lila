package controllers

import akka.stream.scaladsl.*
import play.api.i18n.Lang
import play.api.libs.json.*
import play.api.mvc.*
import scala.util.chaining.*

import lila.api.GameApiV2
import lila.app.{ given, * }
import lila.common.config.{ MaxPerPage, MaxPerSecond }
import lila.common.{ HTTPRequest, IpAddress, LightUser }

final class Api(
    env: Env,
    gameC: => Game
) extends LilaController(env):

  import Api.*
  import env.api.{ userApi, gameApi }

  private lazy val apiStatusJson = Json.obj(
    "api" -> Json.obj(
      "current" -> lila.api.Mobile.Api.currentVersion.value,
      "olds"    -> Json.arr()
    )
  )

  val status = Anon:
    val appVersion  = get("v", req)
    val mustUpgrade = appVersion exists lila.api.Mobile.AppVersion.mustUpgrade
    JsonOk(apiStatusJson.add("mustUpgrade", mustUpgrade)).toFuccess

  def index = Anon:
    Ok(views.html.site.bits.api).toFuccess

  private val userRateLimit = env.security.ipTrust.rateLimit(3_000, 1.day, "user.show.api.ip")
  def user(name: UserStr) =
    def get(req: RequestHeader, me: Option[lila.user.User], lang: Lang) =
      userRateLimit(req.ipAddress, rateLimitedFu):
        userApi.extended(
          name,
          me,
          withFollows = userWithFollows(req),
          withTrophies = getBool("trophies", req)
        )(using lang) map toApiResult map toHttp
    OpenOrScoped()(
      ctx ?=> get(ctx.req, ctx.me, ctx.lang),
      req ?=> me => get(req, me.some, me.realLang | reqLang(using req))
    )

  private[controllers] def userWithFollows(req: RequestHeader) =
    HTTPRequest.apiVersion(req).exists(_.value < 6) && !getBool("noFollows", req)

  private[controllers] val UsersRateLimitPerIP = lila.memo.RateLimit.composite[IpAddress](
    key = "users.api.ip",
    enforce = env.net.rateLimit.value
  )(
    ("fast", 2000, 10.minutes),
    ("slow", 30000, 1.day)
  )

  def usersByIds = AnonBodyOf(parse.tolerantText): body =>
    val usernames = body.replace("\n", "").split(',').take(300).flatMap(UserStr.read).toList
    val cost      = usernames.size / 4
    UsersRateLimitPerIP(req.ipAddress, rateLimitedFu, cost = cost):
      lila.mon.api.users.increment(cost.toLong)
      env.user.repo byIds usernames map {
        _.map { env.user.jsonView.full(_, none, withRating = true, withProfile = true) }
      } map toApiResult map toHttp

  def usersStatus = ApiRequest:
    val ids = get("ids", req).??(_.split(',').take(100).toList flatMap UserStr.read).map(_.id)
    env.user.lightUserApi asyncMany ids dmap (_.flatten) flatMap { users =>
      val streamingIds = env.streamer.liveStreamApi.userIds
      def toJson(u: LightUser) =
        LightUser.lightUserWrites
          .writes(u)
          .add("online" -> env.socket.isOnline(u.id))
          .add("playing" -> env.round.playing(u.id))
          .add("streaming" -> streamingIds(u.id))
      if getBool("withGameIds", req)
      then
        users.map { u =>
          (env.round.playing(u.id) ?? env.game.cached.lastPlayedPlayingId(u.id)) map { gameId =>
            toJson(u).add("playingId", gameId)
          }
        }.parallel map toApiResult
      else fuccess(toApiResult(users map toJson))
    }

  private val UserGamesRateLimitPerIP = lila.memo.RateLimit[IpAddress](
    credits = 10 * 1000,
    duration = 10.minutes,
    key = "user_games.api.ip"
  )

  private val UserGamesRateLimitPerUA = lila.memo.RateLimit[Option[UserAgent]](
    credits = 10 * 1000,
    duration = 5.minutes,
    key = "user_games.api.ua"
  )

  private val UserGamesRateLimitGlobal = lila.memo.RateLimit[String](
    credits = 20 * 1000,
    duration = 2.minute,
    key = "user_games.api.global"
  )

  private def UserGamesRateLimit(cost: Int, req: RequestHeader)(run: => Fu[ApiResult]) =
    val ip      = req.ipAddress
    def limited = fuccess(ApiResult.Limited)
    UserGamesRateLimitPerIP(ip, limited, cost = cost):
      UserGamesRateLimitPerUA(HTTPRequest.userAgent(req), limited, cost = cost, msg = ip.value):
        UserGamesRateLimitGlobal("-", limited, cost = cost, msg = ip.value):
          run

  private def gameFlagsFromRequest(req: RequestHeader) =
    lila.api.GameApi.WithFlags(
      analysis = getBool("with_analysis", req),
      moves = getBool("with_moves", req),
      fens = getBool("with_fens", req),
      opening = getBool("with_opening", req),
      moveTimes = getBool("with_movetimes", req),
      token = get("token", req)
    )

  // for mobile app
  def userGames(name: UserStr) = MobileApiRequest:
    val page = (getInt("page", req) | 1) atLeast 1 atMost 200
    val nb   = MaxPerPage((getInt("nb", req) | 10) atLeast 1 atMost 100)
    val cost = page * nb.value + 10
    UserGamesRateLimit(cost, req) {
      lila.mon.api.userGames.increment(cost.toLong)
      env.user.repo byId name flatMapz { user =>
        gameApi.byUser(
          user = user,
          rated = getBoolOpt("rated", req),
          playing = getBoolOpt("playing", req),
          analysed = getBoolOpt("analysed", req),
          withFlags = gameFlagsFromRequest(req),
          nb = nb,
          page = page
        ) map some
      } map toApiResult
    }

  private val GameRateLimitPerIP = lila.memo.RateLimit[IpAddress](
    credits = 100,
    duration = 1.minute,
    key = "game.api.one.ip"
  )

  def game(id: GameId) = ApiRequest:
    GameRateLimitPerIP(req.ipAddress, fuccess(ApiResult.Limited), cost = 1):
      lila.mon.api.game.increment(1)
      gameApi.one(id, gameFlagsFromRequest(req)) map toApiResult

  private val CrosstableRateLimitPerIP = lila.memo.RateLimit[IpAddress](
    credits = 30,
    duration = 10.minutes,
    key = "crosstable.api.ip"
  )

  def crosstable(name1: UserStr, name2: UserStr) =
    ApiRequest:
      CrosstableRateLimitPerIP(req.ipAddress, fuccess(ApiResult.Limited), cost = 1):
        val (u1, u2) = (name1.id, name2.id)
        env.game.crosstableApi(u1, u2) flatMap { ct =>
          (ct.results.nonEmpty && getBool("matchup", req)).?? {
            env.game.crosstableApi.getMatchup(u1, u2)
          } map { matchup =>
            toApiResult:
              lila.game.JsonView.crosstable(ct, matchup).some
          }
        }

  def currentTournaments = ApiRequest:
    given Lang = reqLang
    env.tournament.api.fetchVisibleTournaments flatMap
      env.tournament.apiJsonView.apply map ApiResult.Data.apply

  def tournament(id: TourId) = ApiRequest:
    env.tournament.tournamentRepo byId id flatMapz { tour =>
      val page = (getInt("page", req) | 1) atLeast 1 atMost 200
      env.tournament.jsonView(
        tour = tour,
        page = page.some,
        me = none,
        getTeamName = env.team.getTeamName.apply,
        playerInfoExt = none,
        socketVersion = none,
        partial = false,
        withScores = true
      )(using reqLang, _ => fuccess(Nil)) map some
    } map toApiResult

  def tournamentGames(id: TourId) =
    AnonOrScoped() { req ?=> me =>
      env.tournament.tournamentRepo byId id flatMapz { tour =>
        val onlyUserId = getUserStr("player", req).map(_.id)
        val config = GameApiV2.ByTournamentConfig(
          tour = tour,
          format = GameApiV2.Format byRequest req,
          flags = gameC.requestPgnFlags(req, extended = false),
          perSecond = gamesPerSecond(me)
        )
        GlobalConcurrencyLimitPerIP
          .download(req.ipAddress)(env.api.gameApiV2.exportByTournament(config, onlyUserId)) { source =>
            Ok.chunked(source)
              .pipe(asAttachmentStream(env.api.gameApiV2.filename(tour, config.format)))
              .as(gameC gameContentType config)
          }
          .toFuccess
      }
    }

  def tournamentResults(id: TourId) = Anon:
    val csv = HTTPRequest.acceptsCsv(req) || get("as", req).has("csv")
    env.tournament.tournamentRepo byId id mapz { tour =>
      import lila.tournament.JsonView.playerResultWrites
      val withSheet = getBool("sheet", req)
      val perSecond = MaxPerSecond:
        if withSheet
        then (20 - (tour.estimateNumberOfGamesOneCanPlay / 20).toInt).atLeast(10)
        else 50
      val source = env.tournament.api
        .resultStream(
          tour,
          perSecond,
          getInt("nb", req) | Int.MaxValue,
          withSheet = withSheet
        )
      val result =
        if (csv) csvDownload(lila.tournament.TournamentCsv(source))
        else jsonDownload(source.map(lila.tournament.JsonView.playerResultWrites.writes))
      result.pipe(asAttachment(env.api.gameApiV2.filename(tour, if (csv) "csv" else "ndjson")))
    }

  def tournamentTeams(id: TourId) = Anon:
    env.tournament.tournamentRepo byId id flatMapz { tour =>
      env.tournament.jsonView.apiTeamStanding(tour) map { arr =>
        JsonOk:
          Json.obj(
            "id"    -> tour.id,
            "teams" -> arr
          )
      }
    }

  def tournamentsByOwner(name: UserStr, status: List[Int]) = Anon:
    (name.id != lila.user.User.lichessId) ?? env.user.repo.byId(name) flatMapz { user =>
      val nb     = getInt("nb", req) | Int.MaxValue
      given Lang = reqLang
      jsonDownload {
        env.tournament.api
          .byOwnerStream(user, status flatMap lila.tournament.Status.apply, MaxPerSecond(20), nb)
          .mapAsync(1)(env.tournament.apiJsonView.fullJson)
      }.toFuccess
    }

  def swissGames(id: SwissId) = AnonOrScoped() { req ?=> me =>
    env.swiss.cache.swissCache byId id flatMapz { swiss =>
      val config = GameApiV2.BySwissConfig(
        swissId = swiss.id,
        format = GameApiV2.Format byRequest req,
        flags = gameC.requestPgnFlags(req, extended = false),
        perSecond = gamesPerSecond(me),
        player = getUserStr("player", req).map(_.id)
      )
      GlobalConcurrencyLimitPerIP
        .download(req.ipAddress)(env.api.gameApiV2.exportBySwiss(config)) { source =>
          val filename = env.api.gameApiV2.filename(swiss, config.format)
          Ok.chunked(source)
            .pipe(asAttachmentStream(filename))
            .as(gameC gameContentType config)
        }
        .toFuccess
    }
  }

  private def gamesPerSecond(me: Option[lila.user.User]) = MaxPerSecond(
    30 + me.isDefined.??(20) + me.exists(_.isVerified).??(40)
  )

  def swissResults(id: SwissId) = Anon:
    val csv = HTTPRequest.acceptsCsv(req) || get("as", req).has("csv")
    env.swiss.cache.swissCache byId id mapz { swiss =>
      val source = env.swiss.api
        .resultStream(swiss, MaxPerSecond(50), getInt("nb", req) | Int.MaxValue)
        .mapAsync(8) { p =>
          env.user.lightUserApi.asyncFallback(p.player.userId) map p.withUser
        }
      val result =
        if csv then csvDownload(lila.swiss.SwissCsv(source))
        else jsonDownload(source.map(env.swiss.json.playerResult))
      result.pipe(asAttachment(env.api.gameApiV2.filename(swiss, if csv then "csv" else "ndjson")))
    }

  def gamesByUsersStream = AnonOrScopedBody(parse.tolerantText)() { req ?=> me =>
    val max = me.fold(300) { u => if u is lila.user.User.lichess4545Id then 900 else 500 }
    withIdsFromReqBody[UserId](req, max, id => UserStr.read(id).map(_.id)) { ids =>
      GlobalConcurrencyLimitPerIP.events(req.ipAddress)(
        addKeepAlive:
          env.game.gamesByUsersStream(userIds = ids, withCurrentGames = getBool("withCurrentGames", req))
      )(sourceToNdJsonOption)
    }.toFuccess
  }

  def gamesByIdsStream(streamId: String) = AnonOrScopedBody(parse.tolerantText)() { req ?=> me =>
    withIdsFromReqBody[GameId](req, gamesByIdsMax(me), lila.game.Game.strToIdOpt) { ids =>
      GlobalConcurrencyLimitPerIP.events(req.ipAddress)(
        addKeepAlive(
          env.game.gamesByIdsStream(
            streamId,
            initialIds = ids,
            maxGames = if me.isDefined then 5_000 else 1_000
          )
        )
      )(sourceToNdJsonOption)
    }.toFuccess
  }

  def gamesByIdsStreamAddIds(streamId: String) = AnonOrScopedBody(parse.tolerantText)() { req ?=> me =>
    withIdsFromReqBody[GameId](req, gamesByIdsMax(me), lila.game.Game.strToIdOpt) { ids =>
      env.game.gamesByIdsStream.addGameIds(streamId, ids)
      jsonOkResult
    }.toFuccess
  }

  private def gamesByIdsMax(me: Option[lila.user.User]) =
    me.fold(500) { u => if (u == lila.user.User.challengermodeId) 10_000 else 1000 }

  private def withIdsFromReqBody[Id](
      req: Request[String],
      max: Int,
      transform: String => Option[Id]
  )(f: Set[Id] => Result): Result =
    val ids = req.body.split(',').view.filter(_.nonEmpty).flatMap(s => transform(s.trim)).toSet
    if (ids.size > max) JsonBadRequest(jsonError(s"Too many ids: ${ids.size}, expected up to $max"))
    else f(ids)

  val cloudEval =
    val rateLimit = lila.memo.RateLimit[IpAddress](3_000, 1.day, "cloud-eval.api.ip")
    Anon:
      rateLimit(req.ipAddress, rateLimitedFu):
        get("fen", req).fold(notFoundJson("Missing FEN")): fen =>
          import chess.variant.Variant
          JsonOptionOk:
            env.evalCache.api.getEvalJson(
              Variant.orDefault(getAs[Variant.LilaKey]("variant", req)),
              chess.format.Fen.Epd.clean(fen),
              getIntAs[MultiPv]("multiPv", req) | MultiPv(1)
            )

  val eventStream =
    val rateLimit = lila.memo.RateLimit[UserId](30, 10.minutes, "api.stream.event.user")
    Scoped(_.Bot.Play, _.Board.Play, _.Challenge.Read) { _ ?=> me =>
      def limited = rateLimitedFu:
        "Please don't poll this endpoint, it is intended to be streamed. See https://lichess.org/api#tag/Board/operation/apiStreamEvent."
      rateLimit(me.id, limited):
        env.round.proxyRepo.urgentGames(me) flatMap { povs =>
          env.challenge.api.createdByDestId(me.id) map { challenges =>
            sourceToNdJsonOption(env.api.eventStream(me, povs.map(_.game), challenges))
          }
        }
    }

  private val UserActivityRateLimitPerIP = lila.memo.RateLimit[IpAddress](
    credits = 15,
    duration = 2.minutes,
    key = "user_activity.api.ip"
  )

  def activity(name: UserStr) = ApiRequest:
    given Lang = reqLang
    UserActivityRateLimitPerIP(req.ipAddress, fuccess(ApiResult.Limited), cost = 1):
      lila.mon.api.activity.increment(1)
      env.user.repo byId name flatMapz { user =>
        env.activity.read.recentAndPreload(user) flatMap {
          _.map { env.activity.jsonView(_, user) }.parallel
        }
      } map toApiResult

  private val ApiMoveStreamGlobalConcurrencyLimitPerIP =
    lila.memo.ConcurrencyLimit[IpAddress](
      name = "API concurrency per IP",
      key = "round.apiMoveStream.ip",
      ttl = 20.minutes,
      maxConcurrency = 8
    )

  def moveStream(gameId: GameId) = Anon:
    env.round.proxyRepo.game(gameId).map {
      case None => NotFound
      case Some(game) =>
        ApiMoveStreamGlobalConcurrencyLimitPerIP(req.ipAddress)(
          addKeepAlive(env.round.apiMoveStream(game, gameC.delayMovesFromReq(req)))
        )(sourceToNdJsonOption)
    }

  def perfStat(username: UserStr, perfKey: lila.rating.Perf.Key) = ApiRequest:
    given play.api.i18n.Lang = reqLang(using req)
    env.perfStat.api.data(username, perfKey, none) map {
      _.fold[ApiResult](ApiResult.NoData) { data => ApiResult.Data(env.perfStat.jsonView(data)) }
    }

  def ApiRequest(js: RequestHeader ?=> Fu[ApiResult]) = Anon:
    js(using req) map toHttp

  def MobileApiRequest(js: RequestHeader ?=> Fu[ApiResult]) = Anon:
    if lila.api.Mobile.Api.requested(req)
    then js(using req) map toHttp
    else fuccess(NotFound)

  def toApiResult(json: Option[JsValue]): ApiResult =
    json.fold[ApiResult](ApiResult.NoData)(ApiResult.Data.apply)
  def toApiResult(json: Seq[JsValue]): ApiResult = ApiResult.Data(JsArray(json))

  def toHttp(result: ApiResult): Result =
    result match
      case ApiResult.Limited          => rateLimitedJson
      case ApiResult.ClientError(msg) => BadRequest(jsonError(msg))
      case ApiResult.NoData           => notFoundJsonSync()
      case ApiResult.Custom(result)   => result
      case ApiResult.Done             => jsonOkResult
      case ApiResult.Data(json)       => JsonOk(json)

  def jsonDownload(makeSource: => Source[JsValue, ?])(using req: RequestHeader): Result =
    GlobalConcurrencyLimitPerIP.download(req.ipAddress)(makeSource)(sourceToNdJson)

  def addKeepAlive(source: Source[JsValue, ?]): Source[Option[JsValue], ?] =
    source
      .map(some)
      .keepAlive(50.seconds, () => none) // play's idleTimeout = 75s

  def sourceToNdJson(source: Source[JsValue, ?]): Result =
    sourceToNdJsonString:
      source.map: o =>
        Json.stringify(o) + "\n"

  def sourceToNdJsonOption(source: Source[Option[JsValue], ?]): Result =
    sourceToNdJsonString:
      source.map:
        _ ?? Json.stringify + "\n"

  private def sourceToNdJsonString(source: Source[String, ?]): Result =
    Ok.chunked(source).as(ndJsonContentType) pipe noProxyBuffer

  def csvDownload(makeSource: => Source[String, ?])(using req: RequestHeader): Result =
    GlobalConcurrencyLimitPerIP.download(req.ipAddress)(makeSource)(sourceToCsv)

  private def sourceToCsv(source: Source[String, ?]): Result =
    Ok.chunked(source.map(_ + "\n")).as(csvContentType) pipe noProxyBuffer

  private[controllers] object GlobalConcurrencyLimitPerIP:
    val events = lila.memo.ConcurrencyLimit[IpAddress](
      name = "API events concurrency per IP",
      key = "api.ip.events",
      ttl = 1.hour,
      maxConcurrency = 4
    )
    val download = lila.memo.ConcurrencyLimit[IpAddress](
      name = "API download concurrency per IP",
      key = "api.ip.download",
      ttl = 1.hour,
      maxConcurrency = 2
    )

  private[controllers] val GlobalConcurrencyGenerousLimitPerIP = lila.memo.ConcurrencyLimit[IpAddress](
    name = "API generous concurrency per IP",
    key = "api.ip.generous",
    ttl = 1.hour,
    maxConcurrency = 20
  )
  private[controllers] val GlobalConcurrencyLimitUser = lila.memo.ConcurrencyLimit[UserId](
    name = "API concurrency per user",
    key = "api.user",
    ttl = 1.hour,
    maxConcurrency = 1
  )
  private[controllers] def GlobalConcurrencyLimitPerUserOption[T](
      user: Option[lila.user.User]
  ): Option[SourceIdentity[T]] =
    user.fold(some[SourceIdentity[T]](identity)) { u =>
      GlobalConcurrencyLimitUser.compose[T](u.id)
    }

  private[controllers] def GlobalConcurrencyLimitPerIpAndUserOption[T, U: UserIdOf](
      req: RequestHeader,
      me: Option[lila.user.User],
      about: Option[U]
  )(makeSource: => Source[T, ?])(makeResult: Source[T, ?] => Result): Result =
    val ipLimiter =
      if me.exists(u => about.exists(u.is(_))) then GlobalConcurrencyGenerousLimitPerIP
      else GlobalConcurrencyLimitPerIP.download
    ipLimiter.compose[T](req.ipAddress) flatMap { limitIp =>
      GlobalConcurrencyLimitPerUserOption[T](me) map { limitUser =>
        makeResult(limitIp(limitUser(makeSource)))
      }
    } getOrElse lila.memo.ConcurrencyLimit.limitedDefault(1)

  private type SourceIdentity[T] = Source[T, ?] => Source[T, ?]

private[controllers] object Api:

  enum ApiResult:
    case Data(json: JsValue)
    case ClientError(msg: String)
    case NoData
    case Done
    case Limited
    case Custom(result: Result)
