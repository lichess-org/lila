package views.html.analyse

import play.api.i18n.Lang

import lila.app.templating.Environment._
import lila.i18n.{ MessageKey, I18nKeys => trans }
import views.html.board.{ userAnalysisI18n => board }

object jsI18n {

  def apply()(implicit lang: Lang) = i18nJsObject(i18nKeys)

  private val i18nKeys: Vector[MessageKey] = {
    board.cevalWidget ++
      board.advantageChartTranslations ++
      board.explorerTranslations ++
      Vector(
        trans.flipBoard,
        trans.gameAborted,
        trans.checkmate,
        trans.whiteResigned,
        trans.blackResigned,
        trans.stalemate,
        trans.whiteLeftTheGame,
        trans.blackLeftTheGame,
        trans.draw,
        trans.whiteTimeOut,
        trans.blackTimeOut,
        trans.playingRightNow,
        trans.whiteIsVictorious,
        trans.blackIsVictorious,
        trans.cheatDetected,
        trans.kingInTheCenter,
        trans.threeChecks,
        trans.variantEnding,
        trans.drawByMutualAgreement,
        trans.fiftyMovesWithoutProgress,
        trans.insufficientMaterial,
        trans.analysis,
        trans.boardEditor,
        trans.continueFromHere,
        trans.playWithTheMachine,
        trans.playWithAFriend,
        trans.openingExplorer,
        trans.nbInaccuracies,
        trans.nbMistakes,
        trans.nbBlunders,
        trans.averageCentipawnLoss,
        trans.viewTheSolution,
        trans.youNeedAnAccountToDoThat,
        // action menu
        trans.menu,
        trans.toStudy,
        trans.inlineNotation,
        trans.computerAnalysis,
        trans.enable,
        trans.bestMoveArrow,
        trans.evaluationGauge,
        trans.infiniteAnalysis,
        trans.removesTheDepthLimit,
        trans.multipleLines,
        trans.cpus,
        trans.memory,
        trans.delete,
        trans.deleteThisImportedGame,
        trans.replayMode,
        trans.slow,
        trans.fast,
        trans.realtimeReplay,
        trans.byCPL,
        // context menu
        trans.promoteVariation,
        trans.makeMainLine,
        trans.deleteFromHere,
        trans.forceVariation,
        // practice (also uses checkmate, draw)
        trans.practiceWithComputer,
        trans.puzzle.goodMove,
        trans.inaccuracy,
        trans.mistake,
        trans.blunder,
        trans.threefoldRepetition,
        trans.anotherWasX,
        trans.bestWasX,
        trans.youBrowsedAway,
        trans.resumePractice,
        trans.whiteWinsGame,
        trans.blackWinsGame,
        trans.theGameIsADraw,
        trans.yourTurn,
        trans.computerThinking,
        trans.seeBestMove,
        trans.hideBestMove,
        trans.getAHint,
        trans.evaluatingYourMove,
        // retrospect (also uses youBrowsedAway, bestWasX, evaluatingYourMove)
        trans.learnFromYourMistakes,
        trans.learnFromThisMistake,
        trans.skipThisMove,
        trans.next,
        trans.xWasPlayed,
        trans.findBetterMoveForWhite,
        trans.findBetterMoveForBlack,
        trans.resumeLearning,
        trans.youCanDoBetter,
        trans.tryAnotherMoveForWhite,
        trans.tryAnotherMoveForBlack,
        trans.solution,
        trans.waitingForAnalysis,
        trans.noMistakesFoundForWhite,
        trans.noMistakesFoundForBlack,
        trans.doneReviewingWhiteMistakes,
        trans.doneReviewingBlackMistakes,
        trans.doItAgain,
        trans.reviewWhiteMistakes,
        trans.reviewBlackMistakes
      ).map(_.key)
  }.distinct
}
