package views.html.puzzle

import play.api.libs.json.{ JsArray, Json }
import play.twirl.api.Html

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.puzzle._

object JsData extends lila.Steroids {

  def apply(
    puzzle: Puzzle,
    userInfos: Option[lila.puzzle.UserInfos],
    mode: String,
    attempt: Option[Attempt] = None,
    win: Option[Boolean] = None,
    voted: Option[Boolean] = None)(implicit ctx: Context) =
    Html(Json.stringify(Json.obj(
      "puzzle" -> Json.obj(
        "fen" -> puzzle.fen,
        "color" -> puzzle.color.name,
        "initialMove" -> puzzle.initialMove,
        "initialPly" -> puzzle.initialPly,
        "gameId" -> puzzle.gameId,
        "lines" -> lila.puzzle.Line.toJson(puzzle.lines),
        "enabled" -> puzzle.enabled
      ),
      "mode" -> mode,
      "attempt" -> attempt.map { a =>
        Json.obj(
          "userRatingDiff" -> a.userRatingDiff,
          "seconds" -> a.seconds,
          "win" -> a.win,
          "vote" -> a.vote
        )
      },
      "win" -> win,
      "voted" -> voted,
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
        "coordinate" -> routes.Coordinate.home.url,
        "editor" -> routes.Editor.load("").url
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
        trans.giveUp,
        trans.victory,
        trans.puzzleSolvedInXSeconds,
        trans.fromGameLink,
        trans.boardEditor,
        trans.continueFromHere,
        trans.playWithTheMachine,
        trans.playWithAFriend,
        trans.wasThisPuzzleAnyGood,
        trans.pleaseVotePuzzle,
        trans.thankYou,
        trans.puzzleId,
        trans.ratingX,
        trans.playedXTimes,
        trans.startTraining,
        trans.continueTraining,
        trans.retryThisPuzzle)
    )))
}
