package lila.tutor

import akka.stream.scaladsl._
import chess.Color
import com.softwaremill.tagging._
import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.analyse.{ Analysis, AnalysisRepo }
import lila.common.IpAddress
import lila.db.dsl._
import lila.fishnet.{ Analyser, FishnetAwaiter }
import lila.insight.{
  Answer => InsightAnswer,
  Cluster,
  Filter,
  Insight,
  InsightApi,
  InsightDimension,
  InsightPerfStatsApi,
  Metric,
  Phase,
  Question
}
import lila.rating.PerfType
import lila.user.{ User, UserRepo }
import scala.concurrent.ExecutionContext
import lila.common.config
import scala.concurrent.Future

final private class TutorBuilder(
    insightApi: InsightApi,
    perfStatsApi: InsightPerfStatsApi,
    userRepo: UserRepo,
    fishnetAnalyser: Analyser,
    fishnetAwaiter: FishnetAwaiter,
    reportColl: Coll @@ ReportColl
)(implicit
    ec: ExecutionContext,
    system: akka.actor.ActorSystem
) {

  import TutorBsonHandlers._
  import TutorBuilder._

  implicit private val insightApiImplicit = insightApi
  // private val requireAnalysisOnLastGames  = 15
  // private val timeToWaitForAnalysis       = 1 second
  // private val timeToWaitForAnalysis      = 3 minutes

  def apply(userId: User.ID): Funit = for {
    user     <- userRepo named userId orFail s"No such user $userId"
    hasFresh <- hasFreshReport(user)
    _ <- !hasFresh ?? {
      val chrono = lila.common.Chronometer.lapTry(produce(user))
      chrono.mon { r => lila.mon.tutor.buildFull(r.isSuccess) }
      for {
        lap    <- chrono.lap
        report <- Future fromTry lap.result
        doc = reportHandler.writeOpt(report).get ++ $doc(
          "_id"    -> s"${report.user}:${dateFormatter print report.at}",
          "millis" -> lap.millis
        )
        _ <- reportColl.insert.one(doc).void
      } yield ()
    }
  } yield ()

  private def produce(user: User): Fu[TutorFullReport] = for {
    _         <- insightApi.indexAll(user).monSuccess(_.tutor buildSegment "insight-index")
    perfStats <- perfStatsApi(user, eligiblePerfTypesOf(user)).monSuccess(_.tutor buildSegment "perf-stats")
    tutorUsers = perfStats
      .collect { case (pt, stats) if stats.nbGames > 5 => TutorUser(user, pt, stats) }
      .toList
      .sortBy(-_.perfStats.nbGames)
    perfs <- tutorUsers.map(producePerf).sequenceFu.monSuccess(_.tutor buildSegment "perf-reports")
  } yield TutorFullReport(user.id, DateTime.now, perfs)

  private def producePerf(user: TutorUser): Fu[TutorPerfReport] = for {
    openings <- TutorOpening compute user
    phases   <- TutorPhases compute user
  } yield TutorPerfReport(user.perfType, user.perfStats, openings, phases)

  private[tutor] def eligiblePerfTypesOf(user: User) =
    PerfType.standardWithUltra.filter { pt =>
      user.perfs(pt).latest.exists(_ isAfter DateTime.now.minusMonths(1))
    }

  private def hasFreshReport(user: User) = reportColl.exists(
    $doc(
      TutorFullReport.F.user -> user.id,
      TutorFullReport.F.at $gt DateTime.now.minusMinutes(TutorFullReport.freshness.toMinutes.toInt)
    )
  )

  private val dateFormatter = org.joda.time.format.DateTimeFormat forPattern "yyyy-MM-dd"

  // private def getAnalysis(userId: User.ID, ip: IpAddress, game: Game, index: Int) =
  //   analysisRepo.byGame(game) orElse {
  //     (index < requireAnalysisOnLastGames) ?? requestAnalysis(
  //       game,
  //       lila.fishnet.Work.Sender(userId = userId, ip = ip.some, mod = false, system = false)
  //     )
  //   }

  // private def requestAnalysis(game: Game, sender: lila.fishnet.Work.Sender): Fu[Option[Analysis]] = {
  //   def fetch = analysisRepo byId game.id
  //   fishnetAnalyser(game, sender, ignoreConcurrentCheck = true) flatMap {
  //     case Analyser.Result.Ok              => fishnetAwaiter(game.id, timeToWaitForAnalysis) >> fetch
  //     case Analyser.Result.AlreadyAnalysed => fetch
  //     case _                               => fuccess(none)
  //   }
  // }
}

private object TutorBuilder {

  type Value = Double
  type Count = Int
  type Pair  = (Value, Count)

  val peerNbGames = config.Max(5_000)

  def answerBoth[Dim](question: Question[Dim], user: TutorUser)(implicit
      insightApi: InsightApi,
      ec: ExecutionContext
  ) =
    for {
      mine <- answerMine(question, user)
      peer <- answerPeer(question, user)
    } yield Answers(mine, peer)

  def answerMine[Dim](question: Question[Dim], user: TutorUser)(implicit
      insightApi: InsightApi,
      ec: ExecutionContext
  ) = insightApi
    .ask(question filter perfFilter(user.perfType), user.user, withPovs = false)
    .monSuccess(_.tutor.askMine(question.monKey, user.perfType.key)) map AnswerMine.apply

  def answerPeer[Dim](question: Question[Dim], user: TutorUser, nbGames: config.Max = peerNbGames)(implicit
      insightApi: InsightApi,
      ec: ExecutionContext
  ) = insightApi
    .askPeers(question filter perfFilter(user.perfType), user.perfStats.rating, nbGames = nbGames)
    .monSuccess(_.tutor.askPeer(question.monKey, user.perfType.key)) map AnswerPeer.apply

  sealed abstract class Answer[Dim](answer: InsightAnswer[Dim]) {

    val list: List[(Dim, Value, Count)] =
      answer.clusters.view.collect { case Cluster(dimension, Insight.Single(point), nbGames, _) =>
        (dimension, point.y, nbGames)
      }.toList

    lazy val map: Map[Dim, Pair] = list.view.map { case (dim, value, count) =>
      dim -> (value, count)
    }.toMap

    def get = map.get _

    def dimensions = list.map(_._1)

    lazy val totalCount = list.map(_._3).sum

    def countRatio(count: Count) = TutorRatio(count, totalCount)

    def alignedQuestion = answer.question filter Filter(answer.question.dimension, dimensions)
  }
  case class AnswerMine[Dim](answer: InsightAnswer[Dim]) extends Answer(answer)
  case class AnswerPeer[Dim](answer: InsightAnswer[Dim]) extends Answer(answer)

  case class Answers[Dim](mine: AnswerMine[Dim], peer: AnswerPeer[Dim]) {

    def countMetric(dim: Dim, myCount: Count) = TutorMetric(
      mine countRatio myCount,
      peer.get(dim).map(_._2).map(peer.countRatio)
    )

    def valueMetric(dim: Dim, myValue: Value) = TutorMetric(myValue, peer.get(dim).map(_._1))

    def valueMetric(dim: Dim) = TutorMetricOption(mine.get(dim).map(_._1), peer.get(dim).map(_._1))
  }

  def colorFilter(color: Color)      = Filter(InsightDimension.Color, List(color))
  def perfFilter(perfType: PerfType) = Filter(InsightDimension.Perf, List(perfType))
}
