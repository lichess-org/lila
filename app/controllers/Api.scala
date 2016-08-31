package controllers

import play.api.libs.json._
import play.api.mvc._, Results._
import scala.concurrent.duration._

import lila.app._
import lila.common.HTTPRequest

object Api extends LilaController {

  private val userApi = Env.api.userApi
  private val gameApi = Env.api.gameApi

  private implicit val limitedDefault = ornicar.scalalib.Zero.instance[ApiResult](Limited)

  private lazy val apiStatusResponse = {
    val api = lila.api.Mobile.Api
    Ok(Json.obj(
      "api" -> Json.obj(
        "current" -> api.currentVersion.value,
        "olds" -> api.oldVersions.map { old =>
          Json.obj(
            "version" -> old.version.value,
            "deprecatedAt" -> old.deprecatedAt,
            "unsupportedAt" -> old.unsupportedAt)
        })
    )) as JSON
  }

  val status = Action { req =>
    apiStatusResponse
  }

  def user(name: String) = ApiRequest { implicit ctx =>
    userApi one name map toApiResult
  }

  private val UsersRateLimitGlobal = new lila.memo.RateLimit(
    credits = 1000,
    duration = 1 minute,
    name = "team users API global")

  private val UsersRateLimitPerIP = new lila.memo.RateLimit(
    credits = 1000,
    duration = 10 minutes,
    name = "team users API per IP")

  def users = ApiRequest { implicit ctx =>
    val page = (getInt("page") | 1) atLeast 1 atMost 50
    val nb = (getInt("nb") | 10) atLeast 1 atMost 50
    val cost = page * nb + 10
    val ip = HTTPRequest lastRemoteAddress ctx.req
    UsersRateLimitPerIP(ip, cost = cost, msg = ip) {
      UsersRateLimitGlobal("-", cost = cost, msg = ip) {
        lila.mon.api.teamUsers.cost(cost)
        (get("team") ?? Env.team.api.team).flatMap {
          _ ?? { team =>
            Env.team.pager(team, page, nb) map userApi.pager map some
          }
        } map toApiResult
      }
    }
  }

  private val GamesRateLimitPerIP = new lila.memo.RateLimit(
    credits = 10 * 1000,
    duration = 10 minutes,
    name = "user games API per IP")

  private val GamesRateLimitPerUA = new lila.memo.RateLimit(
    credits = 10 * 1000,
    duration = 5 minutes,
    name = "user games API per UA")

  private val GamesRateLimitGlobal = new lila.memo.RateLimit(
    credits = 10 * 1000,
    duration = 1 minute,
    name = "user games API global")

  def userGames(name: String) = ApiRequest { implicit ctx =>
    val page = (getInt("page") | 1) atLeast 1 atMost 200
    val nb = (getInt("nb") | 10) atLeast 1 atMost 100
    val cost = page * nb + 10
    val ip = HTTPRequest lastRemoteAddress ctx.req
    GamesRateLimitPerIP(ip, cost = cost, msg = ip) {
      GamesRateLimitPerUA(~HTTPRequest.userAgent(ctx.req), cost = cost, msg = ip) {
        GamesRateLimitGlobal("-", cost = cost, msg = ip) {
          lila.mon.api.userGames.cost(cost)
          lila.user.UserRepo named name flatMap {
            _ ?? { user =>
              gameApi.byUser(
                user = user,
                rated = getBoolOpt("rated"),
                playing = getBoolOpt("playing"),
                analysed = getBoolOpt("analysed"),
                withAnalysis = getBool("with_analysis"),
                withMoves = getBool("with_moves"),
                withOpening = getBool("with_opening"),
                withMoveTimes = getBool("with_movetimes"),
                token = get("token"),
                nb = nb,
                page = page
              ) map some
            }
          } map toApiResult
        }
      }
    }
  }

  private val GameRateLimitPerIdAndIP = new lila.memo.RateLimit(
    credits = 5,
    duration = 3 minutes,
    name = "game API per Id/IP")

  def game(id: String) = ApiRequest { implicit ctx =>
    val ip = HTTPRequest lastRemoteAddress ctx.req
    val key = s"$id:$ip"
    GamesRateLimitPerIP(key, cost = 1, msg = key) {
      lila.mon.api.game.cost(1)
      gameApi.one(
        id = id take lila.game.Game.gameIdSize,
        withAnalysis = getBool("with_analysis"),
        withMoves = getBool("with_moves"),
        withOpening = getBool("with_opening"),
        withFens = getBool("with_fens"),
        withMoveTimes = getBool("with_movetimes"),
        token = get("token")) map toApiResult
    }
  }

  def currentTournaments = ApiRequest { implicit ctx =>
    Env.tournament.api.fetchVisibleTournaments map
      Env.tournament.scheduleJsonView.apply map Data.apply
  }

  def tournament(id: String) = ApiRequest { implicit ctx =>
    val page = (getInt("page") | 1) atLeast 1 atMost 200
    lila.tournament.TournamentRepo byId id flatMap {
      _ ?? { tour =>
        Env.tournament.jsonView(tour, page.some, none, none, none) map some
      }
    } map toApiResult
  }

  sealed trait ApiResult
  case class Data(json: JsValue) extends ApiResult
  case object NoData extends ApiResult
  case object Limited extends ApiResult
  def toApiResult(json: Option[JsValue]) = json.fold[ApiResult](NoData)(Data.apply)

  private def ApiRequest(js: lila.api.Context => Fu[ApiResult]) = Open { implicit ctx =>
    js(ctx) map {
      case Limited => TooManyRequest(jsonError("Try again later"))
      case NoData  => NotFound
      case Data(json) => get("callback") match {
        case None           => Ok(json) as JSON
        case Some(callback) => Ok(s"$callback($json)") as JAVASCRIPT
      }
    }
  }
}
