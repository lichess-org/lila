package lila.tutor
package build

import akka.stream.scaladsl._
import chess.Color
import com.softwaremill.tagging._
import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.analyse.Analysis
import lila.analyse.AnalysisRepo
import lila.common.IpAddress
import lila.db.dsl._
import lila.fishnet.{ Analyser, FishnetAwaiter }
import lila.game.{ Divider, Game, GameRepo, Pov, Query }
import lila.insight.{ Filter, Insight, InsightApi, InsightDimension, Metric, Question }
import lila.rating.PerfType
import lila.user.{ User, UserRepo }
import lila.insight.Cluster
import lila.common.LilaOpeningFamily
import lila.insight.Phase

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
      performanceAnswer <- insightApi.ask(performanceQuestion, user, withPovs = false)
      avgPerformance = performanceAnswer.clusters.foldLeft((0d, 0)) {
        case ((sum, count), Cluster(_, Insight.Single(point), nbGames, _)) =>
          (sum + point.y * nbGames, count + nbGames)
        case (prev, _) => prev
      } match { case (sum, count) => sum / count }
      familyFilter     = Filter(InsightDimension.OpeningFamily, performanceAnswer.clusters.map(_.x))
      filteredQuestion = performanceQuestion.add(familyFilter)
      acplQuestion = filteredQuestion
        .copy(metric = Metric.MeanCpl)
        .add(Filter(InsightDimension.Phase, List(Phase.Opening, Phase.Middle)))
      acplAnswer            <- insightApi.ask(acplQuestion, user, withPovs = false)
      performancePeerAnswer <- insightApi.askPeers(filteredQuestion, avgPerformance.toInt)
      acplPeerAnswer        <- insightApi.askPeers(acplQuestion, avgPerformance.toInt)
      userTotalNbGames = performanceAnswer.clusters.foldLeft(0)(_ + _.size)
      peerTotalNbGames = performancePeerAnswer.clusters.foldLeft(0)(_ + _.size)
      families = performanceAnswer.clusters.collect {
        case Cluster(family, Insight.Single(point), nbGames, _) =>
          val peerData = performancePeerAnswer.clusters collectFirst {
            case Cluster(fam, Insight.Single(point), nbGames, _) if fam == family =>
              (point.y, TutorRatio(nbGames, peerTotalNbGames))
          }
          TutorOpeningFamily(
            family,
            games = TutorMetric(
              TutorRatio(nbGames, userTotalNbGames),
              peerData.??(_._2)
            ),
            performance = TutorMetric(point.y, peerData.??(_._1)),
            acpl = TutorMetricOption(
              acplAnswer.clusters collectFirst {
                case Cluster(fam, Insight.Single(point), _, _) if fam == family => point.y
              },
              acplPeerAnswer.clusters collectFirst {
                case Cluster(fam, Insight.Single(point), _, _) if fam == family => point.y
              }
            )
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
