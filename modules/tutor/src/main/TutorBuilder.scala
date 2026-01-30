package lila.tutor

import lila.core.perf.{ PerfKey, UserWithPerfs }
import lila.db.dsl.{ *, given }
import lila.insight.{
  Answer as InsightAnswer,
  Cluster,
  Filter,
  Insight,
  InsightApi,
  InsightPerfStatsApi,
  Question
}
import lila.rating.PerfType

final private class TutorBuilder(
    colls: TutorColls,
    insightApi: InsightApi,
    perfStatsApi: InsightPerfStatsApi,
    userApi: lila.core.user.UserApi,
    fishnet: TutorFishnet,
    messenger: lila.core.msg.MsgApi,
    routeUrl: lila.core.config.RouteUrl
)(using Executor):

  import TutorBsonHandlers.given
  private given InsightApi = insightApi

  val maxTime = fishnet.maxTime + 5.minutes

  def apply(userId: UserId): Fu[Option[TutorFullReport]] = for
    user <- userApi.withPerfs(userId).orFail(s"No such user $userId")
    hasFresh <- hasFreshReport(user)
    report <- (!hasFresh).so:
      val chrono = lila.common.Chronometer.lapTry(produce(user))
      chrono.mon { r => lila.mon.tutor.buildFull(r.isSuccess) }
      for
        lap <- chrono.lap
        report <- Future.fromTry(lap.result)
        doc = bsonWriteObjTry(report).get ++ $doc(
          "_id" -> s"${report.user}:${dateFormatter.print(report.at)}",
          "millis" -> lap.millis
        )
        _ <- colls.report(_.insert.one(doc).void)
        _ <- messenger.postPreset(userId, doneMsg).void
      yield report.some
  yield report

  private def produce(user: UserWithPerfs): Fu[TutorFullReport] = for
    _ <- insightApi.indexAll(user, force = false).monSuccess(_.tutor.buildSegment("insight-index"))
    perfStats <- perfStatsApi(user, eligiblePerfKeysOf(user).map(PerfType(_)), fishnet.maxGamesToConsider)
      .monSuccess(_.tutor.buildSegment("perf-stats"))
    peerMatches <- findPeerMatches(perfStats.view.mapValues(_.stats.rating).toMap)
      .monSuccess(_.tutor.buildSegment("peer-matches"))
    tutorUsers = perfStats
      .map { (pt, stats) => TutorPlayer(user, pt, stats.stats, peerMatches.find(_.perf == pt)) }
      .toList
      .sortBy(-_.perfStats.totalNbGames)
    _ <- fishnet.ensureSomeAnalysis(perfStats).monSuccess(_.tutor.buildSegment("fishnet-analysis"))
    perfs <- (tutorUsers.toNel.so(TutorPerfReport.compute)).monSuccess(_.tutor.buildSegment("perf-reports"))
  yield TutorFullReport(user.id, nowInstant, perfs)

  private[tutor] def eligiblePerfKeysOf(user: UserWithPerfs): List[PerfKey] =
    lila.rating.PerfType.standardWithUltra.filter: pt =>
      user.perfs(pt).latest.exists(_.isAfter(nowInstant.minusMonths(12)))

  private def hasFreshReport(user: User): Fu[Boolean] = colls.report:
    _.exists:
      $doc(
        TutorFullReport.F.user -> user.id,
        TutorFullReport.F.at.$gt(nowInstant.minusMinutes(TutorFullReport.freshness.toMinutes.toInt))
      )

  private def findPeerMatches(
      perfs: Map[PerfType, lila.insight.MeanRating]
  ): Fu[List[TutorPerfReport.PeerMatch]] =
    val ratingDelta = 2
    perfs
      .map: (pt, rating) =>
        colls.report:
          _.one[Bdoc](
            $doc(
              TutorFullReport.F.perfs -> $doc(
                "$elemMatch" -> $doc(
                  "perf" -> pt.id,
                  "stats.rating".$gte(rating.map(_ - ratingDelta)).$lte(rating.map(_ + ratingDelta))
                )
              ),
              TutorFullReport.F.at.$gt(nowInstant.minusMonths(1)) // index hit
            ),
            $doc(s"${TutorFullReport.F.perfs}.$$" -> true)
          )
            .map: docO =>
              for
                doc <- docO
                reports <- doc.getAsOpt[List[TutorPerfReport]](TutorFullReport.F.perfs)
                report <- reports.headOption
                if report.perf == pt
              yield TutorPerfReport.PeerMatch(report)
      .parallel
      .map(_.toList.flatten)
      .addEffect: matches =>
        perfs.keys.foreach: pt =>
          lila.mon.tutor.peerMatch(matches.exists(_.perf == pt), pt.key).increment()

  private val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")

  lazy private val doneMsg =
    lila.core.msg.MsgPreset("Tutor complete", s"Your tutor report is ready! ${routeUrl(routes.Tutor.home())}")

private object TutorBuilder:

  type Value = Double
  type Count = Int
  type Pair = ValueCount[Value]

  val peerNbGames = Max(5_000)

  def answerMine[Dim](question: Question[Dim], user: TutorPlayer)(using
      insightApi: InsightApi,
      ec: Executor
  ): Fu[AnswerMine[Dim]] = insightApi
    .ask(question.filter(Filter(user.perfType)), user.user, withPovs = false)
    .monSuccess(_.tutor.askMine(question.monKey, user.perfType.key))
    .map(AnswerMine.apply)

  def answerPeer[Dim](question: Question[Dim], user: TutorPlayer, nbGames: Max = peerNbGames)(using
      insightApi: InsightApi,
      ec: Executor
  ): Fu[AnswerPeer[Dim]] = insightApi
    .askPeers(question.filter(Filter(user.perfType)), user.perfStats.rating, nbGames = nbGames)
    .monSuccess(_.tutor.askPeer(question.monKey, user.perfType.key))
    .map(AnswerPeer.apply)

  def answerManyPerfs[Dim](question: Question[Dim], tutorUsers: NonEmptyList[TutorPlayer])(using
      insightApi: InsightApi,
      ec: Executor
  ): Fu[Answers[Dim]] = for
    mine <- insightApi
      .ask(
        question.filter(Filter(tutorUsers.toList.map(_.perfType))),
        tutorUsers.head.user,
        withPovs = false
      )
      .monSuccess(_.tutor.askMine(question.monKey, "all"))
      .map(AnswerMine.apply)
    peerByPerf <- tutorUsers.toList.map(answerPeer(question, _)).parallel
    peer = AnswerPeer(InsightAnswer(question, peerByPerf.flatMap(_.answer.clusters), Nil))
  yield Answers(mine, peer)

  sealed abstract class Answer[Dim](answer: InsightAnswer[Dim]):

    val list: List[(Dim, Pair)] =
      answer.clusters.view.collect { case Cluster(dimension, Insight.Single(point), nbGames, _) =>
        (dimension, ValueCount(point.value, nbGames))
      }.toList

    final lazy val map: Map[Dim, Pair] = list.toMap
    export map.get

    def getValue(dim: Dim): Option[Double] = map.get(dim).map(_.value)

    def dimensions = list._1F

  case class AnswerMine[Dim](answer: InsightAnswer[Dim]) extends Answer(answer)
  case class AnswerPeer[Dim](answer: InsightAnswer[Dim]) extends Answer(answer)

  case class Answers[Dim](mine: AnswerMine[Dim], peer: AnswerPeer[Dim]):

    def valueMetric(dim: Dim): TutorBothOption[Double] = for
      m <- mine.get(dim)
      p <- peer.get(dim)
    yield TutorBothValues(m, p.value)
