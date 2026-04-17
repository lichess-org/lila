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
import lila.common.LilaFuture

final private class TutorBuilder(
    colls: TutorColls,
    insightApi: InsightApi,
    perfStatsApi: InsightPerfStatsApi,
    userApi: lila.core.user.UserApi,
    fishnet: TutorFishnet,
    notifyApi: lila.core.notify.NotifyApi
)(using Executor, Scheduler):

  import TutorBsonHandlers.given
  private given InsightApi = insightApi

  val maxTime = fishnet.maxTime + 3.minutes

  def apply(config: TutorConfig): Fu[TutorFullReport] = for
    user <- userApi.withPerfs(config.user).orFail(s"No such user ${config.user}")
    chrono = lila.common.Chronometer.lapTry(produce(user)(using config))
    _ = chrono.mon { r => lila.mon.tutor.buildFull(r.isSuccess) }
    lap <- chrono.lap
    report <- lap.result.toFuture
    doc = bsonWriteObjTry(report).get ++ $doc("_id" -> report.id, "millis" -> lap.millis)
    _ <- colls.report(_.insert.one(doc).void)
    _ <- notifyOf(report)
  yield report

  private def produce(user: UserWithPerfs)(using config: TutorConfig): Fu[TutorFullReport] = for
    _ <- insightApi.indexAll(user, force = false).monSuccess(_.tutor.buildSegment("insight-index"))
    perfStats <- perfStatsApi(
      user,
      config.period,
      eligiblePerfKeysOf(user).map(PerfType(_)),
      fishnet.maxGamesToConsider
    )
      .monSuccess(_.tutor.buildSegment("perf-stats"))
    peerMatches <- findPeerMatches(perfStats.view.mapValues(_.stats.rating).toMap)
      .monSuccess(_.tutor.buildSegment("peer-matches"))
    tutorUsers = perfStats
      .map { (pt, stats) => TutorPlayer(user, pt, stats.stats, peerMatches.find(_.perf == pt)) }
      .toList
      .sortBy(-_.perfStats.totalNbGames)
    _ <- fishnet.ensureSomeAnalysis(perfStats).monSuccess(_.tutor.buildSegment("fishnet-analysis"))
    _ <- LilaFuture.sleep(1.second) // ensure fishnet analyses are indexed before asking questions
    perfs <- tutorUsers.toNel
      .so(TutorPerfReport.compute)
      .monSuccess(_.tutor.buildSegment("perf-reports"))
  yield TutorFullReport(config, nowInstant, perfs)

  private[tutor] def eligiblePerfKeysOf(user: UserWithPerfs): List[PerfKey] =
    supportedPerfs.filter: pt =>
      val perf = user.perfs(pt)
      perf.nb >= 30 && perf.latest.exists(_.isAfter(lila.insight.minDate))

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

  private def notifyOf(report: TutorFullReport) =
    notifyApi.notifyOne(
      report.config.user,
      lila.core.notify.NotificationContent.GenericLink(
        url = report.url.root.url,
        title = "Tutor report ready".some,
        text =
          if report.nbGames > 0
          then s"${report.nbGames} games analyzed".some
          else "Not enough games in the time range".some,
        icon = lila.ui.Icon.Checkmark.value
      )
    )

private object TutorBuilder:

  type Value = Double
  type Count = Int
  type Pair = ValueCount[Value]

  val peerNbGames = Max(5_000)

  def answerMine[Dim](question: Question[Dim], user: TutorPlayer)(using
      config: TutorConfig,
      insightApi: InsightApi,
      ec: Executor
  ): Fu[AnswerMine[Dim]] = insightApi
    .ask(question.timeFilter(config).filter(Filter(user.perfType)), user.user, withPovs = false)
    .monSuccess(_.tutor.askMine(question.monKey, user.perfType.key))
    .map(AnswerMine.apply)

  def answerPeer[Dim](question: Question[Dim], user: TutorPlayer, nbGames: Max = peerNbGames)(using
      insightApi: InsightApi,
      ec: Executor
  ): Fu[AnswerPeer[Dim]] = insightApi
    .askPeers(question.filter(Filter(user.perfType)), user.perfStats.rating, nbGames = nbGames)
    .monSuccess(_.tutor.askPeer(question.monKey, user.perfType.key))
    .map(AnswerPeer.apply)

  def answerManyPerfs[Dim](
      question: Question[Dim],
      tutorUsers: NonEmptyList[TutorPlayer]
  )(using
      config: TutorConfig,
      insightApi: InsightApi,
      ec: Executor
  ): Fu[Answers[Dim]] = for
    mine <- insightApi
      .ask(
        question.timeFilter(config).filter(Filter(tutorUsers.toList.map(_.perfType))),
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
