package lila.tutor

import cats.data.NonEmptyList
import scala.concurrent.ExecutionContext

import lila.insight._
import lila.rating.PerfType
import lila.common.config

object TutorClockUsage {

  val maxGames = config.Max(10_000)

  private[tutor] def compute(
      users: NonEmptyList[TutorUser]
  )(implicit insightApi: InsightApi, ec: ExecutionContext): Fu[TutorBuilder.Answers[PerfType]] = {
    import lila.db.dsl._
    import lila.rating.BSONHandlers.perfTypeIdHandler
    import lila.insight.{ Insight, Cluster, Answer, InsightStorage, Point }
    import lila.insight.InsightEntry.{ BSONFields => F }
    import lila.insight.BSONHandlers.clockPercentHandler
    val perfs = users.toList.map(_.perfType)
    val question = Question(
      InsightDimension.Perf,
      InsightMetric.ClockPercent,
      List(Filter(InsightDimension.Perf, perfs))
    )
    def clusterParser(docs: List[Bdoc]) = for {
      doc          <- docs
      perf         <- doc.getAsOpt[PerfType]("_id")
      clockPercent <- doc.getAsOpt[ClockPercent]("cp")
      size         <- doc.int("nb")
    } yield Cluster(perf, Insight.Single(Point(100 - clockPercent.value)), size, Nil)
    def aggregate(select: Bdoc, sort: Boolean) = insightApi.coll {
      _.aggregateList(maxDocs = Int.MaxValue) { implicit framework =>
        import framework._
        Match($doc(F.result -> Result.Loss.id, F.perf $in perfs) ++ select) -> List(
          sort option Sort(Descending(F.date)),
          Limit(maxGames.value).some,
          Project($doc(F.perf -> true, F.moves -> $doc("$last" -> s"$$${F.moves}"))).some,
          UnwindField(F.moves).some,
          Project($doc(F.perf -> true, "cp" -> s"$$${F.moves}.s")).some,
          GroupField(F.perf)("cp" -> AvgField("cp"), "nb" -> SumAll).some,
          Project($doc(F.perf -> true, "nb" -> true, "cp" -> $doc("$toInt" -> "$cp"))).some
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
  }
}
