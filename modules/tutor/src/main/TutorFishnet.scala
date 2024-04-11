package lila.tutor

import com.softwaremill.tagging.*

import lila.core.fishnet.{ SystemAnalysisRequest, AnalysisAwaiter }
import lila.game.GameRepo
import lila.insight.InsightPerfStats
import lila.memo.SettingStore
import lila.rating.PerfType
import lila.user.User

final private class TutorFishnet(
    gameRepo: GameRepo,
    analyser: SystemAnalysisRequest,
    awaiter: AnalysisAwaiter,
    nbAnalysis: SettingStore[Int] @@ NbAnalysis
)(using Executor):

  def maxToAnalyse       = Max(nbAnalysis.get())
  def maxGamesToConsider = Max(maxToAnalyse.value * 2)
  val maxTime            = 5.minutes

  def ensureSomeAnalysis(stats: Map[PerfType, InsightPerfStats.WithGameIds]): Funit =
    val totalNbGames = stats.values.map(_.stats.totalNbGames).sum
    stats.values.toList
      .map { s =>
        val ids = s.gameIds.take(s.stats.totalNbGames * maxGamesToConsider.value / totalNbGames)
        gameRepo.unanalysedGames(ids, Max(s.stats.totalNbGames * maxToAnalyse.value / totalNbGames))
      }
      .parallel
      .map(_.flatten)
      .flatMap { games =>
        games.foreach(g => analyser(g.id))
        awaiter(games.map(_.id), maxTime)
      }
