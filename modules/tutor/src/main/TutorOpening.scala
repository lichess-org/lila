package lila.tutor

import chess.Color
import scala.concurrent.ExecutionContext

import lila.analyse.AccuracyPercent
import lila.common.{ Heapsort, LilaOpeningFamily }
import lila.insight.{ Filter, InsightApi, InsightDimension, InsightMetric, Phase, Question }
import lila.rating.PerfType
import lila.tutor.TutorCompare.comparisonOrdering
import lila.common.config

case class TutorColorOpenings(
    families: List[TutorOpeningFamily]
) {
  lazy val accuracyCompare = TutorCompare[LilaOpeningFamily, AccuracyPercent](
    InsightDimension.OpeningFamily,
    TutorMetric.Accuracy,
    families.map { f => (f.family, f.accuracy) }
  )
  lazy val performanceCompare = TutorCompare[LilaOpeningFamily, Rating](
    InsightDimension.OpeningFamily,
    TutorMetric.Performance,
    families.map { f => (f.family, f.performance.toOption) }
  )
  lazy val awarenessCompare = TutorCompare[LilaOpeningFamily, GoodPercent](
    InsightDimension.OpeningFamily,
    TutorMetric.Awareness,
    families.map { f => (f.family, f.awareness) }
  )

  lazy val allCompares = List(accuracyCompare, performanceCompare, awarenessCompare)

  def find(fam: LilaOpeningFamily) = families.find(_.family == fam)
}

case class TutorOpeningFamily(
    family: LilaOpeningFamily,
    performance: TutorBothValues[Rating],
    accuracy: TutorBothValueOptions[AccuracyPercent],
    awareness: TutorBothValueOptions[GoodPercent]
) {

  def mix: TutorBothValueOptions[GoodPercent] = accuracy.map(a => GoodPercent(a.value)) mix awareness
}

private case object TutorOpening {

  import TutorBuilder._

  val nbOpeningsPerColor  = 8
  private val peerNbGames = config.Max(10_000)

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
      myPerfsFull <- answerMine(perfQuestion(color), user)
      myPerfs = myPerfsFull.copy(answer =
        myPerfsFull.answer.copy(
          clusters = myPerfsFull.answer.clusters take nbOpeningsPerColor
        )
      )
      peerPerfs <- answerPeer(myPerfs.alignedQuestion, user, peerNbGames)
      performances = Answers(myPerfs, peerPerfs)
      accuracyQuestion = myPerfs.alignedQuestion
        .withMetric(InsightMetric.MeanAccuracy)
        .filter(Filter(InsightDimension.Phase, List(Phase.Opening, Phase.Middle)))
      accuracy <- answerBoth(accuracyQuestion, user, peerNbGames)
      awarenessQuestion = accuracyQuestion withMetric InsightMetric.Awareness
      awareness <- answerBoth(awarenessQuestion, user, peerNbGames)
    } yield TutorColorOpenings {
      performances.mine.list.map { case (family, myPerformance) =>
        TutorOpeningFamily(
          family,
          performance = performances.valueMetric(family, myPerformance) map Rating.apply,
          accuracy = accuracy valueMetric family map AccuracyPercent.apply,
          awareness = awareness valueMetric family map GoodPercent.apply
        )
      }
    }
  }

  def perfQuestion(color: Color) = Question(
    InsightDimension.OpeningFamily,
    InsightMetric.Performance,
    List(colorFilter(color))
  )
}
