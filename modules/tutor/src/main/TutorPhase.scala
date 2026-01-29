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
  private val phases = InsightDimension.valuesOf(InsightDimension.Phase).toList

  private type PhaseGet = Phase => Option[Double]

  def compute(user: TutorPlayer)(using InsightApi, Executor): Fu[List[TutorPhase]] =

    def cachedOrComputedPeerPhaseGet[V](
        question: Question[Phase],
        cacheGet: TutorPhase => Option[Double]
    ): Fu[PhaseGet] =
      user.peerMatch
        .map:
          _.phases.flatMap(p => cacheGet(p).map(p.phase -> _)).toMap
        .filter(_.size == phases.size)
        .map(_.get)
        .match
          case Some(cache) => fuccess(cache)
          case None => answerPeer(question, user, peerNbGames).map(_.getValue)

    for
      myAccuracy <- answerMine(accuracyQuestion, user)
      peerAccuracyGet <- cachedOrComputedPeerPhaseGet(accuracyQuestion, _.accuracy.map(_.peer.value))
      myAwareness <- answerMine(awarenessQuestion, user)
      peerAwarenessGet <- cachedOrComputedPeerPhaseGet(awarenessQuestion, _.awareness.map(_.peer.value))
    yield phases.map: phase =>
      TutorPhase(
        phase,
        accuracy = for
          mine <- myAccuracy.get(phase)
          peer <- peerAccuracyGet(phase)
        yield AccuracyPercent.from(TutorBothValues(mine, peer)),
        awareness = for
          mine <- myAwareness.get(phase)
          peer <- peerAwarenessGet(phase)
        yield GoodPercent.from(TutorBothValues(mine, peer))
      )
