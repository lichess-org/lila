package views.html.board

import lila.app.templating.Environment.{ *, given }
import lila.core.i18n.I18nKey

object userAnalysisI18n:

  def apply(
      withCeval: Boolean = true,
      withExplorer: Boolean = true,
      withForecast: Boolean = false,
      withAdvantageChart: Boolean = false
  )(using Translate) =
    i18nJsObject(
      baseTranslations ++ {
        withCeval.so(cevalTranslations)
      } ++ {
        withExplorer.so(explorerTranslations)
      } ++ {
        withForecast.so(forecastTranslations)
      } ++ {
        withAdvantageChart.so(advantageTranslations)
      }
    )

  private val baseTranslations = Vector(
    trans.site.analysis,
    trans.site.flipBoard,
    trans.site.backToGame,
    trans.site.gameAborted,
    trans.site.checkmate,
    trans.site.whiteResigned,
    trans.site.blackResigned,
    trans.site.whiteDidntMove,
    trans.site.blackDidntMove,
    trans.site.stalemate,
    trans.site.whiteLeftTheGame,
    trans.site.blackLeftTheGame,
    trans.site.draw,
    trans.site.whiteTimeOut,
    trans.site.blackTimeOut,
    trans.site.playingRightNow,
    trans.site.whiteIsVictorious,
    trans.site.blackIsVictorious,
    trans.site.cheatDetected,
    trans.site.kingInTheCenter,
    trans.site.threeChecks,
    trans.site.variantEnding,
    trans.site.drawByMutualAgreement,
    trans.site.fiftyMovesWithoutProgress,
    trans.site.insufficientMaterial,
    trans.site.whitePlays,
    trans.site.blackPlays,
    trans.site.gameOver,
    trans.site.importPgn,
    trans.site.requestAComputerAnalysis,
    trans.site.computerAnalysis,
    trans.site.learnFromYourMistakes,
    trans.site.averageCentipawnLoss,
    trans.site.accuracy,
    trans.site.viewTheSolution,
    // action menu
    trans.site.menu,
    trans.site.boardEditor,
    trans.site.continueFromHere,
    trans.site.toStudy,
    trans.site.playWithTheMachine,
    trans.site.playWithAFriend,
    trans.site.openStudy,
    trans.preferences.preferences,
    trans.site.inlineNotation,
    trans.site.makeAStudy,
    trans.site.clearSavedMoves,
    trans.site.replayMode,
    trans.site.slow,
    trans.site.fast,
    trans.site.realtimeReplay,
    trans.site.byCPL,
    // context menu
    trans.site.promoteVariation,
    trans.site.makeMainLine,
    trans.site.deleteFromHere,
    trans.site.forceVariation,
    trans.site.copyVariationPgn,
    // practice (also uses checkmate, draw)
    trans.site.practiceWithComputer,
    trans.puzzle.goodMove,
    trans.site.inaccuracy,
    trans.site.mistake,
    trans.site.blunder,
    trans.site.threefoldRepetition,
    trans.site.anotherWasX,
    trans.site.bestWasX,
    trans.site.youBrowsedAway,
    trans.site.resumePractice,
    trans.site.whiteWinsGame,
    trans.site.blackWinsGame,
    trans.site.theGameIsADraw,
    trans.site.yourTurn,
    trans.site.computerThinking,
    trans.site.seeBestMove,
    trans.site.hideBestMove,
    trans.site.getAHint,
    trans.site.evaluatingYourMove,
    // gamebook
    trans.puzzle.findTheBestMoveForWhite,
    trans.puzzle.findTheBestMoveForBlack
  )

  val cevalWidget = Vector(
    // also uses gameOver
    trans.site.depthX,
    trans.site.usingServerAnalysis,
    trans.site.loadingEngine,
    trans.site.calculatingMoves,
    trans.site.engineFailed,
    trans.site.cloudAnalysis,
    trans.site.goDeeper,
    trans.site.showThreat,
    trans.site.inLocalBrowser,
    trans.site.toggleLocalEvaluation,
    trans.site.computerAnalysisDisabled
  )

  val cevalTranslations: Vector[I18nKey] = cevalWidget ++ Vector(
    // ceval menu
    trans.site.computerAnalysis,
    trans.site.enable,
    trans.site.bestMoveArrow,
    trans.site.showVariationArrows,
    trans.site.evaluationGauge,
    trans.site.infiniteAnalysis,
    trans.site.removesTheDepthLimit,
    trans.site.multipleLines,
    trans.site.cpus,
    trans.site.memory,
    trans.site.engineManager
  )

  val explorerTranslations = Vector(
    // also uses gameOver, checkmate, stalemate, draw, variantEnding
    trans.site.openingExplorerAndTablebase,
    trans.site.openingExplorer,
    trans.site.xOpeningExplorer,
    trans.site.move,
    trans.site.games,
    trans.site.variantLoss,
    trans.site.variantWin,
    trans.site.insufficientMaterial,
    trans.site.capture,
    trans.site.pawnMove,
    trans.site.close,
    trans.site.winning,
    trans.site.unknown,
    trans.site.losing,
    trans.site.drawn,
    trans.site.timeControl,
    trans.site.averageElo,
    trans.site.database,
    trans.site.recentGames,
    trans.site.topGames,
    trans.site.whiteDrawBlack,
    trans.site.averageRatingX,
    trans.site.masterDbExplanation,
    trans.site.mateInXHalfMoves,
    trans.site.dtzWithRounding,
    trans.site.winOr50MovesByPriorMistake,
    trans.site.lossOr50MovesByPriorMistake,
    trans.site.unknownDueToRounding,
    trans.site.noGameFound,
    trans.site.maxDepthReached,
    trans.site.maybeIncludeMoreGamesFromThePreferencesMenu,
    trans.site.winPreventedBy50MoveRule,
    trans.site.lossSavedBy50MoveRule,
    trans.site.allSet,
    trans.study.searchByUsername,
    trans.site.mode,
    trans.site.rated,
    trans.site.casual,
    trans.site.since,
    trans.site.until,
    trans.site.switchSides,
    trans.site.lichessDbExplanation,
    trans.site.player,
    trans.site.asWhite,
    trans.site.asBlack
  )

  private val forecastTranslations = Vector(
    trans.site.conditionalPremoves,
    trans.site.addCurrentVariation,
    trans.site.playVariationToCreateConditionalPremoves,
    trans.site.noConditionalPremoves,
    trans.site.playX,
    trans.site.andSaveNbPremoveLines
  )

  val advantageChartTranslations = Vector(
    trans.site.advantage,
    trans.site.nbSeconds,
    trans.site.opening,
    trans.site.middlegame,
    trans.site.endgame
  )

  private val advantageTranslations =
    advantageChartTranslations ++
      Vector(
        trans.site.nbInaccuracies,
        trans.site.nbMistakes,
        trans.site.nbBlunders
      )
