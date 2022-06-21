package lila.tutor

import com.softwaremill.tagging._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.config
import lila.fishnet.{ Analyser, FishnetAwaiter, Work }
import lila.game.{ Game, GameRepo }
import lila.insight.InsightPerfStats
import lila.memo.SettingStore
import lila.rating.PerfType
import lila.user.User

final private class TutorFishnet(
    gameRepo: GameRepo,
    analyser: Analyser,
    awaiter: FishnetAwaiter,
    nbAnalysis: SettingStore[Int] @@ NbAnalysis
)(implicit
    ec: ExecutionContext
) {

  val maxToAnalyse       = config.Max(30)
  val maxGamesToConsider = config.Max(maxToAnalyse.value * 2)

  val sender = Work.Sender(userId = lila.user.User.lichessId, ip = none, mod = false, system = true)

  def ensureSomeAnalysis(stats: Map[PerfType, InsightPerfStats.WithGameIds]): Funit = {
    val totalNbGames = stats.values.map(_.stats.nbGames).sum
    stats.values.toList
      .map { s =>
        val ids = s.gameIds.take(s.stats.nbGames * maxGamesToConsider.value / totalNbGames)
        gameRepo.unanalysedGames(ids, config.Max(s.stats.nbGames * maxToAnalyse.value / totalNbGames))
      }
      .sequenceFu
      .map(_.flatten) flatMap { games =>
      games.foreach(g => println(s"${g.perfKey}"))
      games.foreach { analyser(_, sender, ignoreConcurrentCheck = true) }
      awaiter(games.map(_.id), 5 minutes)
    }
  }
}
