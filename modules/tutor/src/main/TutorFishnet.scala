package lila.tutor

import com.softwaremill.tagging.*

import lila.core.fishnet.{ AnalysisAwaiter, FishnetRequest }
import lila.game.GameRepo
import lila.insight.InsightPerfStats
import lila.memo.SettingStore
import lila.rating.PerfType

final private class TutorFishnet(
    gameRepo: GameRepo,
    analyser: FishnetRequest,
    awaiter: AnalysisAwaiter,
    nbAnalysis: SettingStore[Int] @@ NbAnalysis
)(using Executor):

  def maxToAnalyse = Max(nbAnalysis.get())
  def maxGamesToConsider = Max(maxToAnalyse.value * 2)
  val maxTime = 5.minutes

  def ensureSomeAnalysis(stats: Map[PerfType, InsightPerfStats.WithGameIds]): Funit =
    val totalNbGames = stats.values.map(_.stats.totalNbGames).sum
    stats.values.toList
      .sequentially: s =>
        val ids = s.gameIds.take(s.stats.totalNbGames * maxGamesToConsider.value / totalNbGames)
        gameRepo.unanalysedGames(ids, Max(s.stats.totalNbGames * maxToAnalyse.value / totalNbGames))
      .map(_.flatten)
      .flatMap: games =>
        games.foreach(g => analyser.tutor(g.id))
        awaiter(games.map(_.id), maxTime).map(lila.mon.tutor.fishnetMissing.record(_))
