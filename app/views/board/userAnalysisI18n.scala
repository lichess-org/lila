package views.html.board

import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.i18n.{ I18nKeys as trans }

object userAnalysisI18n:

  def apply(
      withCeval: Boolean = true,
      withExplorer: Boolean = true,
      withForecast: Boolean = false,
      withAdvantageChart: Boolean = false
  )(using Lang) =
    i18nJsObject(
      baseTranslations ++ {
        withCeval so cevalTranslations
      } ++ {
        withExplorer so explorerTranslations
      } ++ {
        withForecast so forecastTranslations
      } ++ {
        withAdvantageChart so advantageTranslations
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
    trans.whiteDidntMove,
    trans.blackDidntMove,
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
    trans.whitePlays,
    trans.blackPlays,
    trans.gameOver,
    trans.importPgn,
    trans.requestAComputerAnalysis,
    trans.computerAnalysis,
    trans.learnFromYourMistakes,
    trans.averageCentipawnLoss,
    trans.accuracy,
    trans.viewTheSolution,
    // action menu
    trans.menu,
    trans.boardEditor,
    trans.continueFromHere,
    trans.toStudy,
    trans.playWithTheMachine,
    trans.playWithAFriend,
    trans.openStudy,
    trans.preferences.preferences,
    trans.inlineNotation,
    trans.makeAStudy,
    trans.clearSavedMoves,
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
    trans.copyVariationPgn,
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
    // gamebook
    trans.puzzle.findTheBestMoveForWhite,
    trans.puzzle.findTheBestMoveForBlack
  )

  val cevalWidget = Vector(
    // also uses gameOver
    trans.depthX,
    trans.usingServerAnalysis,
    trans.loadingEngine,
    trans.calculatingMoves,
    trans.engineFailed,
    trans.cloudAnalysis,
    trans.goDeeper,
    trans.showThreat,
    trans.inLocalBrowser,
    trans.toggleLocalEvaluation,
    trans.computerAnalysisDisabled
  )

  private val cevalTranslations = cevalWidget ++ Vector(
    // ceval menu
    trans.computerAnalysis,
    trans.enable,
    trans.bestMoveArrow,
    trans.showVariationArrows,
    trans.evaluationGauge,
    trans.infiniteAnalysis,
    trans.removesTheDepthLimit,
    trans.multipleLines,
    trans.cpus,
    trans.memory,
    trans.engineManager
  )

  val explorerTranslations = Vector(
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
    trans.dtzWithRounding,
    trans.winOr50MovesByPriorMistake,
    trans.lossOr50MovesByPriorMistake,
    trans.unknownDueToRounding,
    trans.noGameFound,
    trans.maxDepthReached,
    trans.maybeIncludeMoreGamesFromThePreferencesMenu,
    trans.winPreventedBy50MoveRule,
    trans.lossSavedBy50MoveRule,
    trans.allSet,
    trans.study.searchByUsername,
    trans.mode,
    trans.rated,
    trans.casual,
    trans.since,
    trans.until,
    trans.switchSides,
    trans.lichessDbExplanation,
    trans.player,
    trans.asWhite,
    trans.asBlack
  )

  private val forecastTranslations = Vector(
    trans.conditionalPremoves,
    trans.addCurrentVariation,
    trans.playVariationToCreateConditionalPremoves,
    trans.noConditionalPremoves,
    trans.playX,
    trans.andSaveNbPremoveLines
  )

  val advantageChartTranslations = Vector(
    trans.advantage,
    trans.nbSeconds,
    trans.opening,
    trans.middlegame,
    trans.endgame
  )

  private val advantageTranslations =
    advantageChartTranslations ++
      Vector(
        trans.nbInaccuracies,
        trans.nbMistakes,
        trans.nbBlunders
      )
