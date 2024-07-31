package lila.analyse
package ui

import lila.core.i18n.{ I18nKey, Translate }
import lila.ui.*

final class AnalyseI18n(helpers: Helpers):
  import helpers.*

  def apply(
      withCeval: Boolean = true,
      withExplorer: Boolean = true,
      withForecast: Boolean = false,
      withAdvantageChart: Boolean = false
  )(using Translate) =
    i18nJsObject(vector(withCeval, withExplorer, withForecast, withAdvantageChart))

  def vector(
      withCeval: Boolean = true,
      withExplorer: Boolean = true,
      withForecast: Boolean = false,
      withAdvantageChart: Boolean = false
  ): Vector[I18nKey] =
    baseTranslations ++ {
      withCeval.so(cevalTranslations)
    } ++ {
      withExplorer.so(explorerTranslations)
    } ++ {
      withForecast.so(forecastTranslations)
    } ++ {
      withAdvantageChart.so(advantageTranslations)
    }

  import lila.core.i18n.I18nKey.{ site, puzzle, study, preferences }

  private val baseTranslations = Vector(
    site.analysis,
    site.flipBoard,
    site.backToGame,
    site.gameAborted,
    site.checkmate,
    site.whiteResigned,
    site.blackResigned,
    site.whiteDidntMove,
    site.blackDidntMove,
    site.stalemate,
    site.whiteLeftTheGame,
    site.blackLeftTheGame,
    site.draw,
    site.whiteTimeOut,
    site.blackTimeOut,
    site.playingRightNow,
    site.whiteIsVictorious,
    site.blackIsVictorious,
    site.cheatDetected,
    site.kingInTheCenter,
    site.threeChecks,
    site.variantEnding,
    site.drawByMutualAgreement,
    site.fiftyMovesWithoutProgress,
    site.insufficientMaterial,
    site.whitePlays,
    site.blackPlays,
    site.gameOver,
    site.importPgn,
    site.requestAComputerAnalysis,
    site.computerAnalysis,
    site.learnFromYourMistakes,
    site.averageCentipawnLoss,
    site.accuracy,
    site.viewTheSolution,
    // action menu
    site.menu,
    site.boardEditor,
    site.continueFromHere,
    site.toStudy,
    site.playWithTheMachine,
    site.playWithAFriend,
    site.openStudy,
    preferences.preferences,
    site.inlineNotation,
    site.makeAStudy,
    site.clearSavedMoves,
    site.replayMode,
    site.slow,
    site.fast,
    site.realtimeReplay,
    site.byCPL,
    // context menu
    site.promoteVariation,
    site.makeMainLine,
    site.deleteFromHere,
    site.collapseVariations,
    site.expandVariations,
    site.forceVariation,
    site.copyVariationPgn,
    // practice (also uses checkmate, draw)
    site.practiceWithComputer,
    puzzle.goodMove,
    site.inaccuracy,
    site.mistake,
    site.blunder,
    site.threefoldRepetition,
    site.anotherWasX,
    site.bestWasX,
    site.youBrowsedAway,
    site.resumePractice,
    site.whiteWinsGame,
    site.blackWinsGame,
    site.theGameIsADraw,
    site.yourTurn,
    site.computerThinking,
    site.seeBestMove,
    site.hideBestMove,
    site.getAHint,
    site.evaluatingYourMove,
    // gamebook
    puzzle.findTheBestMoveForWhite,
    puzzle.findTheBestMoveForBlack
  )

  val cevalWidget = Vector(
    site.gameOver,
    site.depthX,
    site.usingServerAnalysis,
    site.loadingEngine,
    site.calculatingMoves,
    site.engineFailed,
    site.cloudAnalysis,
    site.goDeeper,
    site.showThreat,
    site.inLocalBrowser,
    site.toggleLocalEvaluation,
    site.computerAnalysisDisabled
  )

  val cevalTranslations: Vector[I18nKey] = cevalWidget ++ Vector(
    // ceval menu
    site.computerAnalysis,
    site.enable,
    site.bestMoveArrow,
    site.showVariationArrows,
    site.evaluationGauge,
    site.infiniteAnalysis,
    site.removesTheDepthLimit,
    site.multipleLines,
    site.cpus,
    site.memory,
    site.engineManager
  )

  val explorerTranslations = Vector(
    // also uses gameOver, checkmate, stalemate, draw, variantEnding
    site.openingExplorerAndTablebase,
    site.openingExplorer,
    site.xOpeningExplorer,
    site.move,
    site.games,
    site.variantLoss,
    site.variantWin,
    site.insufficientMaterial,
    site.capture,
    site.pawnMove,
    site.close,
    site.winning,
    site.unknown,
    site.losing,
    site.drawn,
    site.timeControl,
    site.averageElo,
    site.database,
    site.recentGames,
    site.topGames,
    site.whiteDrawBlack,
    site.averageRatingX,
    site.masterDbExplanation,
    site.mateInXHalfMoves,
    site.dtzWithRounding,
    site.winOr50MovesByPriorMistake,
    site.lossOr50MovesByPriorMistake,
    site.unknownDueToRounding,
    site.noGameFound,
    site.maxDepthReached,
    site.maybeIncludeMoreGamesFromThePreferencesMenu,
    site.winPreventedBy50MoveRule,
    site.lossSavedBy50MoveRule,
    site.allSet,
    study.searchByUsername,
    site.mode,
    site.rated,
    site.casual,
    site.since,
    site.until,
    site.switchSides,
    site.lichessDbExplanation,
    site.player,
    site.asWhite,
    site.asBlack
  )

  private val forecastTranslations = Vector(
    site.conditionalPremoves,
    site.addCurrentVariation,
    site.playVariationToCreateConditionalPremoves,
    site.noConditionalPremoves,
    site.playX,
    site.andSaveNbPremoveLines
  )

  val advantageChartTranslations = Vector(
    site.advantage,
    site.nbSeconds,
    site.opening,
    site.middlegame,
    site.endgame
  )

  private val advantageTranslations =
    advantageChartTranslations ++
      Vector(
        site.nbInaccuracies,
        site.nbMistakes,
        site.nbBlunders
      )

