package lila.tutor

import cats.data.NonEmptyList
import scala.concurrent.ExecutionContext

import lila.insight.*
import lila.rating.PerfType
import lila.common.config
import lila.db.dsl.{ *, given }
import lila.rating.BSONHandlers.perfTypeIdHandler
import lila.insight.{ Insight, Cluster, Answer, InsightStorage, Point }
import lila.insight.InsightEntry.{ BSONFields as F }
import lila.insight.BSONHandlers.given

object TutorOvercome:

  val maxGames = config.Max(10_000)

  private[tutor] def compute(
      users: NonEmptyList[TutorUser]
  )(using insightApi: InsightApi, ec: ExecutionContext): Fu[TutorBuilder.Answers[PerfType]] =
    val perfs = users.toList.map(_.perfType)
    val question = Question(
      InsightDimension.Perf,
      InsightMetric.MeanAccuracy,
      List(Filter(InsightDimension.Perf, perfs))
    )

    def clusterParser(docs: List[Bdoc]) = for {
      doc     <- docs
      perf    <- doc.getAsOpt[PerfType]("_id")
      winDiff <- doc.getAsOpt[Double]("diff")
      size    <- doc.int("nb")
    } yield Cluster(perf, Insight.Single(Point(50 + winDiff)), size, Nil)

    def aggregate(select: Bdoc, sort: Boolean) = insightApi.coll {
      _.aggregateList(maxDocs = Int.MaxValue) { framework =>
        import framework.*
        Match($doc(F.analysed -> true, F.perf $in perfs) ++ select) -> List(
          sort option Sort(Descending(F.date)),
          Limit(maxGames.value).some,
          Project($doc(F.perf -> true, s"${F.moves}.w" -> true)).some,
          UnwindField(F.moves).some,
          GroupField("_id")(
            F.perf -> FirstField(F.perf),
            "mean" -> AvgField(s"${F.moves}.w"),
            "last" -> LastField(s"${F.moves}.w")
          ).some,
          Match($doc("mean" $lt 50)).some,
          AddFields($doc("diff" -> $doc("$subtract" -> $arr("$last", "$mean")))).some,
          GroupField(F.perf)(
            "diff" -> AvgField("diff"),
            "nb"   -> SumAll
          ).some
        ).flatten
      }
    }
    for {
      mine <- aggregate(InsightStorage.selectUserId(users.head.user.id), true)
        .map { docs => TutorBuilder.AnswerMine(Answer(question, clusterParser(docs), Nil)) }
        .monSuccess(_.tutor.askMine(question.monKey, "all"))
      peer <- aggregate(InsightStorage.selectPeers(Question.Peers(users.head.perfStats.rating)), false)
        .map { docs => TutorBuilder.AnswerPeer(Answer(question, clusterParser(docs), Nil)) }
        .monSuccess(_.tutor.askPeer(question.monKey, "all"))
    } yield TutorBuilder.Answers(mine, peer)
