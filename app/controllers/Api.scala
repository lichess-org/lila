package controllers

import org.joda.time.DateTime
import ornicar.scalalib.Zero
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.api.{ Context, GameApiV2, UserApi }
import lila.app._
import lila.common.PimpedJson._
import lila.common.{ HTTPRequest, IpAddress, MaxPerPage, MaxPerSecond }

object Api extends LilaController {

  private val userApi = Env.api.userApi
  private val gameApi = Env.api.gameApi

  private[controllers] implicit val limitedDefault = Zero.instance[ApiResult](Limited)

  private lazy val apiStatusJson = {
    val api = lila.api.Mobile.Api
    Json.obj(
      "api" -> Json.obj(
        "current" -> api.currentVersion.value,
        "olds" -> api.oldVersions.map { old =>
          Json.obj(
            "version" -> old.version.value,
            "deprecatedAt" -> old.deprecatedAt,
            "unsupportedAt" -> old.unsupportedAt
          )
        }
      )
    )
  }

  val status = Action { req =>
    val appVersion = get("v", req)
    lila.mon.mobile.version(appVersion | "none")()
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
    val ip = HTTPRequest lastRemoteAddress req
    val cost = usernames.size / 4
    UsersRateLimitPerIP(ip, cost = cost) {
      UsersRateLimitGlobal("-", cost = cost, msg = ip.value) {
        lila.mon.api.users.cost(cost)
        lila.user.UserRepo nameds usernames map {
          _.map { Env.user.jsonView(_, none) }
        } map toApiResult map toHttp
      }
    }
  }

  def usersStatus = ApiRequest { req =>
    val ids = get("ids", req).??(_.split(',').take(50).toList map lila.user.User.normalize)
    Env.user.lightUserApi asyncMany ids dmap (_.flatten) map { users =>
      val actualIds = users.map(_.id)
      val onlineIds = Env.user.onlineUserIdMemo intersect actualIds
      val playingIds = Env.relation.online.playing intersect actualIds
      val streamingIds = Env.streamer.liveStreamApi.userIds
      toApiResult {
        users.map { u =>
          lila.common.LightUser.lightUserWrites.writes(u)
            .add("online" -> onlineIds(u.id))
            .add("playing" -> playingIds(u.id))
            .add("streaming" -> streamingIds(u.id))
        }
      }
    }
  }

