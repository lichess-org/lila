package lila.analyse
package actorApi

import lila.game.Game

case class AnalysisReady(game: Game, analysis: Analysis)

case class AnalysisProgress(
  ratio: Float,
  pgnMoves: List[String],
  variant: chess.variant.Variant,
  initialFen: chess.format.FEN,
  analysis: Analysis)
