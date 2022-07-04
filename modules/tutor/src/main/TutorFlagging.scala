package lila.tutor

import cats.data.NonEmptyList
import scala.concurrent.ExecutionContext
import lila.insight._
import lila.rating.PerfType

case class TutorFlagging(win: TutorBothValueOptions[TutorRatio], loss: TutorBothValueOptions[TutorRatio])

object TutorFlagging {

  private[tutor] def compute(
      users: NonEmptyList[TutorUser]
  )(implicit insightApi: InsightApi, ec: ExecutionContext): Fu[Map[PerfType, TutorFlagging]] =
    for {
      mine <- insightApi
        .ask(
          Question(InsightDimension.Perf, InsightMetric.Result) filter
            Filter(InsightDimension.Termination, List(lila.insight.Termination.ClockFlag)) filter
            TutorBuilder.perfsFilter(users.toList.map(_.perfType)),
          users.head.user,
          withPovs = false
        )
      // Answer(
      // Question(Perf,Result,List(Filter(Perf,List(Blitz, Bullet)), Filter(Termination,List(ClockFlag)))),
      // List(
      // Cluster(Bullet,Stacked(List((MetricValueName(Victory),Point(71.46131805157593)), (MetricValueName(Draw),Point(5.329512893982808)), (MetricValueName(Defeat),Point(23.20916905444126)))),1745,List()),
      // Cluster(Blitz,Stacked(List((MetricValueName(Victory),Point(69.95073891625616)), (MetricValueName(Draw),Point(6.502463054187192)), (MetricValueName(Defeat),Point(23.54679802955665)))),1015,List())),List())
    } yield {
      mine.clusters
        .collect { case Cluster(perf, stackedPoints: Insight.Stacked, nbFlaggedGames, _) =>
          users.toList.find(_.perfType == perf).map { tutorUser =>
            def valueCountOf(
                stacked: Insight.Stacked,
                name: InsightMetric.MetricValueName
            ) =
              stacked.points.collectFirst {
                case (valueName, point) if valueName == name =>
                  ValueCount(
                    TutorRatio.fromPercent(point.y * nbFlaggedGames / tutorUser.perfStats.totalNbGames),
                    nbFlaggedGames
                  )
              }
            perf -> TutorFlagging(
              win = TutorBothValueOptions(
                mine = valueCountOf(
                  stackedPoints,
                  InsightMetric.MetricValueName("Victory")
                ),
                peer = none
              ),
              loss = TutorBothValueOptions(
                mine = valueCountOf(
                  stackedPoints,
                  InsightMetric.MetricValueName("Defeat")
                ),
                peer = none
              )
            )
          }
        }
        .flatten
        .toMap
    }
}
