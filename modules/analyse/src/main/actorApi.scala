package lila.analyse
package actorApi

import lila.game.Game

case class AnalysisReady(game: Game, analysis: Analysis)

case class AnalysisProgress(
    game: Game,
    variant: shogi.variant.Variant,
    initialSfen: shogi.format.forsyth.Sfen,
    analysis: Analysis
)

case class StudyAnalysisProgress(analysis: Analysis, complete: Boolean)