final class GameAnalyseI18n(helpers: Helpers, board: AnalyseI18n):
  import helpers.*

  def apply()(using Translate) = i18nJsObject(i18nKeys)

  import lila.core.i18n.I18nKey.{ site, puzzle }

  private val i18nKeys: Vector[I18nKey] = {
    board.cevalWidget ++
      board.advantageChartTranslations ++
      board.explorerTranslations ++
      Vector(
        site.flipBoard,
        site.gameAborted,
        site.gameOver,
        site.checkmate,
        site.whiteResigned,
        site.blackResigned,
        site.whiteDidntMove,
        site.blackDidntMove,
        site.stalemate,
        site.whiteLeftTheGame,
        site.blackLeftTheGame,
        site.draw,
        site.whiteTimeOut,
        site.blackTimeOut,
        site.playingRightNow,
        site.whiteIsVictorious,
        site.blackIsVictorious,
        site.cheatDetected,
        site.kingInTheCenter,
        site.threeChecks,
        site.variantEnding,
        site.drawByMutualAgreement,
        site.fiftyMovesWithoutProgress,
        site.insufficientMaterial,
        site.analysis,
        site.boardEditor,
        site.continueFromHere,
        site.playWithTheMachine,
        site.playWithAFriend,
        site.openingExplorer,
        site.nbInaccuracies,
        site.nbMistakes,
        site.nbBlunders,
        site.averageCentipawnLoss,
        site.accuracy,
        site.viewTheSolution,
        site.youNeedAnAccountToDoThat,
        site.aiNameLevelAiLevel,
        // action menu
        site.menu,
        site.toStudy,
        site.inlineNotation,
        site.makeAStudy,
        site.clearSavedMoves,
        site.computerAnalysis,
        site.enable,
        site.bestMoveArrow,
        site.showVariationArrows,
        site.evaluationGauge,
        site.infiniteAnalysis,
        site.removesTheDepthLimit,
        site.multipleLines,
        site.cpus,
        site.memory,
        site.engineManager,
        site.replayMode,
        site.slow,
        site.fast,
        site.realtimeReplay,
        site.byCPL,
        // context menu
        site.promoteVariation,
        site.makeMainLine,
        site.deleteFromHere,
        site.collapseVariations,
        site.expandVariations,
        site.forceVariation,
        site.copyVariationPgn,
        // practice (also uses checkmate, draw)
        site.practiceWithComputer,
        puzzle.goodMove,
        site.inaccuracy,
        site.mistake,
        site.blunder,
        site.threefoldRepetition,
        site.anotherWasX,
        site.bestWasX,
        site.youBrowsedAway,
        site.resumePractice,
        site.whiteWinsGame,
        site.blackWinsGame,
        site.drawByFiftyMoves,
        site.theGameIsADraw,
        site.yourTurn,
        site.computerThinking,
        site.seeBestMove,
        site.hideBestMove,
        site.getAHint,
        site.evaluatingYourMove,
        // retrospect (also uses youBrowsedAway, bestWasX, evaluatingYourMove)
        site.learnFromYourMistakes,
        site.learnFromThisMistake,
        site.skipThisMove,
        site.next,
        site.xWasPlayed,
        site.findBetterMoveForWhite,
        site.findBetterMoveForBlack,
        site.resumeLearning,
        site.youCanDoBetter,
        site.tryAnotherMoveForWhite,
        site.tryAnotherMoveForBlack,
        site.solution,
        site.waitingForAnalysis,
        site.noMistakesFoundForWhite,
        site.noMistakesFoundForBlack,
        site.doneReviewingWhiteMistakes,
        site.doneReviewingBlackMistakes,
        site.doItAgain,
        site.reviewWhiteMistakes,
        site.reviewBlackMistakes
      )
  }.distinct
