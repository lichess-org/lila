package lila.tutor

import alleycats.Zero

import lila.insight.*
import lila.rating.PerfType

private case class TutorFlagging(
    win: TutorBothOption[GoodPercent],
    loss: TutorBothOption[GoodPercent]
)

private object TutorFlagging:

  private def relevant(pt: PerfType): Boolean = pt != PerfType.Correspondence && pt != PerfType.Classical

  given Zero[TutorFlagging] = Zero(TutorFlagging(none, none))

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
      def valueCountOf(answer: Answer[Result], result: Result): Option[ValueCount[GoodPercent]] =
        answer.clusters.collectFirst:
          case Cluster(res, Insight.Stacked(points), _, _) if res == result =>
            ValueCount(
              GoodPercent(~points.collectFirst {
                case (valueName, point) if valueName == clockFlagValueName => point.value
              }),
              mine.totalSize
            )
      def valueOf(answer: Answer[Result], result: Result): Option[GoodPercent] =
        valueCountOf(answer, result).map(_.value)

      TutorFlagging(
        win = (valueCountOf(mine, Result.Win), valueOf(peer, Result.Win)).mapN(TutorBothValues(_, _)),
        loss = (valueCountOf(mine, Result.Loss), valueOf(peer, Result.Loss)).mapN(TutorBothValues(_, _))
      )