  def titledUsers = Action.async { req =>
    val titles = lila.user.Title get get("titles", req).??(_.split(',').take(20).toList)
    GlobalLinearLimitPerIP(HTTPRequest lastRemoteAddress req) {
      val config = UserApi.Titled(
        titles = lila.user.Title get get("titles", req).??(_.split(',').take(20).toList),
        online = getBool("online", req),
        perSecond = MaxPerSecond(50)
      )
      jsonStream(Env.api.userApi.exportTitled(config)).fuccess
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
    val nb = (getInt("nb", req) | 10) atLeast 1 atMost 100
    val cost = page * nb + 10
    UserGamesRateLimit(cost, req) {
      lila.mon.api.userGames.cost(cost)
      lila.user.UserRepo named name flatMap {
        _ ?? { user =>
          gameApi.byUser(
            user = user,
            rated = getBoolOpt("rated", req),
            playing = getBoolOpt("playing", req),
            analysed = getBoolOpt("analysed", req),
            withFlags = gameFlagsFromRequest(req),
            nb = MaxPerPage(nb),
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
      lila.mon.api.game.cost(1)
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
      Env.game.crosstableApi(u1, u2, timeout = 15.seconds) map { ct =>
        toApiResult {
          ct map lila.game.JsonView.crosstableWrites.writes
        }
      }
    }
  }

  def gamesVsTeam(teamId: String) = ApiRequest { req =>
    Env.team.api team teamId flatMap {
      case None => fuccess {
        Custom { BadRequest(jsonError("No such team.")) }
      }
      case Some(team) if team.nbMembers > 200 => fuccess {
        Custom { BadRequest(jsonError(s"The team has too many players. ${team.nbMembers} > 200")) }
      }
      case Some(team) =>
        lila.team.MemberRepo.userIdsByTeam(team.id) flatMap { userIds =>
          val page = (getInt("page", req) | 1) atLeast 1 atMost 200
          val nb = (getInt("nb", req) | 10) atLeast 1 atMost 100
          val cost = page * nb * 5 + 10
          UserGamesRateLimit(cost, req) {
            lila.mon.api.userGames.cost(cost)
            gameApi.byUsersVs(
              userIds = userIds,
              rated = getBoolOpt("rated", req),
              playing = getBoolOpt("playing", req),
              analysed = getBoolOpt("analysed", req),
              withFlags = gameFlagsFromRequest(req),
              since = DateTime.now minusYears 1,
              nb = MaxPerPage(nb),
              page = page
            ) map some map toApiResult
          }
        }
    }
  }

  def currentTournaments = ApiRequest { implicit ctx =>
    Env.tournament.api.fetchVisibleTournaments flatMap
      Env.tournament.scheduleJsonView.apply map Data.apply
  }

  def tournament(id: String) = ApiRequest { req =>
    lila.tournament.TournamentRepo byId id flatMap {
      _ ?? { tour =>
        val page = (getInt("page", req) | 1) atLeast 1 atMost 200
        Env.tournament.jsonView(tour, page.some, none, { _ => fuccess(Nil) }, none, none, partial = false, lila.i18n.defaultLang) map some
      }
    } map toApiResult
  }

  def tournamentGames(id: String) = Action.async { req =>
    lila.tournament.TournamentRepo byId id flatMap {
      _ ?? { tour =>
        GlobalLinearLimitPerIP(HTTPRequest lastRemoteAddress req) {
          val format = GameApiV2.Format byRequest req
          val config = GameApiV2.ByTournamentConfig(
            tournamentId = tour.id,
            format = GameApiV2.Format byRequest req,
            flags = Game.requestPgnFlags(req, extended = false),
            perSecond = MaxPerSecond(20)
          )
          Ok.chunked(Env.api.gameApiV2.exportByTournament(config)).withHeaders(
            noProxyBufferHeader,
            CONTENT_TYPE -> Game.gameContentType(config)
          ).fuccess
        }
      }
    }
  }

  def tournamentResults(id: String) = Action.async { req =>
    lila.tournament.TournamentRepo byId id flatMap {
      _ ?? { tour =>
        GlobalLinearLimitPerIP(HTTPRequest lastRemoteAddress req) {
          import lila.tournament.JsonView.playerResultWrites
          val nb = getInt("nb", req) | Int.MaxValue
          val enumerator = Env.tournament.api.resultStream(tour, 50, nb) &>
            Enumeratee.map(playerResultWrites.writes)
          jsonStream(enumerator).fuccess
        }
      }
    }
  }

  def gamesByUsersStream = Action.async(parse.tolerantText) { req =>
    val userIds = req.body.split(',').take(300).toSet map lila.user.User.normalize
    jsonStream(Env.game.gamesByUsersStream(userIds)).fuccess
  }

  def eventStream = Scoped(_.Bot.Play, _.Challenge.Read) { req => me =>
    lila.game.GameRepo.urgentGames(me) flatMap { povs =>
      Env.challenge.api.createdByDestId(me.id) map { challenges =>
        jsonOptionStream(Env.api.eventStream(me, povs.map(_.game), challenges))
      }
    }
  }

  private val UserActivityRateLimitPerIP = new lila.memo.RateLimit[IpAddress](
    credits = 15,
    duration = 2 minutes,
    name = "user activity API per IP",
    key = "user_activity.api.ip"
  )

  def activity(name: String) = ApiRequest { req =>
    UserActivityRateLimitPerIP(HTTPRequest lastRemoteAddress req, cost = 1) {
      lila.mon.api.activity.cost(1)
      lila.user.UserRepo named name flatMap {
        _ ?? { user =>
          Env.activity.read.recent(user) flatMap {
            _.map { Env.activity.jsonView(_, user) }.sequenceFu
          }
        }
      } map toApiResult
    }
  }

  private[controllers] val GlobalLinearLimitPerIP = new lila.memo.LinearLimit[IpAddress](
    name = "linear API per IP",
    key = "api.ip",
    ttl = 6 hours
  )
  private[controllers] val GlobalLinearLimitPerUser = new lila.memo.LinearLimit[lila.user.User.ID](
    name = "linear API per user",
    key = "api.user",
    ttl = 6 hours
  )
  private[controllers] def GlobalLinearLimitPerUserOption(user: Option[lila.user.User])(f: Fu[Result]): Fu[Result] =
    user.fold(f) { u =>
      GlobalLinearLimitPerUser(u.id)(f)
    }

  sealed trait ApiResult
  case class Data(json: JsValue) extends ApiResult
  case object NoData extends ApiResult
  case object Limited extends ApiResult
  case class Custom(result: Result) extends ApiResult
  def toApiResult(json: Option[JsValue]): ApiResult = json.fold[ApiResult](NoData)(Data.apply)
  def toApiResult(json: Seq[JsValue]): ApiResult = Data(JsArray(json))

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

  private[controllers] val tooManyRequests = TooManyRequest(jsonError("Error 429: Too many requests! Try again later."))

  private[controllers] def toHttp(result: ApiResult): Result = result match {
    case Limited => tooManyRequests
    case NoData => NotFound
    case Custom(result) => result
    case Data(json) => Ok(json) as JSON
  }

  private[controllers] def jsonStream(stream: Enumerator[JsObject]): Result = jsonStringStream {
    stream &> Enumeratee.map { o => Json.stringify(o) + "\n" }
  }

  private[controllers] def jsonOptionStream(stream: Enumerator[Option[JsObject]]): Result = jsonStringStream {
    stream &> Enumeratee.map { _ ?? Json.stringify + "\n" }
  }

  private def jsonStringStream(stream: Enumerator[String]): Result =
    Ok.chunked(stream).withHeaders(CONTENT_TYPE -> ndJsonContentType) |> noProxyBuffer
}
