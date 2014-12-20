package lila.api

import play.api.libs.json._

import lila.common.PimpedJson._
import lila.db.api._
import lila.db.Implicits._
import lila.game.GameRepo
import lila.hub.actorApi.{ router => R }
import lila.rating.Perf
import lila.user.tube.userTube
import lila.user.{ UserRepo, User, Perfs, Profile }
import makeTimeout.short

private[api] final class UserApi(
    jsonView: lila.user.JsonView,
    makeUrl: Any => Fu[String],
    apiToken: String,
    userIdsSharingIp: String => Fu[List[String]]) {

  def list(
    team: Option[String],
    token: Option[String],
    nb: Option[Int],
    engine: Option[Boolean]): Fu[JsObject] = (team match {
    case Some(teamId) => lila.team.MemberRepo.userIdsByTeam(teamId) flatMap UserRepo.enabledByIds
    case None => $find(pimpQB($query(
      UserRepo.enabledSelect ++ (engine ?? UserRepo.engineSelect)
    )) sort UserRepo.sortPerfDesc(lila.rating.PerfType.Standard.key), makeNb(nb, token))
  }) flatMap { users =>
    users.map(u => makeUrl(R User u.username)).sequenceFu map { urls =>
      Json.obj(
        "list" -> JsArray(
          users zip urls map {
            case (u, url) => jsonView(u, extended = team.isDefined) ++ Json.obj("url" -> url).noNull
          }
        )
      )
    }
  }

  def one(username: String, token: Option[String]): Fu[Option[JsObject]] = UserRepo named username flatMap {
    case None => fuccess(none)
    case Some(u) => GameRepo onePlaying u zip
      makeUrl(R User username) zip
      (check(token) ?? (knownEnginesSharingIp(u.id) map (_.some))) flatMap {
        case ((gameOption, userUrl), knownEngines) => gameOption ?? { g =>
          makeUrl(R.Watcher(g.gameId, g.color.name)) map (_.some)
        } map { gameUrlOption =>
          jsonView(u, extended = true) ++ Json.obj(
            "url" -> userUrl,
            "playing" -> gameUrlOption,
            "knownEnginesSharingIp" -> knownEngines
          ).noNull
        }
      } map (_.some)
  }

  def knownEnginesSharingIp(userId: String): Fu[List[String]] =
    userIdsSharingIp(userId) flatMap UserRepo.filterByEngine

  private def makeNb(nb: Option[Int], token: Option[String]) = math.min(check(token) ? 1000 | 100, nb | 10)

  private def check(token: Option[String]) = token ?? (apiToken==)
}
