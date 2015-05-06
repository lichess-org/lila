package controllers

import play.api.libs.json._
import play.api.mvc._, Results._

import lila.app._

object Api extends LilaController {

  private val userApi = Env.api.userApi
  private val gameApi = Env.api.gameApi

  def status = Action { req =>
    val api = lila.api.Mobile.Api
    val app = lila.api.Mobile.App
    Ok(Json.obj(
      "api" -> Json.obj(
        "current" -> api.currentVersion,
        "olds" -> api.oldVersions.map { old =>
          Json.obj(
            "version" -> old.version,
            "deprecatedAt" -> old.deprecatedAt,
            "unsupportedAt" -> old.unsupportedAt)
        }),
      "app" -> Json.obj(
        "current" -> app.currentVersion
      )
    )) as JSON
  }

  def user(username: String) = ApiResult { implicit ctx =>
    userApi.one(
      username = username,
      token = get("token"))
  }

  def users = ApiResult { implicit ctx =>
    userApi.list(
      team = get("team"),
      engine = getBoolOpt("engine"),
      token = get("token"),
      nb = getInt("nb")
    ) map (_.some)
  }

  def games = ApiResult { implicit ctx =>
    gameApi.list(
      username = get("username"),
      rated = getBoolOpt("rated"),
      analysed = getBoolOpt("analysed"),
      withAnalysis = getBool("with_analysis"),
      withMoves = getBool("with_moves"),
      withOpening = getBool("with_opening"),
      token = get("token"),
      nb = getInt("nb")
    ) map (_.some)
  }

  def game(id: String) = ApiResult { implicit ctx =>
    gameApi.one(
      id = id take lila.game.Game.gameIdSize,
      withAnalysis = getBool("with_analysis"),
      withMoves = getBool("with_moves"),
      withOpening = getBool("with_opening"),
      withFens = getBool("with_fens"),
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
