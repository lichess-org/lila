package lila.tutor

import alleycats.Zero

import lila.insight.*
import lila.rating.PerfType

private case class TutorFlagging(
    win: TutorBothValueOptions[GoodPercent],
    loss: TutorBothValueOptions[GoodPercent]
)

private object TutorFlagging:

  private def relevant(pt: PerfType): Boolean = pt != PerfType.Correspondence && pt != PerfType.Classical

  given Zero[TutorFlagging] =
    val values = TutorBothValueOptions.zero[GoodPercent].zero
    Zero(TutorFlagging(values, values))

  val maxPeerGames = Max(10_000)

  private[tutor] def computeIfRelevant(
      user: TutorPlayer
  )(using insightApi: InsightApi, ec: Executor): Fu[TutorFlagging] = relevant(user.perfType).so:
    val question = Question(InsightDimension.Result, InsightMetric.Termination).filter(Filter(user.perfType))
    val clockFlagValueName = InsightMetric.MetricValueName(Termination.ClockFlag.name)
    for
      mine <- insightApi
        .ask(question, user.user, withPovs = false)
        .monSuccess(_.tutor.askMine(question.monKey, user.perfType.key))
      peer <- insightApi
        .askPeers(question, user.perfStats.rating, nbGames = maxPeerGames)
        .monSuccess(_.tutor.askPeer(question.monKey, user.perfType.key))
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
