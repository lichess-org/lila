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
    userInfos: Option[lila.opening.UserInfos],
    play: Boolean)(implicit ctx: Context) =
    Html(Json.stringify(Json.obj(
      "opening" -> Json.obj(
        "id" -> opening.id,
        "score" -> opening.score.toInt,
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
        "url" -> s"$netBaseUrl${routes.Opening.show(opening.id)}"
      ),
      "user" -> userInfos.map { i =>
        Json.obj(
          "score" -> i.score,
          "history" -> i.history.nonEmpty.option(Json.toJson(i.chart))
        )
      },
      "play" -> play)))
}
