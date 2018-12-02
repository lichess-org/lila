package views
package html.puzzle

import play.twirl.api.Html
import scalatags.Text.all._

import lila.api.Context
import lila.app.templating.Environment._
import lila.i18n.{ I18nKeys => trans }

import controllers.routes

object bits {

  def daily(p: lila.puzzle.Puzzle, fen: String, lastMove: String) = a(
    href := routes.Puzzle.daily(),
    cls := "mini_board parse_fen is2d",
    attr("data-color") := p.color.name,
    attr("data-fen") := fen,
    attr("data-lastmove") := lastMove
  )(miniBoardContent)

  def jsI18n(implicit context: Context) = toJson(i18nJsObject(
    trans.training,
    trans.yourPuzzleRatingX,
    trans.goodMove,
    trans.butYouCanDoBetter,
    trans.bestMove,
    trans.keepGoing,
    trans.puzzleFailed,
    trans.butYouCanKeepTrying,
    trans.yourTurn,
    trans.findTheBestMoveForBlack,
    trans.findTheBestMoveForWhite,
    trans.viewTheSolution,
    trans.success,
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
    trans.continueTraining,
    trans.retryThisPuzzle,
    trans.toTrackYourProgress,
    trans.signUp,
    trans.trainingSignupExplanation,
    trans.thisPuzzleIsCorrect,
    trans.thisPuzzleIsWrong,
    trans.puzzles,
    trans.analysis,
    trans.rated,
    trans.casual,
    // ceval
    trans.depthX,
    trans.usingServerAnalysis,
    trans.loadingEngine,
    trans.cloudAnalysis,
    trans.goDeeper,
    trans.showThreat,
    trans.gameOver,
    trans.inLocalBrowser,
    trans.toggleLocalEvaluation
  ))
}
