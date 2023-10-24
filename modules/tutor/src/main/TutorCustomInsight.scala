package lila.tutor

import lila.insight.*
import lila.rating.PerfType
import lila.rating.BSONHandlers.perfTypeIdHandler
import lila.db.AggregationPipeline
import lila.db.dsl.*

final private class TutorCustomInsight[A: TutorNumber: Ordering](
    user: TutorUser,
    question: Question[PerfType],
    monitoringKey: String,
    peerMatch: TutorPerfReport.PeerMatch => TutorBothValueOptions[A]
)(pointParser: Bdoc => Option[ValueCount[A]]):

  def apply(insightColl: Coll)(
      aggregateMine: Bdoc => AggregationPipeline[insightColl.PipelineOperator],
      aggregatePeer: Bdoc => AggregationPipeline[insightColl.PipelineOperator]
  )(using Executor): Fu[TutorBothValueOptions[A]] =
    for
      mine <- insightColl
        .aggregateOne(): _ =>
          aggregateMine(InsightStorage.selectUserId(user.user.id))
        .map(_ flatMap pointParser)
        .monSuccess(_.tutor.askMine(monitoringKey, "all"))
      peer <-
        user.peerMatch.flatMap(peerMatch(_).peer) match
          case Some(cached) => fuccess(cached.some)
          case None =>
            val peerSelect = $doc(lila.insight.InsightEntry.BSONFields.perf -> user.perfType) ++
              InsightStorage.selectPeers(user.perfStats.peers)
            insightColl
              .aggregateOne()(_ => aggregatePeer(peerSelect))
              .map(_ flatMap pointParser)
              .monSuccess(_.tutor.askPeer(monitoringKey, user.perfType.key.value))
    yield TutorBothValueOptions[A](mine, peer)
