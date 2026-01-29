package lila.tutor

import lila.analyse.AccuracyPercent
import lila.insight.{ InsightApi, InsightDimension, InsightMetric, Phase, Question }

case class TutorPhase(
    phase: Phase,
    accuracy: TutorBothOption[AccuracyPercent],
    awareness: TutorBothOption[GoodPercent]
):

  def mix: TutorBothOption[GoodPercent] =
    TutorBothValues.mix(accuracy.map(_.map(_.into(GoodPercent))), awareness)

private object TutorPhases:

  import TutorBuilder.*

  private val accuracyQuestion = Question(InsightDimension.Phase, InsightMetric.MeanAccuracy)
  private val awarenessQuestion = Question(InsightDimension.Phase, InsightMetric.Awareness)

  def compute(user: TutorPlayer)(using insightApi: InsightApi, ec: Executor): Fu[List[TutorPhase]] =
    for
      accuracy <- answerBoth(accuracyQuestion, user)
      awareness <- answerBoth(awarenessQuestion, user)
    yield InsightDimension
      .valuesOf(InsightDimension.Phase)
      .toList
      .map: phase =>
        TutorPhase(
          phase,
          accuracy = AccuracyPercent.from(accuracy.valueMetric(phase)),
          awareness = GoodPercent.from(awareness.valueMetric(phase))
        )
