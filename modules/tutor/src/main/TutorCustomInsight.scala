package lila.tutor

import cats.data.NonEmptyList
import scala.concurrent.ExecutionContext

import lila.insight.*
import lila.rating.PerfType
import lila.common.config
import lila.db.dsl.{ *, given }

object TutorCustomInsight:

  opaque type Sort = Boolean
  object Sort extends YesNo[Sort]

  private[tutor] def compute(
      users: NonEmptyList[TutorUser],
      question: Question[PerfType],
      aggregate: (InsightApi, Bdoc, Sort) => Fu[List[Bdoc]],
      clusterParser: List[Bdoc] => List[Cluster[PerfType]],
      monitoringKey: String
  )(using insightApi: InsightApi, ec: ExecutionContext): Fu[TutorBuilder.Answers[PerfType]] =
    for
      mine <- aggregate(insightApi, InsightStorage.selectUserId(users.head.user.id), Sort.Yes)
        .map { docs => TutorBuilder.AnswerMine(Answer(question, clusterParser(docs), Nil)) }
        .monSuccess(_.tutor.askMine(monitoringKey, "all"))
      peer <- aggregate(
        insightApi,
        InsightStorage.selectPeers(Question.Peers(users.head.perfStats.rating)),
        Sort.No
      )
        .map { docs => TutorBuilder.AnswerPeer(Answer(question, clusterParser(docs), Nil)) }
        .monSuccess(_.tutor.askPeer(monitoringKey, "all"))
    yield TutorBuilder.Answers(mine, peer)
