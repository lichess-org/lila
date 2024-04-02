package lila.analyse
package actorApi

import lila.game.Game
import lila.tree.Analysis

case class AnalysisReady(game: Game, analysis: Analysis)

case class AnalysisProgress(
    game: Game,
    variant: chess.variant.Variant,
    initialFen: chess.format.Fen.Full,
    analysis: Analysis
)

case class StudyAnalysisProgress(analysis: Analysis, complete: Boolean)
