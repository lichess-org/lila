package lila.tutor
package build

import akka.stream.scaladsl._
import chess.Color
import com.softwaremill.tagging._
import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.analyse.{ Analysis, AnalysisRepo }
import lila.common.{ IpAddress, LilaOpeningFamily }
import lila.db.dsl._
import lila.fishnet.{ Analyser, FishnetAwaiter }
import lila.game.{ Divider, Game, GameRepo, Pov, Query }
import lila.insight.{
  Answer => InsightAnswer,
  Cluster,
  Filter,
  Insight,
  InsightApi,
  InsightDimension,
  Metric,
  Phase,
  Question
}
import lila.rating.PerfType
import lila.user.{ User, UserRepo }

final class TutorReportBuilder(
    userRepo: UserRepo,
    fishnetAnalyser: Analyser,
    fishnetAwaiter: FishnetAwaiter,
    insightApi: InsightApi,
    reportColl: Coll @@ ReportColl
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  import TutorBsonHandlers._
  import TutorReportBuilder._
  // import TutorRatio.{ ordering, zero }

  private val maxGames                   = 1000
  private val requireAnalysisOnLastGames = 15
  private val timeToWaitForAnalysis      = 1 second
  // private val timeToWaitForAnalysis      = 3 minutes

  private val sequencer = new lila.hub.AskPipelines[(User.ID, IpAddress), TutorFullReport](
    compute = getOrCompute,
    expiration = 1 hour,
    timeout = 1 hour,
    name = "tutor.fullReport"
  )

  def apply(user: User, ip: IpAddress): Fu[TutorFullReport] = sequencer(user.id -> ip)

  private def getOrCompute(key: (User.ID, IpAddress)): Fu[TutorFullReport] = key match {
    case (userId, ip) =>
      for {
        // previous <- reportColl.find($doc("user" -> userId)).sort($sort desc "at").one[TutorFullReport]
        previous <- fuccess(none[TutorFullReport])
        report <- previous match {
          case Some(p) if p.isFresh => fuccess(p)
          case prev =>
            for {
              newReport <- compute(userId, ip, prev)
              _         <- reportColl.insert.one(newReport)
            } yield newReport
        }
      } yield report
  }

  private def compute(
      userId: User.ID,
      ip: IpAddress,
      previous: Option[TutorFullReport]
  ): Fu[TutorFullReport] = for {
    user          <- userRepo.byId(userId) orFail s"Missing tutor user $userId"
    whiteOpenings <- computeOpenings(user, Color.White)
    blackOpenings <- computeOpenings(user, Color.Black)
  } yield TutorFullReport(userId, DateTime.now, TutorOpenings(Color.Map(whiteOpenings, blackOpenings)))

  private def computeOpenings(user: User, color: Color): Fu[TutorColorOpenings] = {
    val performanceQuestion = Question(
      InsightDimension.OpeningFamily,
      Metric.Performance,
      List(
        Filter(InsightDimension.Color, List(color)),
        Filter(InsightDimension.Perf, PerfType.standard)
      )
    )
    for {
      myPerformances <- insightApi.ask(performanceQuestion, user, withPovs = false) map Answer.apply
      familyFilter     = Filter(InsightDimension.OpeningFamily, myPerformances.dimensions)
      filteredQuestion = performanceQuestion.add(familyFilter)
      acplQuestion = filteredQuestion
        .copy(metric = Metric.MeanCpl)
        .add(Filter(InsightDimension.Phase, List(Phase.Opening, Phase.Middle)))
      myAcpls <- insightApi.ask(acplQuestion, user, withPovs = false) map Answer.apply
      peerPerformances <- insightApi.askPeers(
        filteredQuestion,
        myPerformances.average.toInt
      ) map Answer.apply
      performances = Answers(myPerformances, peerPerformances)
      peerAcpls <- insightApi.askPeers(acplQuestion, myPerformances.average.toInt) map Answer.apply
      acpls = Answers(myAcpls, peerAcpls)
      families = performances.list.map { case (family, (myValue, myCount), peer) =>
        TutorOpeningFamily(
          family,
          games = TutorMetric(
            myPerformances countRatio myCount,
            peer.map(_._2).map(peerPerformances.countRatio)
          ),
          performance = TutorMetric(myValue, peer.map(_._1)),
          acpl = acpls metric family
        )
      }
    } yield TutorColorOpenings(families)
  }

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

private object TutorReportBuilder {

  type Value = Double
  type Count = Int
  type Pair  = (Value, Count)

  case class Answer[Dim](answer: InsightAnswer[Dim]) {

    val list: List[(Dim, Value, Count)] =
      answer.clusters.view.collect { case Cluster(dimension, Insight.Single(point), nbGames, _) =>
        (dimension, point.y, nbGames)
      }.toList

    val map: Map[Dim, Pair] = list.view.map { case (dim, value, count) =>
      dim -> (value, count)
    }.toMap

    def get = map.get _

    def dimensions = list.map(_._1)

    lazy val average =
      list.foldLeft((0d, 0)) { case ((sum, count), (_, y, nb)) =>
        (sum + y * nb, count + nb)
      } match { case (sum, count) => sum / count }

    lazy val totalCount = list.map(_._3).sum

    def countRatio(count: Count) = TutorRatio(count, totalCount)
  }

  case class Answers[Dim](mine: Answer[Dim], peer: Answer[Dim]) {

    def list: List[(Dim, Pair, Option[Pair])] = mine.list.map { case (dim, value, count) =>
      (dim, (value, count), peer.map.get(dim))
    }

    def metric(dim: Dim) = TutorMetricOption(mine.get(dim).map(_._1), peer.get(dim).map(_._1))
  }
}
