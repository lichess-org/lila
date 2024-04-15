package lila.analyse
package actorApi

import lila.tree.Analysis

case class AnalysisReady(game: Game, analysis: Analysis)

case class StudyAnalysisProgress(analysis: Analysis, complete: Boolean)
