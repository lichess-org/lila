package views.html.board

import lila.api.Context
import lila.app.templating.Environment._
import lila.common.Lang
import lila.i18n.{ I18nKeys => trans }

object userAnalysisI18n {

  def apply(
    withCeval: Boolean = true,
    withExplorer: Boolean = true,
    withForecast: Boolean = false,
    withAdvantageChart: Boolean = false
  )(implicit lang: Lang) = i18nJsObject(
    baseTranslations ++ {
      withCeval ?? cevalTranslations
    } ++ {
      withExplorer ?? explorerTranslations
    } ++ {
      withForecast ?? forecastTranslations
    } ++ {
      withAdvantageChart ?? advantageChartTranslations
    }
  )

  private val baseTranslations = Vector(
    trans.analysis,
    trans.flipBoard,
    trans.backToGame,
    trans.gameAborted,
    trans.checkmate,
    trans.whiteResigned,
    trans.blackResigned,
    trans.stalemate,
    trans.whiteLeftTheGame,
    trans.blackLeftTheGame,
    trans.draw,
    trans.timeOut,
    trans.playingRightNow,
    trans.whiteIsVictorious,
    trans.blackIsVictorious,
    trans.kingInTheCenter,
    trans.threeChecks,
    trans.variantEnding,
    trans.whitePlays,
    trans.blackPlays,
    trans.gameOver,
    trans.importPgn,
    trans.requestAComputerAnalysis,
    trans.computerAnalysis,
    trans.learnFromYourMistakes,
    trans.averageCentipawnLoss,
    trans.inaccuracies,
    trans.mistakes,
    trans.blunders,
    trans.goodMove,
    trans.viewTheSolution,
    // action menu
    trans.menu,
    trans.boardEditor,
    trans.continueFromHere,
    trans.playWithTheMachine,
    trans.playWithAFriend,
    trans.openStudy,
    trans.preferences,
    trans.inlineNotation,
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
    trans.goodMove,
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
    // gamebook
    trans.findTheBestMoveForWhite,
    trans.findTheBestMoveForBlack
  )

  private val cevalTranslations = Vector(
    // also uses gameOver
    trans.depthX,
    trans.usingServerAnalysis,
    trans.loadingEngine,
    trans.cloudAnalysis,
    trans.goDeeper,
    trans.showThreat,
    trans.inLocalBrowser,
    trans.toggleLocalEvaluation,
    // ceval menu
    trans.computerAnalysis,
    trans.enable,
    trans.bestMoveArrow,
    trans.evaluationGauge,
    trans.infiniteAnalysis,
    trans.removesTheDepthLimit,
    trans.multipleLines,
    trans.cpus,
    trans.memory
  )

  private val explorerTranslations = Vector(
    // also uses gameOver, checkmate, stalemate, draw, variantEnding
    trans.openingExplorerAndTablebase,
    trans.openingExplorer,
    trans.xOpeningExplorer,
    trans.move,
    trans.games,
    trans.variantLoss,
    trans.variantWin,
    trans.insufficientMaterial,
    trans.capture,
    trans.pawnMove,
    trans.close,
    trans.winning,
    trans.unknown,
    trans.losing,
    trans.drawn,
    trans.timeControl,
    trans.averageElo,
    trans.database,
    trans.recentGames,
    trans.topGames,
    trans.whiteDrawBlack,
    trans.averageRatingX,
    trans.masterDbExplanation,
    trans.mateInXHalfMoves,
    trans.nextCaptureOrPawnMoveInXHalfMoves,
    trans.noGameFound,
    trans.maybeIncludeMoreGamesFromThePreferencesMenu,
    trans.winPreventedBy50MoveRule,
    trans.lossSavedBy50MoveRule,
    trans.allSet
  )

  private val forecastTranslations = Vector(
    trans.conditionalPremoves,
    trans.addCurrentVariation,
    trans.playVariationToCreateConditionalPremoves,
    trans.noConditionalPremoves,
    trans.playX,
    trans.andSaveNbPremoveLines
  )

  private val advantageChartTranslations = Vector(
    trans.advantage,
    trans.opening,
    trans.middlegame,
    trans.endgame
  )
}
