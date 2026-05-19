package lila.tutor

import lila.analyse.AccuracyPercent
import lila.insight.{ InsightApi, InsightDimension, InsightMetric, Phase, Question }
import lila.rating.PerfType

case class TutorPhase(
    phase: Phase,
    accuracy: TutorBothOption[AccuracyPercent],
    awareness: TutorBothOption[GoodPercent]
):
  def mix: TutorBothOption[GoodPercent] =
    TutorBothValues.mix(accuracy.map(_.map(_.into(GoodPercent))), awareness)

case class TutorPhases(list: List[TutorPhase]):

  // Dimension comparison is not interesting for phase accuracy (opening always better)
  // But peer comparison is gold
  val accuracyCompare = TutorCompare[Phase, AccuracyPercent](
    InsightDimension.Phase,
    TutorMetric.Accuracy,
    list.map { phase => (phase.phase, phase.accuracy) }
  )

  val awarenessCompare = TutorCompare[Phase, GoodPercent](
    InsightDimension.Phase,
    TutorMetric.Awareness,
    list.map { phase => (phase.phase, phase.awareness) }
  )

  def compares = List(accuracyCompare, awarenessCompare)

  val highlights = TutorCompare.mixedBag(compares.flatMap(_.peerComparisons))

private object TutorPhases:

  import TutorBuilder.*

  private val accuracyQuestion = Question(InsightDimension.Phase, InsightMetric.MeanAccuracy)
  private val awarenessQuestion = Question(InsightDimension.Phase, InsightMetric.Awareness)
  private val phases = InsightDimension.valuesOf(InsightDimension.Phase).toList
  private def phasesOf(perfType: PerfType) = perfType match
    case PerfType.Crazyhouse => phases.filter(_ != Phase.End)
    case _ => phases

  private type PhaseGet = Phase => Option[Double]

  def compute(user: TutorPlayer)(using TutorConfig, InsightApi, Executor): Fu[TutorPhases] =

    def cachedOrComputedPeerPhaseGet[V](
        question: Question[Phase],
        cacheGet: TutorPhase => Option[Double]
    ): Fu[PhaseGet] =
      user.peerMatch
        .map:
          _.phases.list.flatMap(p => cacheGet(p).map(p.phase -> _)).toMap
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
    yield TutorPhases:
      phasesOf(user.perfType).map: phase =>
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
