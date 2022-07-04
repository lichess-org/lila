package lila.tutor

import cats.data.NonEmptyList
import scala.concurrent.ExecutionContext

import lila.common.config
import lila.insight._
import lila.rating.PerfType

case class TutorFlagging(win: TutorBothValueOptions[TutorRatio], loss: TutorBothValueOptions[TutorRatio])

object TutorFlagging {

  private[tutor] def compute(
      user: TutorUser
  )(implicit insightApi: InsightApi, ec: ExecutionContext): Fu[TutorFlagging] = {
    val question = Question(InsightDimension.Result, InsightMetric.Termination) filter
      Filter(InsightDimension.Termination, List(lila.insight.Termination.ClockFlag)) filter
      TutorBuilder.perfFilter(user.perfType)
    def valueCountOf(answer: Answer[Result], result: Result) = answer.clusters.collectFirst {
      case Cluster(res, _, nbGames, _) if res == result =>
        ValueCount(TutorRatio(nbGames.toDouble / user.perfStats.totalNbGames), answer.totalSize)
    }
    for {
      mine <- insightApi.ask(question, user.user, withPovs = false)
      peer <- insightApi.askPeers(question, user.perfStats.rating, nbGames = config.Max(5_000))
    } yield {
      TutorFlagging(
        win = TutorBothValueOptions(
          mine = valueCountOf(mine, Result.Win),
          peer = valueCountOf(peer, Result.Win)
        ),
        loss = TutorBothValueOptions(
          mine = valueCountOf(mine, Result.Loss),
          peer = valueCountOf(peer, Result.Loss)
        )
      )
    }
  }
}
