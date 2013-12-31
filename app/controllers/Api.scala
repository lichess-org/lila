package controllers

import play.api.libs.json._
import play.api.mvc._, Results._

import lila.app._
import lila.user.{ UserRepo, User ⇒ UserModel, Perf, Perfs }

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
  private implicit val userWrites: Writes[UserModel] = Writes { u ⇒
    Json.obj(
      "username" -> u.username,
      "perfs" -> u.perfs,
      "progress" -> u.progress)
  }

  def user(username: String) = Action.async { req ⇒
    UserRepo named username map {
      case None    ⇒ NotFound
      case Some(u) ⇒ Ok(Json toJson u) as JSON
    }
  }
}
