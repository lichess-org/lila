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

  private val i18nKeys: List[MessageKey] = List(
    trans.puzzle.yourPuzzleRatingX,
    trans.puzzle.bestMove,
    trans.puzzle.keepGoing,
    trans.puzzle.notTheMove,
    trans.puzzle.trySomethingElse,
    trans.yourTurn,
    trans.puzzle.findTheBestMoveForBlack,
    trans.puzzle.findTheBestMoveForWhite,
    trans.viewTheSolution,
    trans.puzzle.puzzleSuccess,
    trans.puzzle.puzzleComplete,
    trans.puzzle.fromGameLink,
    trans.boardEditor,
    trans.continueFromHere,
    trans.playWithTheMachine,
    trans.playWithAFriend,
    trans.puzzle.didYouLikeThisPuzzle,
    trans.puzzle.voteToLoadNextOne,
    trans.puzzle.puzzleId,
    trans.puzzle.ratingX,
    trans.puzzle.playedXTimes,
    trans.puzzle.continueTraining,
    trans.puzzle.toTrackYourProgress,
    trans.signUp,
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
