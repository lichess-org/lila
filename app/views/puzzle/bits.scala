package views
package html.puzzle

import play.twirl.api.Html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object bits {

  private val dataColor = attr("data-color")
  private val dataFen = attr("data-fen")
  private val dataLastmove = attr("data-lastmove")

  def daily(p: lila.puzzle.Puzzle, fen: String, lastMove: String) = a(
    href := routes.Puzzle.daily(),
    cls := "mini_board parse_fen is2d",
    dataColor := p.color.name,
    dataFen := fen,
    dataLastmove := lastMove
  )(miniBoardContent)

  def jsI18n(implicit ctx: Context) = toJson(i18nJsObject(translations))

  private val translations = List(
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
  )
}
