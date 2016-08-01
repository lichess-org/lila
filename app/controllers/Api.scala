package controllers

import play.api.libs.json._
import play.api.mvc._, Results._
import scala.concurrent.duration._

import lila.app._
import lila.common.HTTPRequest

object Api extends LilaController {

  private val userApi = Env.api.userApi
  private val gameApi = Env.api.gameApi

  def status = Action { req =>
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

  def user(name: String) = ApiResult { implicit ctx =>
    userApi one name
  }

  def users = ApiResult { implicit ctx =>
    get("team") ?? { teamId =>
      userApi.list(
        teamId = teamId,
        engine = getBoolOpt("engine"),
        nb = getInt("nb")
      ).map(_.some)
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

  def userGames(name: String) = ApiResult { implicit ctx =>
    val page = (getInt("page") | 1) max 1 min 200
    val nb = (getInt("nb") | 10) max 1 min 100
    val cost = page * nb + 10
    GamesRateLimitPerIP(ctx.req.remoteAddress, cost = cost) {
      GamesRateLimitPerUA(~HTTPRequest.userAgent(ctx.req), cost = cost) {
        GamesRateLimitGlobal("", cost = cost) {
          lila.user.UserRepo named name flatMap {
            _ ?? { user =>
              gameApi.byUser(
                user = user,
                rated = getBoolOpt("rated"),
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
          }
        }
      }
    }
  }

  def game(id: String) = ApiResult { implicit ctx =>
    gameApi.one(
      id = id take lila.game.Game.gameIdSize,
      withAnalysis = getBool("with_analysis"),
      withMoves = getBool("with_moves"),
      withOpening = getBool("with_opening"),
      withFens = getBool("with_fens"),
      withMoveTimes = getBool("with_movetimes"),
      token = get("token"))
  }

  private def ApiResult(js: lila.api.Context => Fu[Option[JsValue]]) = Open { implicit ctx =>
    js(ctx) map {
      case None => NotFound
      case Some(json) => get("callback") match {
        case None           => Ok(json) as JSON
        case Some(callback) => Ok(s"$callback($json)") as JAVASCRIPT
      }
    }
  }
}
