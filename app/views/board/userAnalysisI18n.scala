package views.html.board

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.common.Lang
import lidraughts.i18n.{ I18nKeys => trans }

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
    trans.backToGame,
    trans.whitePlays,
    trans.blackPlays,
    trans.gameOver,
    trans.importPdn,
    trans.requestAComputerAnalysis,
    trans.computerAnalysis,
    trans.learnFromYourMistakes,
    trans.analysis,
    trans.flipBoard,
    trans.gameAborted,
    trans.whiteResigned,
    trans.blackResigned,
    trans.whiteLeftTheGame,
    trans.blackLeftTheGame,
    trans.draw,
    trans.timeOut,
    trans.playingRightNow,
    trans.whiteIsVictorious,
    trans.blackIsVictorious,
    trans.promotion,
    trans.variantEnding,
    trans.inaccuracies,
    trans.mistakes,
    trans.blunders,
    trans.averageCentipieceLoss,
    trans.goodMove,
    trans.viewTheSolution,
    trans.speedUpYourAnalysis,
    trans.enableFullCaptureAtXPreferences,
    trans.gameBehavior,
    trans.edit,
    trans.delete,
    trans.spectators,
    // action menu
    trans.menu,
    trans.boardEditor,
    trans.continueFromHere,
    trans.playWithTheMachine,
    trans.playWithAFriend,
    trans.studyMenu,
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
    // interactive lessons (also uses yourTurn, goodMove, getAHint, viewTheSolution)
    trans.helpPlayerInitialMove,
    trans.introduceLesson,
    trans.putOpponentsFirstMove,
    trans.explainOpponentMove,
    trans.reflectOnCorrectMove,
    trans.addVariationsWrongMoves,
    trans.explainWhyThisMoveIsWrong,
    trans.orPromoteItToMainline,
    trans.anyOtherWrongMove,
    trans.explainWhyOtherMovesWrong,
    trans.whatWouldYouPlay,
    trans.optionalHint,
    trans.giveThePlayerAHint,
    trans.lessonCompleted,
    trans.nextChapter,
    trans.playAgain,
    trans.analyse,
    trans.preview,
    trans.retry,
    trans.next,
    trans.back,
    // practice (also uses draw)
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
    // also uses gameOver, stalemate, draw, variantEnding
    trans.openingExplorerAndTablebase,
    trans.openingExplorer,
    trans.xOpeningExplorer,
    trans.move,
    trans.games,
    trans.variantLoss,
    trans.variantWin,
    trans.insufficientMaterial,
    trans.capture,
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
    trans.winInXHalfMoves,
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
    trans.nbSeconds,
    trans.opening,
    trans.middlegame,
    trans.endgame
  )
}
