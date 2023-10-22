package lila.tutor

import com.softwaremill.tagging.*

import lila.common.config
import lila.fishnet.{ Analyser, FishnetAwaiter, Work }
import lila.game.GameRepo
import lila.insight.InsightPerfStats
import lila.memo.SettingStore
import lila.rating.PerfType
import lila.user.User

final private class TutorFishnet(
    gameRepo: GameRepo,
    analyser: Analyser,
    awaiter: FishnetAwaiter,
    nbAnalysis: SettingStore[Int] @@ NbAnalysis
)(using Executor):

  def maxToAnalyse       = config.Max(nbAnalysis.get())
  def maxGamesToConsider = config.Max(maxToAnalyse.value * 2)
  val maxTime            = 5.minutes

  val sender = Work.Sender(userId = lila.user.User.lichessId, ip = none, mod = false, system = true)

  def ensureSomeAnalysis(stats: InsightPerfStats.WithGameIds): Funit =
    gameRepo
      .unanalysedGames(stats.gameIds.take(maxGamesToConsider.value), maxToAnalyse)
      .flatMap: games =>
        games.foreach { analyser(_, sender, ignoreConcurrentCheck = true) }
        awaiter(games.map(_.id), maxTime)
