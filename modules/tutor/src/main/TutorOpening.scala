package lila.tutor

import chess.Color
import scala.concurrent.ExecutionContext

import lila.common.LilaOpeningFamily
import lila.insight.{ Filter, InsightApi, InsightDimension, Metric, Phase, Question }
import lila.rating.PerfType

case class TutorColorOpenings(
    families: List[TutorOpeningFamily]
)

case class TutorOpeningFamily(
    family: LilaOpeningFamily,
    games: TutorMetric[TutorRatio],
    performance: TutorMetric[Double],
    acpl: TutorMetricOption[Double],
    awareness: TutorMetricOption[TutorRatio]
)

private case object TutorOpening {

  import TutorBuilder._

  def compute(user: TutorUser)(implicit
      insightApi: InsightApi,
      ec: ExecutionContext
  ): Fu[Color.Map[TutorColorOpenings]] = for {
    whiteOpenings <- computeOpenings(user, Color.White)
    blackOpenings <- computeOpenings(user, Color.Black)
  } yield Color.Map(whiteOpenings, blackOpenings)

  def computeOpenings(user: TutorUser, color: Color)(implicit
      insightApi: InsightApi,
      ec: ExecutionContext
  ): Fu[TutorColorOpenings] = {
    for {
      myPerfs   <- answerMine(perfQuestion(color), user)
      peerPerfs <- answerPeer(myPerfs.alignedQuestion, user)
      performances = Answers(myPerfs, peerPerfs)
      acplQuestion = myPerfs.alignedQuestion
        .withMetric(Metric.MeanCpl)
        .filter(Filter(InsightDimension.Phase, List(Phase.Opening, Phase.Middle)))
      acpls <- answerBoth(acplQuestion, user)
      awarenessQuestion = acplQuestion.withMetric(Metric.Awareness)
      awareness <- answerBoth(awarenessQuestion, user)
    } yield TutorColorOpenings {
      performances.mine.list.map { case (family, myValue, myCount) =>
        TutorOpeningFamily(
          family,
          games = performances.countMetric(family, myCount),
          performance = performances.valueMetric(family, myValue),
          acpl = acpls valueMetric family,
          awareness = awareness valueMetric family map TutorRatio.fromPercent
        )
      }
    }
  }

  def perfQuestion(color: Color) = Question(
    InsightDimension.OpeningFamily,
    Metric.Performance,
    List(colorFilter(color))
  )
}
