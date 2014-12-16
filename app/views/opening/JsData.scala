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
    userInfos: Option[lila.opening.UserInfos])(implicit ctx: Context) =
    Html(Json.stringify(Json.obj(
      "opening" -> Json.obj(
        "id" -> opening.id,
        "attempts" -> opening.attempts,
        "fen" -> opening.fen,
        "color" -> opening.color.name,
        "moves" -> JsArray(opening.scoredMoves.map {
          case ScoredMove(move, score) => Json.obj(
            "first" -> move.first,
            "cp" -> move.cp,
            "line" -> move.line.mkString(" "),
            "score" -> score.name)
        }),
        "url" -> s"$netBaseUrl${routes.Opening.show(opening.id)}"
      ))))
}
