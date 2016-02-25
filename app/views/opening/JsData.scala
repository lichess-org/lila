package views.html.opening

import play.api.libs.json.{ JsArray, Json }
import play.twirl.api.Html

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.opening._

object JsData extends lila.Steroids {

  def apply(
    opening: Opening,
    identified: List[String],
    userInfos: Option[lila.opening.UserInfos],
    play: Boolean,
    attempt: Option[Attempt],
    win: Option[Boolean],
    animationDuration: scala.concurrent.duration.Duration)(implicit ctx: Context) =
    Html(Json.stringify(Json.obj(
      "opening" -> Json.obj(
        "id" -> opening.id,
        "rating" -> opening.perf.intRating,
        "attempts" -> opening.attempts,
        "goal" -> opening.goal,
        "fen" -> opening.fen,
        "color" -> opening.color.name,
        "moves" -> JsArray(opening.qualityMoves.map {
          case QualityMove(move, quality) => Json.obj(
            "uci" -> move.first,
            "san" -> move.line.headOption,
            "cp" -> move.cp,
            "line" -> move.line.mkString(" "),
            "quality" -> quality.name)
        }),
        "url" -> s"$netBaseUrl${routes.Opening.show(opening.id)}",
        "identified" -> identified
      ),
      "pref" -> Json.obj(
        "coords" -> ctx.pref.coords
      ),
      "animation" -> Json.obj(
        "duration" -> ctx.pref.animationFactor * animationDuration.toMillis
      ),
      "attempt" -> attempt.map { a =>
        Json.obj(
          "userRatingDiff" -> a.userRatingDiff,
          "win" -> a.win)
      },
      "win" -> win,
      "user" -> userInfos.map { i =>
        Json.obj(
          "rating" -> i.user.perfs.opening.intRating,
          "history" -> i.history.nonEmpty.option(Json.toJson(i.chart))
        )
      },
      "play" -> play)))
}
