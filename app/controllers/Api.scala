package controllers

import akka.stream.scaladsl._
import ornicar.scalalib.Zero
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.api.{ Context, GameApiV2 }
import lila.app._
import lila.common.config.{ MaxPerPage, MaxPerSecond }
import lila.common.Json.jodaWrites
import lila.common.{ HTTPRequest, IpAddress }

final class Api(
    env: Env,
    gameC: => Game
) extends LilaController(env) {

  import Api._

  private val userApi = env.api.userApi
  private val gameApi = env.api.gameApi

  implicit private[controllers] val limitedDefault = Zero.instance[ApiResult](Limited)

  private lazy val apiStatusJson = {
    val api = lila.api.Mobile.Api
    Json.obj(
      "api" -> Json.obj(
        "current" -> api.currentVersion.value,
        "olds" -> api.oldVersions.map { old =>
          Json.obj(
            "version"       -> old.version.value,
            "deprecatedAt"  -> old.deprecatedAt,
            "unsupportedAt" -> old.unsupportedAt
          )
        }
      )
    )
  }

  val status = Action { req =>
    val appVersion  = get("v", req)
    val mustUpgrade = appVersion exists lila.api.Mobile.AppVersion.mustUpgrade _
    Ok(apiStatusJson.add("mustUpgrade", mustUpgrade)) as JSON
  }

  def index = Action {
    Ok(views.html.site.bits.api)
  }

  def user(name: String) = CookieBasedApiRequest { ctx =>
    userApi.extended(name, ctx.me) map toApiResult
  }

  private[controllers] val UsersRateLimitGlobal = new lila.memo.RateLimit[String](
    credits = 1000,
    duration = 1 minute,
    name = "team users API global",
    key = "team_users.api.global"
  )

  private[controllers] val UsersRateLimitPerIP = new lila.memo.RateLimit[IpAddress](
    credits = 1000,
    duration = 10 minutes,
    name = "team users API per IP",
    key = "team_users.api.ip"
  )

  def usersByIds = Action.async(parse.tolerantText) { req =>
    val usernames = req.body.split(',').take(300).toList
    val ip        = HTTPRequest lastRemoteAddress req
    val cost      = usernames.size / 4
    UsersRateLimitPerIP(ip, cost = cost) {
      UsersRateLimitGlobal("-", cost = cost, msg = ip.value) {
        lila.mon.api.users.increment(cost)
        env.user.repo nameds usernames map {
          _.map { env.user.jsonView(_, none) }
        } map toApiResult map toHttp
      }
    }
  }

  def usersStatus = ApiRequest { req =>
    val ids = get("ids", req).??(_.split(',').take(50).toList map lila.user.User.normalize)
    env.user.lightUserApi asyncMany ids dmap (_.flatten) map { users =>
      val actualIds    = users.map(_.id)
      val playingIds   = env.relation.online.playing intersect actualIds
      val streamingIds = env.streamer.liveStreamApi.userIds
      toApiResult {
        users.map { u =>
          lila.common.LightUser.lightUserWrites
            .writes(u)
            .add("online" -> env.socket.isOnline(u.id))
            .add("playing" -> playingIds(u.id))
            .add("streaming" -> streamingIds(u.id))
        }
      }
    }
  }

  private val UserGamesRateLimitPerIP = new lila.memo.RateLimit[IpAddress](
    credits = 10 * 1000,
    duration = 10 minutes,
    name = "user games API per IP",
    key = "user_games.api.ip"
  )

  private val UserGamesRateLimitPerUA = new lila.memo.RateLimit[String](
    credits = 10 * 1000,
    duration = 5 minutes,
    name = "user games API per UA",
    key = "user_games.api.ua"
  )

  private val UserGamesRateLimitGlobal = new lila.memo.RateLimit[String](
    credits = 20 * 1000,
    duration = 2 minute,
    name = "user games API global",
    key = "user_games.api.global"
  )

  private def UserGamesRateLimit(cost: Int, req: RequestHeader)(run: => Fu[ApiResult]) = {
    val ip = HTTPRequest lastRemoteAddress req
    UserGamesRateLimitPerIP(ip, cost = cost) {
      UserGamesRateLimitPerUA(~HTTPRequest.userAgent(req), cost = cost, msg = ip.value) {
        UserGamesRateLimitGlobal("-", cost = cost, msg = ip.value) {
          run
        }
      }
    }
  }

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
  def userGames(name: String) = MobileApiRequest { req =>
    val page = (getInt("page", req) | 1) atLeast 1 atMost 200
    val nb   = MaxPerPage((getInt("nb", req) | 10) atLeast 1 atMost 100)
    val cost = page * nb.value + 10
    UserGamesRateLimit(cost, req) {
      lila.mon.api.userGames.increment(cost)
      env.user.repo named name flatMap {
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
    duration = 1 minute,
    name = "game API per IP",
    key = "game.api.one.ip"
  )

  def game(id: String) = ApiRequest { req =>
    GameRateLimitPerIP(HTTPRequest lastRemoteAddress req, cost = 1) {
      lila.mon.api.game.increment(1)
      gameApi.one(id take lila.game.Game.gameIdSize, gameFlagsFromRequest(req)) map toApiResult
    }
  }

  private val CrosstableRateLimitPerIP = new lila.memo.RateLimit[IpAddress](
    credits = 30,
    duration = 10 minutes,
    name = "crosstable API per IP",
    key = "crosstable.api.ip"
  )

  def crosstable(u1: String, u2: String) = ApiRequest { req =>
    CrosstableRateLimitPerIP(HTTPRequest lastRemoteAddress req, cost = 1) {
      env.game.crosstableApi.fetchOrEmpty(u1, u2) map { ct =>
        toApiResult {
          lila.game.JsonView.crosstableWrites.writes(ct).some
        }
      }
    }
  }

  def currentTournaments = ApiRequest { implicit req =>
    implicit val lang = reqLang
    env.tournament.api.fetchVisibleTournaments flatMap
      env.tournament.apiJsonView.apply map Data.apply
  }

  def tournament(id: String) = ApiRequest { implicit req =>
    env.tournament.tournamentRepo byId id flatMap {
      _ ?? { tour =>
        val page = (getInt("page", req) | 1) atLeast 1 atMost 200
        env.tournament.jsonView(
          tour = tour,
          page = page.some,
          me = none,
          getUserTeamIds = _ => fuccess(Nil),
          getTeamName = env.team.getTeamName.apply _,
          playerInfoExt = none,
          socketVersion = none,
          partial = false
        )(reqLang) map some
      }
    } map toApiResult
  }

  def tournamentGames(id: String) = Action.async { req =>
    env.tournament.tournamentRepo byId id flatMap {
      _ ?? { tour =>
        val config = GameApiV2.ByTournamentConfig(
          tournamentId = tour.id,
          format = GameApiV2.Format byRequest req,
          flags = gameC.requestPgnFlags(req, extended = false),
          perSecond = MaxPerSecond(20)
        )
        GlobalConcurrencyLimitPerIP(HTTPRequest lastRemoteAddress req)(
          env.api.gameApiV2.exportByTournament(config)
        ) { source =>
          Ok.chunked(source).as(gameC gameContentType config) |> noProxyBuffer
        }.fuccess
      }
    }
  }

  def tournamentResults(id: String) = Action.async { implicit req =>
    env.tournament.tournamentRepo byId id flatMap {
      _ ?? { tour =>
        import lila.tournament.JsonView.playerResultWrites
        val nb = getInt("nb", req) | Int.MaxValue
        jsonStream {
          env.tournament.api
            .resultStream(tour, MaxPerSecond(50), nb)
            .map(playerResultWrites.writes)
        }.fuccess
      }
    }
  }

  def tournamentsByOwner(name: String) = Action.async { implicit req =>
    implicit val lang = reqLang
    (name != "lichess") ?? env.user.repo.named(name) flatMap {
      _ ?? { user =>
        val nb = getInt("nb", req) | Int.MaxValue
        jsonStream {
          env.tournament.api
            .byOwnerStream(user, MaxPerSecond(20), nb)
            .mapAsync(1)(env.tournament.apiJsonView.fullJson)
        }.fuccess
      }
    }
  }

  def gamesByUsersStream = Action.async(parse.tolerantText) { implicit req =>
    val userIds = req.body.split(',').view.take(300).map(lila.user.User.normalize).toSet
    jsonStream {
      env.game.gamesByUsersStream(userIds)
    }.fuccess
  }

  private val EventStreamConcurrencyLimitPerUser = new lila.memo.ConcurrencyLimit[String](
    name = "Event Stream API concurrency per user",
    key = "eventStream.concurrency.limit.user",
    ttl = 10 minutes,
    maxConcurrency = 1
  )
  def eventStream = Scoped(_.Bot.Play, _.Challenge.Read) { _ => me =>
    env.round.proxyRepo.urgentGames(me) flatMap { povs =>
      env.challenge.api.createdByDestId(me.id) map { challenges =>
        EventStreamConcurrencyLimitPerUser(me.id)(
          env.api.eventStream(me, povs.map(_.game), challenges)
        )(sourceToNdJsonOption)
      }
    }
  }

  private val UserActivityRateLimitPerIP = new lila.memo.RateLimit[IpAddress](
    credits = 15,
    duration = 2 minutes,
    name = "user activity API per IP",
    key = "user_activity.api.ip"
  )

  def activity(name: String) = ApiRequest { implicit req =>
    implicit val lang = reqLang
    UserActivityRateLimitPerIP(HTTPRequest lastRemoteAddress req, cost = 1) {
      lila.mon.api.activity.increment(1)
      env.user.repo named name flatMap {
        _ ?? { user =>
          env.activity.read.recent(user) flatMap {
            _.map { env.activity.jsonView(_, user) }.sequenceFu
          }
        }
      } map toApiResult
    }
  }

  def CookieBasedApiRequest(js: Context => Fu[ApiResult]) = Open { ctx =>
    js(ctx) map toHttp
  }
  def ApiRequest(js: RequestHeader => Fu[ApiResult]) = Action.async { req =>
    js(req) map toHttp
  }
  def MobileApiRequest(js: RequestHeader => Fu[ApiResult]) = Action.async { req =>
    if (lila.api.Mobile.Api requested req) js(req) map toHttp
    else fuccess(NotFound)
  }

  lazy val tooManyRequests =
    Results.TooManyRequests(jsonError("Error 429: Too many requests! Try again later."))
  def toApiResult(json: Option[JsValue]): ApiResult = json.fold[ApiResult](NoData)(Data.apply)
  def toApiResult(json: Seq[JsValue]): ApiResult    = Data(JsArray(json))

  def toHttp(result: ApiResult): Result = result match {
    case Limited        => tooManyRequests
    case NoData         => NotFound
    case Custom(result) => result
    case Data(json)     => Ok(json) as JSON
  }

  def jsonStream(makeSource: => Source[JsValue, _])(implicit req: RequestHeader): Result =
    GlobalConcurrencyLimitPerIP(HTTPRequest lastRemoteAddress req)(makeSource)(sourceToNdJson)

  def sourceToNdJson(source: Source[JsValue, _]) =
    sourceToNdJsonString {
      source.map { o =>
        Json.stringify(o) + "\n"
      }
    }

  def sourceToNdJsonOption(source: Source[Option[JsValue], _]) =
    sourceToNdJsonString {
      source.map { _ ?? Json.stringify + "\n" }
    }

  private def sourceToNdJsonString(source: Source[String, _]) =
    Ok.chunked(source).as(ndJsonContentType) |> noProxyBuffer

  private[controllers] val GlobalConcurrencyLimitPerIP = new lila.memo.ConcurrencyLimit[IpAddress](
    name = "API concurrency per IP",
    key = "api.ip",
    ttl = 1 hour,
    maxConcurrency = 2
  )
  private[controllers] val GlobalConcurrencyLimitUser = new lila.memo.ConcurrencyLimit[lila.user.User.ID](
    name = "API concurrency per user",
    key = "api.user",
    ttl = 1 hour,
    maxConcurrency = 1
  )
  private[controllers] def GlobalConcurrencyLimitPerUserOption[T](
      user: Option[lila.user.User]
  ): Option[SourceIdentity[T]] =
    user.fold(some[SourceIdentity[T]](identity _)) { u =>
      GlobalConcurrencyLimitUser.compose[T](u.id)
    }

  private[controllers] def GlobalConcurrencyLimitPerIpAndUserOption[T](
      req: RequestHeader,
      me: Option[lila.user.User]
  )(makeSource: => Source[T, _])(makeResult: Source[T, _] => Result): Result =
    GlobalConcurrencyLimitPerIP.compose[T](HTTPRequest lastRemoteAddress req) flatMap { limitIp =>
      GlobalConcurrencyLimitPerUserOption[T](me) map { limitUser =>
        makeResult(limitIp(limitUser(makeSource)))
      }
    } getOrElse lila.memo.ConcurrencyLimit.limitedDefault(1)

  private type SourceIdentity[T] = Source[T, _] => Source[T, _]
}

private[controllers] object Api {

  sealed trait ApiResult
  case class Data(json: JsValue)    extends ApiResult
  case object NoData                extends ApiResult
  case object Limited               extends ApiResult
  case class Custom(result: Result) extends ApiResult
}
