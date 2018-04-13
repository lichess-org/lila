package controllers

import org.joda.time.DateTime
import ornicar.scalalib.Zero
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.api.Context
import lila.app._
import lila.common.PimpedJson._
import lila.common.{ HTTPRequest, IpAddress, MaxPerPage }

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
    Ok(views.html.site.api())
  }

  def user(name: String) = ApiRequest { implicit ctx =>
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

  def usersByIds = OpenBody(parse.tolerantText) { implicit ctx =>
    val usernames = ctx.body.body.split(',').take(300).toList
    val ip = HTTPRequest lastRemoteAddress ctx.req
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

  def usersStatus = ApiRequest { implicit ctx =>
    val ids = get("ids").??(_.split(',').take(50).toList map lila.user.User.normalize)
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
    credits = 15 * 1000,
    duration = 2 minute,
    name = "user games API global",
    key = "user_games.api.global"
  )

  private def UserRateLimit(cost: Int)(run: => Fu[ApiResult])(implicit ctx: Context) = {
    val ip = HTTPRequest lastRemoteAddress ctx.req
    UserGamesRateLimitPerIP(ip, cost = cost) {
      UserGamesRateLimitPerUA(~HTTPRequest.userAgent(ctx.req), cost = cost, msg = ip.value) {
        UserGamesRateLimitGlobal("-", cost = cost, msg = ip.value) {
          run
        }
      }
    }
  }

  private def gameFlagsFromRequest(implicit ctx: Context) =
    lila.api.GameApi.WithFlags(
      analysis = getBool("with_analysis"),
      moves = getBool("with_moves"),
      fens = getBool("with_fens"),
      opening = getBool("with_opening"),
      moveTimes = getBool("with_movetimes"),
      token = get("token")
    )

  def userGames(name: String) = ApiRequest { implicit ctx =>
    val page = (getInt("page") | 1) atLeast 1 atMost 200
    val nb = (getInt("nb") | 10) atLeast 1 atMost 100
    val cost = page * nb + 10
    UserRateLimit(cost = cost) {
      lila.mon.api.userGames.cost(cost)
      lila.user.UserRepo named name flatMap {
        _ ?? { user =>
          gameApi.byUser(
            user = user,
            rated = getBoolOpt("rated"),
            playing = getBoolOpt("playing"),
            analysed = getBoolOpt("analysed"),
            withFlags = gameFlagsFromRequest,
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

  def game(id: String) = ApiRequest { implicit ctx =>
    val ip = HTTPRequest lastRemoteAddress ctx.req
    GameRateLimitPerIP(ip, cost = 1) {
      lila.mon.api.game.cost(1)
      gameApi.one(id take lila.game.Game.gameIdSize, gameFlagsFromRequest) map toApiResult
    }
  }

  def games = OpenBody(parse.tolerantText) { implicit ctx =>
    val gameIds = ctx.body.body.split(',').take(300)
    val ip = HTTPRequest lastRemoteAddress ctx.req
    GameRateLimitPerIP(ip, cost = gameIds.size / 4) {
      lila.mon.api.game.cost(1)
      gameApi.many(
        ids = gameIds,
        withMoves = getBool("with_moves")
      ) map toApiResult map toHttp
    }(Zero.instance(tooManyRequests.fuccess))
  }

  def gamesVs(u1: String, u2: String) = ApiRequest { implicit ctx =>
    val page = (getInt("page") | 1) atLeast 1 atMost 200
    val nb = (getInt("nb") | 10) atLeast 1 atMost 100
    val cost = page * nb * 2 + 10
    UserRateLimit(cost = cost) {
      lila.mon.api.userGames.cost(cost)
      for {
        usersO <- lila.user.UserRepo.pair(
          lila.user.User.normalize(u1),
          lila.user.User.normalize(u2)
        )
        res <- usersO.?? { users =>
          gameApi.byUsersVs(
            users = users,
            rated = getBoolOpt("rated"),
            playing = getBoolOpt("playing"),
            analysed = getBoolOpt("analysed"),
            withFlags = gameFlagsFromRequest,
            nb = MaxPerPage(nb),
            page = page
          ) map some
        }
      } yield toApiResult(res)
    }
  }

  def crosstable(u1: String, u2: String) = ApiRequest { implicit ctx =>
    UserRateLimit(cost = 200) {
      Env.game.crosstableApi(u1, u2, timeout = 15.seconds) map { ct =>
        toApiResult {
          ct map lila.game.JsonView.crosstableWrites.writes
        }
      }
    }
  }

  def gamesVsTeam(teamId: String) = ApiRequest { implicit ctx =>
    Env.team.api team teamId flatMap {
      case None => fuccess {
        Custom { BadRequest(jsonError("No such team.")) }
      }
      case Some(team) if team.nbMembers > 200 => fuccess {
        Custom { BadRequest(jsonError(s"The team has too many players. ${team.nbMembers} > 200")) }
      }
      case Some(team) =>
        lila.team.MemberRepo.userIdsByTeam(team.id) flatMap { userIds =>
          val page = (getInt("page") | 1) atLeast 1 atMost 200
          val nb = (getInt("nb") | 10) atLeast 1 atMost 100
          val cost = page * nb * 5 + 10
          UserRateLimit(cost = cost) {
            lila.mon.api.userGames.cost(cost)
            gameApi.byUsersVs(
              userIds = userIds,
              rated = getBoolOpt("rated"),
              playing = getBoolOpt("playing"),
              analysed = getBoolOpt("analysed"),
              withFlags = gameFlagsFromRequest,
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

  def tournament(id: String) = ApiRequest { implicit ctx =>
    val page = (getInt("page") | 1) atLeast 1 atMost 200
    lila.tournament.TournamentRepo byId id flatMap {
      _ ?? { tour =>
        Env.tournament.jsonView(tour, page.some, none, none, none, ctx.lang) map some
      }
    } map toApiResult
  }

  def gameStream = Action.async(parse.tolerantText) { req =>
    RequireHttp11(req) {
      val userIds = req.body.split(',').take(300).toSet map lila.user.User.normalize
      Ok.chunked(Env.game.stream.startedByUserIds(userIds)).fuccess
    }
  }

  def activity(name: String) = ApiRequest { implicit ctx =>
    val cost = 50
    UserRateLimit(cost = cost) {
      lila.mon.api.activity.cost(cost)
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
  case class JsonStream(value: Enumerator[JsObject]) extends ApiResult
  case object NoData extends ApiResult
  case object Limited extends ApiResult
  case class Custom(result: Result) extends ApiResult
  def toApiResult(json: Option[JsValue]): ApiResult = json.fold[ApiResult](NoData)(Data.apply)
  def toApiResult(json: Seq[JsValue]): ApiResult = Data(JsArray(json))
  def toApiResult(stream: Enumerator[JsObject]): ApiResult = JsonStream(stream)

  def ApiRequest(js: Context => Fu[ApiResult]) = Open { implicit ctx =>
    js(ctx) map toHttp
  }

  private[controllers] val tooManyRequests = TooManyRequest(jsonError("Try again later"))

  private def toHttp(result: ApiResult)(implicit ctx: Context): Result = result match {
    case Limited => tooManyRequests
    case NoData => NotFound
    case Custom(result) => result
    case JsonStream(stream) =>
      Ok.chunked {
        stream &> Enumeratee.map { o =>
          Json.stringify(o) + "\n"
        }
      }.withHeaders(CONTENT_TYPE -> "application/x-ndjson")
    case Data(json) => get("callback") match {
      case None => Ok(json) as JSON
      case Some(callback) => Ok(s"$callback($json)") as JAVASCRIPT
    }
  }
}
