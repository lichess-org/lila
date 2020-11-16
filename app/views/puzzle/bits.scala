package views
package html.puzzle

import play.api.i18n.Lang

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object bits {

  private val dataLastmove = attr("data-lastmove")

  def daily(p: lila.puzzle.Puzzle, fen: String, lastMove: String) =
    a(
      href := routes.Page.notSupported(), // puzzle daily
      cls := "mini-board cg-wrap parse-fen is2d",
      dataColor := p.color.name,
      dataFen := fen,
      dataLastmove := lastMove
    )(cgWrapContent)

  def jsI18n()(implicit lang: Lang) = i18nJsObject(i18nKeys)

  private val i18nKeys = List(
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
    trans.thisPuzzleIsCorrect,
    trans.thisPuzzleIsWrong,
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
  ).map(_.key)
}
