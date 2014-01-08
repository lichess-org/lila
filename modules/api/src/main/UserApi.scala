package lila.api

import play.api.libs.json._

import lila.common.PimpedJson._
import lila.game.GameRepo
import lila.hub.actorApi.{ router ⇒ R }
import lila.user.{ UserRepo, User, Perf, Perfs }
import makeTimeout.short

private[api] final class UserApi(
    makeUrl: Any ⇒ Fu[String],
    apiToken: String,
    isOnline: String ⇒ Boolean) {

  private implicit val perfWrites: Writes[Perf] = Writes { o ⇒
    Json.obj(
      "nbGames" -> o.nb,
      "rating" -> o.glicko.rating.toInt,
      "deviation" -> o.glicko.deviation.toInt)
  }
  private implicit val perfsWrites: Writes[Perfs] = Writes { o ⇒
    JsObject(o.perfs map {
      case (name, perf) ⇒ name -> perfWrites.writes(perf)
    })
  }
  private implicit val userWrites: OWrites[User] = OWrites { u ⇒
    Json.obj(
      "username" -> u.username,
      "perfs" -> u.perfs)
  }

  def one(username: String, token: Option[String]): Fu[Option[JsObject]] = UserRepo named username flatMap {
    case None ⇒ fuccess(none)
    case Some(u) ⇒ GameRepo nowPlaying u.id zip makeUrl(R User username) flatMap {
      case (gameOption, userUrl) ⇒ gameOption ?? { g ⇒
        makeUrl(R.Watcher(g.id, g.firstPlayer.color.name)) map (_.some)
      } map { gameUrlOption ⇒
        userWrites.writes(u) ++ Json.obj(
          "url" -> userUrl,
          "online" -> isOnline(u.id),
          "playing" -> gameUrlOption,
          "engine" -> u.engine
        ).noNull
      }
    } map (_.some)
  }
}
