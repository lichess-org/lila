package lila.tutor

import cats.data.NonEmptyList
import scala.concurrent.ExecutionContext

import lila.common.config
import lila.insight._
import lila.rating.PerfType

case class TutorFlagging(win: TutorBothValueOptions[TutorRatio], loss: TutorBothValueOptions[TutorRatio])

object TutorFlagging {

  val maxPeerGames = config.Max(10_000)

  private[tutor] def compute(
      user: TutorUser
  )(implicit insightApi: InsightApi, ec: ExecutionContext): Fu[TutorFlagging] = {
    val question = Question(InsightDimension.Result, InsightMetric.Termination) filter
      TutorBuilder.perfFilter(user.perfType)
    for {
      mine <- insightApi.ask(
        question filter Filter(InsightDimension.Termination, List(lila.insight.Termination.ClockFlag)),
        user.user,
        withPovs = false
      )
      peer <- insightApi.askPeers(question, user.perfStats.rating, nbGames = maxPeerGames)
    } yield {
      def valueCountOfMine(result: Result) =
        mine.clusters.collectFirst {
          case Cluster(res, _, nbGames, _) if res == result =>
            ValueCount(TutorRatio(nbGames.toDouble / user.perfStats.totalNbGames), mine.totalSize)
        }
      def valueCountOfPeer(result: Result) =
        peer.clusters.collectFirst {
          case Cluster(res, Insight.Stacked(points), nbGames, _) if res == result =>
            points.collectFirst {
              case (name, point) if name == InsightMetric.MetricValueName(Termination.ClockFlag.name) =>
                ValueCount(TutorRatio.fromPercent(point.y * nbGames / peer.totalSize), peer.totalSize)
            }
        }.flatten
      TutorFlagging(
        win = TutorBothValueOptions(
          mine = valueCountOfMine(Result.Win),
          peer = valueCountOfPeer(Result.Win)
        ),
        loss = TutorBothValueOptions(
          mine = valueCountOfMine(Result.Loss),
          peer = valueCountOfPeer(Result.Loss)
        )
      )
    }
  }
}
