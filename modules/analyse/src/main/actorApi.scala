package lidraughts.analyse
package actorApi

import lidraughts.game.Game

case class AnalysisReady(game: Game, analysis: Analysis)

case class AnalysisProgress(
    game: Game,
    variant: draughts.variant.Variant,
    initialFen: draughts.format.FEN,
    analysis: Analysis
)

case class StudyAnalysisProgress(analysis: Analysis, complete: Boolean)
