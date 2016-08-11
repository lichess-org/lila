package controllers

import play.api.libs.json._
import play.api.mvc._, Results._
import scala.concurrent.duration._

import lila.app._
import lila.common.HTTPRequest

object Api extends LilaController {

  private val userApi = Env.api.userApi
  private val gameApi = Env.api.gameApi

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

  def users = ApiRequest { implicit ctx =>
    (get("team") ?? Env.team.api.team).flatMap {
      _ ?? { team =>
        val page = (getInt("page") | 1) max 1 min 50
        val nb = (getInt("nb") | 10) max 1 min 50
        Env.team.pager(team, page, nb) map userApi.pager map some
      }
    } map toApiResult
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
    val page = (getInt("page") | 1) max 1 min 200
    val nb = (getInt("nb") | 10) max 1 min 100
    val cost = page * nb + 10
    val ip = HTTPRequest lastRemoteAddress ctx.req
    implicit val default = ornicar.scalalib.Zero.instance[ApiResult](Limited)
    GamesRateLimitPerIP(ip, cost = cost, msg = ip) {
      GamesRateLimitPerUA(~HTTPRequest.userAgent(ctx.req), cost = cost, msg = ip) {
        GamesRateLimitGlobal("-", cost = cost, msg = ip) {
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

  def game(id: String) = ApiRequest { implicit ctx =>
    gameApi.one(
      id = id take lila.game.Game.gameIdSize,
      withAnalysis = getBool("with_analysis"),
      withMoves = getBool("with_moves"),
      withOpening = getBool("with_opening"),
      withFens = getBool("with_fens"),
      withMoveTimes = getBool("with_movetimes"),
      token = get("token")) map toApiResult
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
