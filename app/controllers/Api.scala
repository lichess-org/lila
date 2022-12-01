package controllers

import akka.stream.scaladsl.*
import play.api.i18n.Lang
import play.api.libs.json.*
import play.api.mvc.*
import scala.concurrent.duration.*
import scala.util.chaining.*

import lila.api.{ Context, GameApiV2 }
import lila.app.{ given, * }
import lila.common.config.{ MaxPerPage, MaxPerSecond }
import lila.common.{ HTTPRequest, IpAddress, LightUser }
import lila.game.GamesByIdsStream

final class Api(
    env: Env,
    gameC: => Game
) extends LilaController(env):

  import Api.*

  private val userApi = env.api.userApi
  private val gameApi = env.api.gameApi

  private lazy val apiStatusJson =
    Json.obj(
      "api" -> Json.obj(
        "current" -> lila.api.Mobile.Api.currentVersion.value,
        "olds"    -> Json.arr()
      )
    )

  val status = Action { (req: RequestHeader) =>
    val appVersion  = get("v", req)
    val mustUpgrade = appVersion exists lila.api.Mobile.AppVersion.mustUpgrade
    JsonOk(apiStatusJson.add("mustUpgrade", mustUpgrade))
  }

  def index =
    Action {
      Ok(views.html.site.bits.api)
    }

  def user(name: UserStr) =
    def get(req: RequestHeader, lang: Lang, me: Option[lila.user.User]) = userApi.extended(
      name,
      me,
      withFollows = userWithFollows(req),
      withTrophies = getBool("trophies", req)
    )(using lang) map toApiResult map toHttp
    OpenOrScoped()(
      ctx => get(ctx.req, ctx.lang, ctx.me),
      req => me => get(req, me.realLang | reqLang(req), me.some)
    )

  private[controllers] def userWithFollows(req: RequestHeader) =
    HTTPRequest.apiVersion(req).exists(_.value < 6) && !getBool("noFollows", req)

  private[controllers] val UsersRateLimitPerIP = lila.memo.RateLimit.composite[IpAddress](
    key = "users.api.ip",
    enforce = env.net.rateLimit.value
  )(
    ("fast", 2000, 10.minutes),
    ("slow", 40000, 1.day)
  )

  def usersByIds =
    Action.async(parse.tolerantText) { req =>
      val usernames = req.body.replace("\n", "").split(',').take(300).flatMap(UserStr.read).toList
      val ip        = HTTPRequest ipAddress req
      val cost      = usernames.size / 4
      UsersRateLimitPerIP(ip, cost = cost) {
        lila.mon.api.users.increment(cost.toLong)
        env.user.repo byIds usernames map {
          _.map { env.user.jsonView.full(_, none, withRating = true, withProfile = true) }
        } map toApiResult map toHttp
      }(rateLimitedFu)
    }

  def usersStatus =
    ApiRequest { req =>
      val ids = get("ids", req).??(_.split(',').take(100).toList flatMap UserStr.read).map(_.id)
      env.user.lightUserApi asyncMany ids dmap (_.flatten) flatMap { users =>
        val streamingIds = env.streamer.liveStreamApi.userIds
        def toJson(u: LightUser) =
          lila.common.LightUser.lightUserWrites
            .writes(u)
            .add("online" -> env.socket.isOnline.value(u.id))
            .add("playing" -> env.round.playing(u.id))
            .add("streaming" -> streamingIds(u.id))
        if (getBool("withGameIds", req)) users.map { u =>
          (env.round.playing(u.id) ?? env.game.cached.lastPlayedPlayingId(u.id)) map { gameId =>
            toJson(u).add("playingId", gameId)
          }
        }.sequenceFu map toApiResult
        else fuccess(toApiResult(users map toJson))
      }
    }

  private val UserGamesRateLimitPerIP = new lila.memo.RateLimit[IpAddress](
    credits = 10 * 1000,
    duration = 10.minutes,
    key = "user_games.api.ip"
  )

  private val UserGamesRateLimitPerUA = new lila.memo.RateLimit[String](
    credits = 10 * 1000,
    duration = 5.minutes,
    key = "user_games.api.ua"
  )

  private val UserGamesRateLimitGlobal = new lila.memo.RateLimit[String](
    credits = 20 * 1000,
    duration = 2.minute,
    key = "user_games.api.global"
  )

  private def UserGamesRateLimit(cost: Int, req: RequestHeader)(run: => Fu[ApiResult]) =
    val ip = HTTPRequest ipAddress req
    UserGamesRateLimitPerIP(ip, cost = cost) {
      UserGamesRateLimitPerUA(~HTTPRequest.userAgent(req), cost = cost, msg = ip.value) {
        UserGamesRateLimitGlobal("-", cost = cost, msg = ip.value) {
          run
        }(fuccess(ApiResult.Limited))
      }(fuccess(ApiResult.Limited))
    }(fuccess(ApiResult.Limited))

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
  def userGames(name: UserStr) =
    MobileApiRequest { req =>
      val page = (getInt("page", req) | 1) atLeast 1 atMost 200
      val nb   = MaxPerPage((getInt("nb", req) | 10) atLeast 1 atMost 100)
      val cost = page * nb.value + 10
      UserGamesRateLimit(cost, req) {
        lila.mon.api.userGames.increment(cost.toLong)
        env.user.repo byId name flatMap {
          _ ?? { user =>
            gameApi.byUser(
              user = user,
              rated = getBoolOpt("rated", req),
              playing = getBoolOpt("playing", req),
              analysed = getBoolOpt("analysed", req),
              withFlags = gameFlagsFromRequest(req),
              nb = nb,
              page = page
            ) map some
          }
        } map toApiResult
      }
    }

  private val GameRateLimitPerIP = new lila.memo.RateLimit[IpAddress](
    credits = 100,
    duration = 1.minute,
    key = "game.api.one.ip"
  )

  def game(id: GameId) =
    ApiRequest { req =>
      GameRateLimitPerIP(HTTPRequest ipAddress req, cost = 1) {
        lila.mon.api.game.increment(1)
        gameApi.one(id, gameFlagsFromRequest(req)) map toApiResult
      }(fuccess(ApiResult.Limited))
    }

  private val CrosstableRateLimitPerIP = new lila.memo.RateLimit[IpAddress](
    credits = 30,
    duration = 10.minutes,
    key = "crosstable.api.ip"
  )

  def crosstable(name1: UserStr, name2: UserStr) =
    ApiRequest { req =>
      CrosstableRateLimitPerIP(HTTPRequest ipAddress req, cost = 1) {
        val (u1, u2) = (name1.id, name2.id)
        env.game.crosstableApi(u1, u2) flatMap { ct =>
          (ct.results.nonEmpty && getBool("matchup", req)).?? {
            env.game.crosstableApi.getMatchup(u1, u2)
          } map { matchup =>
            toApiResult {
              lila.game.JsonView.crosstable(ct, matchup).some
            }
          }
        }
      }(fuccess(ApiResult.Limited))
    }

  def currentTournaments =
    ApiRequest { implicit req =>
      implicit val lang: Lang = reqLang
      env.tournament.api.fetchVisibleTournaments flatMap
        env.tournament.apiJsonView.apply map ApiResult.Data.apply
    }

  def tournament(id: TourId) =
    ApiRequest { implicit req =>
      env.tournament.tournamentRepo byId id flatMap {
        _ ?? { tour =>
          val page = (getInt("page", req) | 1) atLeast 1 atMost 200
          env.tournament.jsonView(
            tour = tour,
            page = page.some,
            me = none,
            getUserTeamIds = _ => fuccess(Nil),
            getTeamName = env.team.getTeamName.value,
            playerInfoExt = none,
            socketVersion = none,
            partial = false,
            withScores = true
          )(using reqLang) map some
        }
      } map toApiResult
    }

  def tournamentGames(id: TourId) =
    AnonOrScoped() { req => me =>
      env.tournament.tournamentRepo byId id flatMap {
        _ ?? { tour =>
          val onlyUserId = getUserStr("player", req).map(_.id)
          val config = GameApiV2.ByTournamentConfig(
            tour = tour,
            format = GameApiV2.Format byRequest req,
            flags = gameC.requestPgnFlags(req, extended = false),
            perSecond = MaxPerSecond(20 + me.isDefined ?? 10)
          )
          GlobalConcurrencyLimitPerIP(HTTPRequest ipAddress req)(
            env.api.gameApiV2.exportByTournament(config, onlyUserId)
          ) { source =>
            val filename = env.api.gameApiV2.filename(tour, config.format)
            Ok.chunked(source)
              .pipe(asAttachmentStream(env.api.gameApiV2.filename(tour, config.format)))
              .as(gameC gameContentType config)
          }.toFuccess
        }
      }
    }

  def tournamentResults(id: TourId) =
    Action.async { implicit req =>
      val csv = HTTPRequest.acceptsCsv(req) || get("as", req).has("csv")
      env.tournament.tournamentRepo byId id map {
        _ ?? { tour =>
          import lila.tournament.JsonView.playerResultWrites
          val source = env.tournament.api
            .resultStream(tour, MaxPerSecond(40), getInt("nb", req) | Int.MaxValue)
          val result =
            if (csv) csvStream(lila.tournament.TournamentCsv(source))
            else jsonStream(source.map(lila.tournament.JsonView.playerResultWrites.writes))
          result.pipe(asAttachment(env.api.gameApiV2.filename(tour, if (csv) "csv" else "ndjson")))
        }
      }
    }

  def tournamentTeams(id: TourId) =
    Action.async {
      env.tournament.tournamentRepo byId id flatMap {
        _ ?? { tour =>
          env.tournament.jsonView.apiTeamStanding(tour) map { arr =>
            JsonOk(
              Json.obj(
                "id"    -> tour.id,
                "teams" -> arr
              )
            )
          }
        }
      }
    }

  def tournamentsByOwner(name: UserStr, status: List[Int]) =
    Action.async { implicit req =>
      given Lang = reqLang
      (name.id != lila.user.User.lichessId) ?? env.user.repo.byId(name) flatMap {
        _ ?? { user =>
          val nb = getInt("nb", req) | Int.MaxValue
          jsonStream {
            env.tournament.api
              .byOwnerStream(user, status flatMap lila.tournament.Status.apply, MaxPerSecond(20), nb)
              .mapAsync(1)(env.tournament.apiJsonView.fullJson)
          }.toFuccess
        }
      }
    }

  def swissGames(id: SwissId) =
    AnonOrScoped() { req => me =>
      env.swiss.cache.swissCache byId id flatMap {
        _ ?? { swiss =>
          val config = GameApiV2.BySwissConfig(
            swissId = swiss.id,
            format = GameApiV2.Format byRequest req,
            flags = gameC.requestPgnFlags(req, extended = false),
            perSecond = MaxPerSecond(20 + me.isDefined ?? 10)
          )
          GlobalConcurrencyLimitPerIP(HTTPRequest ipAddress req)(
            env.api.gameApiV2.exportBySwiss(config)
          ) { source =>
            val filename = env.api.gameApiV2.filename(swiss, config.format)
            Ok.chunked(source)
              .pipe(asAttachmentStream(filename))
              .as(gameC gameContentType config)
          }.toFuccess
        }
      }
    }

  def swissResults(id: SwissId) = Action.async { implicit req =>
    val csv = HTTPRequest.acceptsCsv(req) || get("as", req).has("csv")
    env.swiss.cache.swissCache byId id map {
      _ ?? { swiss =>
        val source = env.swiss.api
          .resultStream(swiss, MaxPerSecond(50), getInt("nb", req) | Int.MaxValue)
          .mapAsync(8) { p =>
            env.user.lightUserApi.asyncFallback(p.player.userId) map p.withUser
          }
        val result =
          if (csv) csvStream(lila.swiss.SwissCsv(source))
          else jsonStream(source.map(env.swiss.json.playerResult))
        result.pipe(asAttachment(env.api.gameApiV2.filename(swiss, if (csv) "csv" else "ndjson")))
      }
    }
  }

  def gamesByUsersStream =
    AnonOrScopedBody(parse.tolerantText)() { req => me =>
      val max = me.fold(300) { u => if (u == lila.user.User.lichess4545Id) 900 else 500 }
      withIdsFromReqBody[UserId](req, max, UserId(_)) { ids =>
        GlobalConcurrencyLimitPerIP(HTTPRequest ipAddress req)(
          addKeepAlive(
            env.game.gamesByUsersStream(userIds = ids, withCurrentGames = getBool("withCurrentGames", req))
          )
        )(sourceToNdJsonOption)
      }.toFuccess
    }

  def gamesByIdsStream(streamId: String) =
    AnonOrScopedBody(parse.tolerantText)() { req => me =>
      withIdsFromReqBody[GameId](req, gamesByIdsMax(me), lila.game.Game.strToId(_)) { ids =>
        GlobalConcurrencyLimitPerIP(HTTPRequest ipAddress req)(
          addKeepAlive(
            env.game.gamesByIdsStream(
              streamId,
              initialIds = ids,
              maxGames = if (me.isDefined) 5_000 else 1_000
            )
          )
        )(sourceToNdJsonOption)
      }.toFuccess
    }

  def gamesByIdsStreamAddIds(streamId: String) =
    AnonOrScopedBody(parse.tolerantText)() { req => me =>
      withIdsFromReqBody[GameId](req, gamesByIdsMax(me), lila.game.Game.strToId(_)) { ids =>
        env.game.gamesByIdsStream.addGameIds(streamId, ids)
        jsonOkResult
      }.toFuccess
    }

  private def gamesByIdsMax(me: Option[lila.user.User]) =
    me.fold(500) { u => if (u == lila.user.User.challengermodeId) 10_000 else 1000 }

  private def withIdsFromReqBody[Id](
      req: Request[String],
      max: Int,
      transform: String => Id
  )(f: Set[Id] => Result): Result =
    val ids = req.body.toLowerCase.split(',').view.filter(_.nonEmpty).map(s => transform(s.trim)).toSet
    if (ids.size > max) JsonBadRequest(jsonError(s"Too many ids: ${ids.size}, expected up to $max"))
    else f(ids)

  def cloudEval =
    Action.async { req =>
      get("fen", req).fold(notFoundJson("Missing FEN")) { fen =>
        JsonOptionOk(
          env.evalCache.api.getEvalJson(
            chess.variant.Variant orDefault ~get("variant", req),
            chess.format.Fen(fen),
            getInt("multiPv", req) | 1
          )
        )
      }
    }

  def eventStream =
    Scoped(_.Bot.Play, _.Board.Play, _.Challenge.Read) { _ => me =>
      env.round.proxyRepo.urgentGames(me) flatMap { povs =>
        env.challenge.api.createdByDestId(me.id) map { challenges =>
          sourceToNdJsonOption(env.api.eventStream(me, povs.map(_.game), challenges))
        }
      }
    }

  private val UserActivityRateLimitPerIP = new lila.memo.RateLimit[IpAddress](
    credits = 15,
    duration = 2.minutes,
    key = "user_activity.api.ip"
  )

  def activity(name: UserStr) =
    ApiRequest { implicit req =>
      given Lang = reqLang
      UserActivityRateLimitPerIP(HTTPRequest ipAddress req, cost = 1) {
        lila.mon.api.activity.increment(1)
        env.user.repo byId name flatMap {
          _ ?? { user =>
            env.activity.read.recentAndPreload(user) flatMap {
              _.map { env.activity.jsonView(_, user) }.sequenceFu
            }
          }
        } map toApiResult
      }(fuccess(ApiResult.Limited))
    }

  private val ApiMoveStreamGlobalConcurrencyLimitPerIP =
    new lila.memo.ConcurrencyLimit[IpAddress](
      name = "API concurrency per IP",
      key = "round.apiMoveStream.ip",
      ttl = 20.minutes,
      maxConcurrency = 8
    )

  def moveStream(gameId: GameId) =
    Action.async { req =>
      env.round.proxyRepo.gameIfPresent(gameId) map {
        case None => NotFound
        case Some(game) =>
          ApiMoveStreamGlobalConcurrencyLimitPerIP(HTTPRequest ipAddress req)(
            addKeepAlive(env.round.apiMoveStream(game, gameC.delayMovesFromReq(req)))
          )(sourceToNdJsonOption)
      }
    }

  def perfStat(username: UserStr, perfKey: lila.rating.Perf.Key) = ApiRequest { req =>
    given play.api.i18n.Lang = reqLang(req)
    env.perfStat.api.data(username, perfKey, none) map {
      _.fold[ApiResult](ApiResult.NoData) { data => ApiResult.Data(env.perfStat.jsonView(data)) }
    }
  }

  def CookieBasedApiRequest(js: Context => Fu[ApiResult]) =
    Open { ctx =>
      js(ctx) map toHttp
    }
  def ApiRequest(js: RequestHeader => Fu[ApiResult]) =
    Action.async { req =>
      js(req) map toHttp
    }
  def MobileApiRequest(js: RequestHeader => Fu[ApiResult]) =
    Action.async { req =>
      if (lila.api.Mobile.Api requested req) js(req) map toHttp
      else fuccess(NotFound)
    }

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

  def jsonStream(makeSource: => Source[JsValue, ?])(implicit req: RequestHeader): Result =
    GlobalConcurrencyLimitPerIP(HTTPRequest ipAddress req)(makeSource)(sourceToNdJson)

  def addKeepAlive(source: Source[JsValue, ?]): Source[Option[JsValue], ?] =
    source
      .map(some)
      .keepAlive(50.seconds, () => none) // play's idleTimeout = 75s

  def sourceToNdJson(source: Source[JsValue, ?]): Result =
    sourceToNdJsonString {
      source.map { o =>
        Json.stringify(o) + "\n"
      }
    }

  def sourceToNdJsonOption(source: Source[Option[JsValue], ?]): Result =
    sourceToNdJsonString {
      source.map { _ ?? Json.stringify + "\n" }
    }

  private def sourceToNdJsonString(source: Source[String, ?]): Result =
    Ok.chunked(source).as(ndJsonContentType) pipe noProxyBuffer

  def csvStream(makeSource: => Source[String, ?])(implicit req: RequestHeader): Result =
    GlobalConcurrencyLimitPerIP(HTTPRequest ipAddress req)(makeSource)(sourceToCsv)

  private def sourceToCsv(source: Source[String, ?]): Result =
    Ok.chunked(source.map(_ + "\n")).as(csvContentType) pipe noProxyBuffer

  private[controllers] val GlobalConcurrencyLimitPerIP = new lila.memo.ConcurrencyLimit[IpAddress](
    name = "API concurrency per IP",
    key = "api.ip",
    ttl = 1.hour,
    maxConcurrency = 2
  )
  private[controllers] val GlobalConcurrencyLimitUser = new lila.memo.ConcurrencyLimit[UserId](
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

  private[controllers] def GlobalConcurrencyLimitPerIpAndUserOption[T](
      req: RequestHeader,
      me: Option[lila.user.User]
  )(makeSource: => Source[T, ?])(makeResult: Source[T, ?] => Result): Result =
    GlobalConcurrencyLimitPerIP.compose[T](HTTPRequest ipAddress req) flatMap { limitIp =>
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
