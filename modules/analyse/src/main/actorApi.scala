package lila.analyse
package actorApi

import lila.game.Game

case class AnalysisReady(game: Game, analysis: Analysis)

case class AnalysisProgress(
    game: Game,
    variant: shogi.variant.Variant,
    analysis: Analysis
)

case class StudyAnalysisProgress(analysis: Analysis, complete: Boolean)
case class PostGameStudyAnalysisProgress(analysis: Analysis, complete: Boolean)
