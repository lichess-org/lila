package controllers

import play.api.libs.json.JsValue
import play.api.mvc._, Results._

import lila.app._

object Api extends LilaController {

  private val userApi = Env.api.userApi
  private val gameApi = Env.api.gameApi
  private val analysisApi = Env.api.analysisApi
  private val puzzleApi = Env.api.puzzleApi

  def user(username: String) = ApiResult { req =>
    userApi.one(
      username = username,
      token = get("token", req))
  }

  def users = ApiResult { req =>
    userApi.list(
      team = get("team", req),
      engine = get("engine", req) map ("1"==),
      token = get("token", req),
      nb = getInt("nb", req)
    ) map (_.some)
  }

  def games = ApiResult { req =>
    gameApi.list(
      username = get("username", req),
      rated = get("rated", req) map ("1"==),
      token = get("token", req),
      nb = getInt("nb", req)
    ) map (_.some)
  }

  def analysis = ApiResult { req =>
    analysisApi.list(
      nb = getInt("nb", req),
      token = get("token", req)
    ) map (_.some)
  }

  def puzzle(id: String) = ApiResult { req =>
    (id, parseIntOption(id)) match {
      case ("daily", _) => puzzleApi.daily
      case (_, Some(i)) => puzzleApi one i
      case _            => fuccess(none)
    }
  }

  private def ApiResult(js: RequestHeader => Fu[Option[JsValue]]) = Action async { req =>
    js(req) map {
      case None => NotFound
      case Some(json) => get("callback", req) match {
        case None           => Ok(json) as JSON
        case Some(callback) => Ok(s"$callback($json)") as JAVASCRIPT
      }
    }
  }
}
