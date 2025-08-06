package lila.tutor

import lila.db.AggregationPipeline
import lila.db.dsl.*
import lila.insight.*
import lila.rating.BSONHandlers.perfTypeIdHandler
import lila.rating.PerfType

final private class TutorCustomInsight[A: TutorNumber](
    users: NonEmptyList[TutorUser],
    question: Question[PerfType],
    monitoringKey: String,
    peerMatch: TutorPerfReport.PeerMatch => TutorBothValueOptions[A]
)(clusterParser: List[Bdoc] => List[Cluster[PerfType]]):

  def apply(insightColl: Coll)(
      aggregateMine: Bdoc => AggregationPipeline[insightColl.PipelineOperator],
      aggregatePeer: Bdoc => AggregationPipeline[insightColl.PipelineOperator]
  )(using Executor): Fu[TutorBuilder.Answers[PerfType]] =
    for
      mine <- insightColl
        .aggregateList(maxDocs = Int.MaxValue)(_ =>
          aggregateMine(InsightStorage.selectUserId(users.head.user.id))
        )
        .map { docs => TutorBuilder.AnswerMine(Answer(question, clusterParser(docs), Nil)) }
        .monSuccess(_.tutor.askMine(monitoringKey, "all"))
      peerDocs <- users.toList.map { u =>
        u.peerMatch.flatMap(peerMatch(_).peer) match
          case Some(cached) =>
            fuccess(List(Cluster(u.perfType, Insight.Single(Point(cached.double.value)), cached.count, Nil)))
          case None =>
            val peerSelect = $doc(lila.insight.InsightEntry.BSONFields.perf -> u.perfType) ++
              InsightStorage.selectPeers(u.perfStats.peers)
            insightColl
              .aggregateList(maxDocs = Int.MaxValue)(_ => aggregatePeer(peerSelect))
              .map(clusterParser)
              .monSuccess(_.tutor.askPeer(monitoringKey, u.perfType.key))
      }.parallel
      peer = TutorBuilder.AnswerPeer(Answer(question, peerDocs.flatten, Nil))
    yield TutorBuilder.Answers(mine, peer)
