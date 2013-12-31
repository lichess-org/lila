package controllers

import play.api.libs.json._
import play.api.mvc._, Results._

import lila.app._
import lila.user.{ UserRepo, User ⇒ UserModel, Perf, Perfs }
import lila.game.GameRepo
import lila.app.templating.Environment.netBaseUrl

object Api extends LilaController {

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
  private implicit val userWrites: OWrites[UserModel] = OWrites { u ⇒
    Json.obj(
      "username" -> u.username,
      "perfs" -> u.perfs)
  }

  def user(username: String) = Action.async { req ⇒
    UserRepo named username flatMap {
      case None ⇒ fuccess(NotFound)
      case Some(u) ⇒ GameRepo nowPlaying u.id map { gameOption ⇒
        val json = userWrites.writes(u) ++ Json.obj(
          "online" -> Env.user.isOnline(u.id),
          "playing" -> gameOption.map(g ⇒
            netBaseUrl + routes.Round.watcher(g.id, g.firstPlayer.color.name).url)
        )
        get("callback", req) match {
          case None           ⇒ Ok(json) as JSON
          case Some(callback) ⇒ Ok(s"$callback($json)") as JAVASCRIPT
        }
      }
    }
  }
}
