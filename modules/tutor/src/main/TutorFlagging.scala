package lila.tutor

import alleycats.Zero

import lila.insight.*

case class TutorFlagging(win: TutorBothValueOptions[GoodPercent], loss: TutorBothValueOptions[GoodPercent])

object TutorFlagging:

  given Zero[TutorFlagging] =
    val values = TutorBothValueOptions.zero[GoodPercent].zero
    Zero(TutorFlagging(values, values))

  val maxPeerGames = Max(10_000)

  private[tutor] def compute(
      user: TutorPlayer
  )(using insightApi: InsightApi, ec: Executor): Fu[TutorFlagging] =
    val question = Question(InsightDimension.Result, InsightMetric.Termination).filter(
      TutorBuilder.perfFilter(user.perfType)
    )
    val clockFlagValueName = InsightMetric.MetricValueName(Termination.ClockFlag.name)
    for
      mine <- insightApi.ask(question, user.user, withPovs = false)
      peer <- insightApi.askPeers(question, user.perfStats.rating, nbGames = maxPeerGames)
    yield
      def valueCountOf(answer: Answer[Result], result: Result) =
        answer.clusters.collectFirst:
          case Cluster(res, Insight.Stacked(points), _, _) if res == result =>
            ValueCount(
              GoodPercent(~points.collectFirst {
                case (valueName, point) if valueName == clockFlagValueName => point.value
              }),
              mine.totalSize
            )
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
