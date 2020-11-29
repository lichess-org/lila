package views
package html.puzzle

import play.api.i18n.Lang

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.i18n.MessageKey
import lila.puzzle.PuzzleTheme

object bits {

  private val dataLastmove = attr("data-lastmove")

  def daily(p: lila.puzzle.Puzzle, fen: chess.format.FEN, lastMove: String) =
    views.html.board.bits.mini(fen, p.color, lastMove)(span)

  def jsI18n()(implicit lang: Lang) = i18nJsObject(i18nKeys)

  private val i18nKeys: List[MessageKey] = {
    PuzzleTheme.allTranslationKeys ::: List(
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
    )
  }.map(_.key)
}
