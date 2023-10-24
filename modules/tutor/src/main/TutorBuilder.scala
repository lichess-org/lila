package lila.tutor

import chess.Color

import lila.db.dsl.{ *, given }
import lila.insight.{
  Answer as InsightAnswer,
  Cluster,
  Filter,
  Insight,
  InsightApi,
  InsightDimension,
  InsightPerfStatsApi,
  Question,
  InsightPerfStats,
  MeanRating,
  Point
}
import lila.rating.PerfType
import lila.user.{ User, UserApi }
import lila.common.config

final private class TutorBuilder(
    colls: TutorColls,
    insightApi: InsightApi,
    perfStatsApi: InsightPerfStatsApi,
    userApi: UserApi,
    fishnet: TutorFishnet
)(using Executor):

  import TutorBsonHandlers.given
  import TutorBuilder.*
  private given InsightApi = insightApi

  val maxTime = fishnet.maxTime + 5.minutes

  def apply(query: TutorPeriodReport.Query): Fu[Option[TutorPeriodReport]] = for
    user     <- userApi withPerfs query.user orFail s"No such user $query"
    hasFresh <- hasFreshReport(query)
    report <- !hasFresh so {
      val chrono = lila.common.Chronometer.lapTry(produceReport(query, user))
      chrono.mon { r => lila.mon.tutor.buildFull(r.isSuccess) }
      for
        lap    <- chrono.lap
        report <- Future fromTry lap.result
        doc = bsonWriteObjTry(report).get ++ $doc(
          "_id"    -> s"${report.user}:${dateFormatter print report.at}",
          "millis" -> lap.millis
        )
        _ <- colls.report.insert.one(doc).void
      yield report.some
    }
  yield report

  private def produceReport(query: TutorPeriodReport.Query, user: User.WithPerfs): Fu[TutorPeriodReport] = for
    _ <- insightApi.indexAll(user).monSuccess(_.tutor buildSegment "insight-index")
    perfStats <- perfStatsApi
      .only(user, query.perf, query.nb into config.Max, fishnet.maxGamesToConsider)
      .monSuccess(_.tutor buildSegment "perf-stats")
      .map(_ | InsightPerfStats.empty)
    peerCache <- findPeerMatch(query.perf, perfStats.stats.rating)
    tutorUser = TutorUser(user, query.perf, perfStats.stats, peerCache)
    _      <- fishnet.ensureSomeAnalysis(perfStats).monSuccess(_.tutor buildSegment "fishnet-analysis")
    report <- TutorPerfReport.compute(tutorUser).monSuccess(_.tutor buildSegment "perf-report")
  yield TutorPeriodReport(TutorPeriodReport.Id.make, user.id, nowInstant, query.nb, report)

  private[tutor] def eligiblePerfTypesOf(user: User.WithPerfs) =
    PerfType.standardWithUltra.filter: pt =>
      user.perfs(pt).latest.exists(_ isAfter nowInstant.minusMonths(12))

  private def hasFreshReport(query: TutorPeriodReport.Query): Fu[Boolean] = colls.report.exists:
    $doc(
      TutorPeriodReport.F.user    -> query.user,
      TutorPeriodReport.F.nbGames -> query.nb,
      TutorPeriodReport.F.at $gt nowInstant.minusMinutes(TutorFullReport.freshness.toMinutes.toInt)
    )

  private def findPeerMatch(
      perf: PerfType,
      rating: MeanRating
  ): Fu[Option[TutorPerfReport.PeerMatch]] =
    colls.report
      .one[TutorPeriodReport](
        $doc(
          TutorPeriodReport.F.perf -> perf.id,
          TutorPeriodReport.F.meanRating.$gte(rating - 2).$lte(rating + 2),
          TutorPeriodReport.F.at $gt nowInstant.minusMonths(1) // index hit
        )
      )
      .map:
        _.map(_.report).map(TutorPerfReport.PeerMatch.apply)
      .addEffect: res =>
        lila.mon.tutor.peerMatch(res.isDefined).increment()

  private val dateFormatter = java.time.format.DateTimeFormatter ofPattern "yyyy-MM-dd"

private object TutorBuilder:

  type Value = Double
  type Count = Int
  type Pair  = ValueCount[Value]

  val peerNbGames = config.Max(5_000)

  def answerMine[Dim](question: Question[Dim], user: TutorUser)(using
      insightApi: InsightApi,
      ec: Executor
  ): Fu[AnswerMine[Dim]] = insightApi
    .ask(question filter perfFilter(user), user.user, withPovs = false)
    .monSuccess(_.tutor.askMine(question.monKey, user.perfType.key.value)) map AnswerMine.apply

  def answerPeer[Dim](question: Question[Dim], user: TutorUser, nbGames: config.Max = peerNbGames)(using
      insightApi: InsightApi,
      ec: Executor
  ): Fu[AnswerPeer[Dim]] = insightApi
    .askPeers(question filter perfFilter(user), user.perfStats.rating, nbGames = nbGames)
    .monSuccess(_.tutor.askPeer(question.monKey, user.perfType.key.value)) map AnswerPeer.apply

  def answerBoth[Dim](question: Question[Dim], user: TutorUser, nbPeerGames: config.Max = peerNbGames)(using
      InsightApi,
      Executor
  ): Fu[Answers[Dim]] = for
    mine <- answerMine(question, user)
    peer <- answerPeer(question, user, nbPeerGames)
  yield Answers(mine, peer)

  // def answerManyPerfs[Dim](question: Question[Dim], tutorUsers: NonEmptyList[TutorUser])(using
  //     insightApi: InsightApi,
  //     ec: Executor
  // ): Fu[Answers[Dim]] = for
  //   mine <- insightApi
  //     .ask(
  //       question filter perfsFilter(tutorUsers.toList.map(_.perfType)),
  //       tutorUsers.head.user,
  //       withPovs = false
  //     )
  //     .monSuccess(_.tutor.askMine(question.monKey, "all")) map AnswerMine.apply
  //   peerByPerf <- tutorUsers.toList.map { answerPeer(question, _) }.parallel
  //   peer = AnswerPeer(InsightAnswer(question, peerByPerf.flatMap(_.answer.clusters), Nil))
  // yield Answers(mine, peer)

  sealed abstract class Answer[Dim](answer: InsightAnswer[Dim]):

    val list: List[(Dim, Pair)] =
      answer.clusters.view.collect { case Cluster(dimension, Insight.Single(point), nbGames, _) =>
        (dimension, ValueCount(point.value, nbGames))
      }.toList

    final lazy val map: Map[Dim, Pair] = list.toMap
    export map.get

    def dimensions      = list.map(_._1)
    def alignedQuestion = answer.question filter Filter(answer.question.dimension, dimensions)

  case class AnswerMine[Dim](answer: InsightAnswer[Dim]) extends Answer(answer)
  case class AnswerPeer[Dim](answer: InsightAnswer[Dim]) extends Answer(answer)

  case class Answers[Dim](mine: AnswerMine[Dim], peer: AnswerPeer[Dim]):
    def valueMetric(dim: Dim, myValue: Pair) = TutorBothValues(myValue, peer.get(dim))
    def valueMetric(dim: Dim)                = TutorBothValueOptions(mine.get(dim), peer.get(dim))

  def colorFilter(color: Color)   = Filter(InsightDimension.Color, List(color))
  def perfFilter(user: TutorUser) = Filter(InsightDimension.Perf, List(user.perfType))
  // def perfsFilter(perfTypes: Iterable[PerfType]) = Filter(InsightDimension.Perf, perfTypes.toList)
