package lila.api

import play.api.libs.json._

import lila.common.PimpedJson._
import lila.db.api._
import lila.db.Implicits._
import lila.game.GameRepo
import lila.hub.actorApi.{ router => R }
import lila.rating.Perf
import lila.user.tube.userTube
import lila.user.{ UserRepo, User, Perfs }
import makeTimeout.short

private[api] final class UserApi(
    makeUrl: Any => Fu[String],
    apiToken: String,
    userIdsSharingIp: String => Fu[List[String]],
    isOnline: String => Boolean) {

  private implicit val perfWrites: Writes[Perf] = Writes { o =>
    Json.obj(
      "nbGames" -> o.nb,
      "rating" -> o.glicko.rating.toInt,
      "deviation" -> o.glicko.deviation.toInt)
  }
  private implicit val perfsWrites: Writes[Perfs] = Writes { o =>
    JsObject(o.perfs map {
      case (name, perf) => name -> perfWrites.writes(perf)
    })
  }
  private implicit val userWrites: OWrites[User] = OWrites { u =>
    Json.obj(
      "username" -> u.username,
      "rating" -> u.rating,
      "rd" -> u.perfs.global.glicko.deviation,
      "progress" -> u.progress)
  }

  def list(
    team: Option[String],
    token: Option[String],
    nb: Option[Int],
    engine: Option[Boolean]): Fu[JsObject] = (team match {
    case Some(teamId) => lila.team.MemberRepo.userIdsByTeam(teamId) flatMap UserRepo.enabledByIds
    case None => $find(pimpQB($query(
      UserRepo.enabledSelect ++ (engine ?? UserRepo.engineSelect)
    )) sort ((~engine).fold(
      UserRepo.sortCreatedAtDesc,
      UserRepo.sortRatingDesc
    )), makeNb(nb, token))
  }) flatMap { users =>
    users.map(u => makeUrl(R User u.username)).sequenceFu map { urls =>
      Json.obj(
        "list" -> JsArray(
          users zip urls map {
            case (u, url) => userWrites.writes(u) ++ Json.obj(
              "url" -> url,
              "online" -> isOnline(u.id),
              "engine" -> u.engine
            ).noNull
          }
        )
      )
    }
  }

  def one(username: String, token: Option[String]): Fu[Option[JsObject]] = UserRepo named username flatMap {
    case None => fuccess(none)
    case Some(u) => GameRepo nowPlaying u.id zip
      makeUrl(R User username) zip
      (check(token) ?? (knownEnginesSharingIp(u.id) map (_.some))) flatMap {
        case ((gameOption, userUrl), knownEngines) => gameOption ?? { g =>
          makeUrl(R.Watcher(g.id, g.firstPlayer.color.name)) map (_.some)
        } map { gameUrlOption =>
          userWrites.writes(u) ++ Json.obj(
            "url" -> userUrl,
            "online" -> isOnline(u.id),
            "playing" -> gameUrlOption,
            "engine" -> u.engine,
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
