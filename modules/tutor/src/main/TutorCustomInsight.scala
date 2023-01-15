package lila.tutor

import cats.data.NonEmptyList
import scala.concurrent.ExecutionContext

import lila.insight.*
import lila.rating.PerfType
import lila.common.config
import lila.rating.BSONHandlers.perfTypeIdHandler
import lila.db.AggregationPipeline
import lila.db.dsl.{ *, given }

final private class TutorCustomInsight(
    users: NonEmptyList[TutorUser],
    question: Question[PerfType],
    monitoringKey: String
)(clusterParser: List[Bdoc] => List[Cluster[PerfType]]):

  def apply(insightColl: Coll)(
      aggregateMine: Bdoc => AggregationPipeline[insightColl.PipelineOperator],
      aggregatePeer: Bdoc => AggregationPipeline[insightColl.PipelineOperator]
  )(using ExecutionContext): Fu[TutorBuilder.Answers[PerfType]] =
    for
      mine <- insightColl
        .aggregateList(maxDocs = Int.MaxValue)(_ =>
          aggregateMine(InsightStorage.selectUserId(users.head.user.id))
        )
        .map { docs => TutorBuilder.AnswerMine(Answer(question, clusterParser(docs), Nil)) }
        .monSuccess(_.tutor.askMine(monitoringKey, "all"))
      peerDocs <- users.toList.map { u =>
        val peerSelect = $doc(lila.insight.InsightEntry.BSONFields.perf -> u.perfType) ++
          InsightStorage.selectPeers(u.perfStats.peers)
        insightColl
          .aggregateList(maxDocs = Int.MaxValue)(_ => aggregatePeer(peerSelect))
          .map(clusterParser)
          .monSuccess(_.tutor.askPeer(monitoringKey, u.perfType.key.value))
      }.sequenceFu
      peer = TutorBuilder.AnswerPeer(Answer(question, peerDocs.flatten, Nil))
    yield TutorBuilder.Answers(mine, peer)
