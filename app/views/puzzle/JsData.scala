package views.html.puzzle

import play.api.libs.json.{ JsArray, Json }
import play.twirl.api.Html

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._

object JsData extends lila.Steroids {

  def apply(
    puzzle: lila.puzzle.Puzzle,
    userInfos: Option[lila.puzzle.UserInfos],
    mode: String)(implicit ctx: Context) =
    Html(Json.stringify(Json.obj(
      "mode" -> mode,
      "fen" -> puzzle.fen,
      "color" -> puzzle.color.name,
      "initialMove" -> puzzle.initialMove,
      "lines" -> lila.puzzle.Line.toJson(puzzle.lines),
      "user" -> userInfos.map { i =>
        Json.obj(
          "rating" -> i.user.perfs.puzzle.intRating,
          "history" -> i.history.nonEmpty.option(Json.toJson(i.chart))
        )
      },
      "difficulty" -> ctx.isAuth.option {
        Json.obj(
          "choices" -> JsArray(lila.pref.Pref.Difficulty.choices.map {
            case (k, v) => Json.arr(k, v)
          }),
          "current" -> ctx.pref.puzzleDifficulty
        )
      },
      "urls" -> Json.obj(
        "post" -> routes.Puzzle.attempt(puzzle.id).url,
        "history" -> ctx.isAuth.option(routes.Puzzle.history.url),
        "difficulty" -> ctx.isAuth.option(routes.Puzzle.difficulty.url),
        "puzzle" -> routes.Puzzle.home.url,
        "coordinate" -> routes.Coordinate.home.url
      ),
      "i18n" -> i18nJsObject(
        trans.training,
        trans.yourPuzzleRatingX,
        trans.goodMove,
        trans.butYouCanDoBetter,
        trans.bestMove,
        trans.keepGoing,
        trans.puzzleFailed,
        trans.butYouCanKeepTrying,
        trans.yourTurn,
        trans.waiting,
        trans.findTheBestMoveForBlack,
        trans.findTheBestMoveForWhite,
        trans.giveUp)
    )))
}
