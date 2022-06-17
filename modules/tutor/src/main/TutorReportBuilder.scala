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
import lila.insight.{ Filter, Insight, InsightApi, InsightDimension, Metric, Question, RatingCateg }
import lila.rating.PerfType
import lila.user.{ User, UserRepo }
import lila.insight.Cluster
import lila.common.LilaOpeningFamily

final class TutorReportBuilder(
    userRepo: UserRepo,
    fishnetAnalyser: Analyser,
    fishnetAwaiter: FishnetAwaiter,
    insightApi: InsightApi,
    reportColl: Coll @@ ReportColl
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    mode: play.api.Mode,
    mat: akka.stream.Materializer
) {

  import TutorBsonHandlers._

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
    val question = Question(
      InsightDimension.OpeningFamily,
      Metric.Performance,
      List(
        Filter(InsightDimension.Color, List(color)),
        Filter(InsightDimension.Perf, perfTypes)
      )
    )
    for {
      userAnswer <- insightApi.ask(question, user, withPovs = false)
      peerAnswer <- insightApi.askPeers(
        question.copy(
          filters = Filter(InsightDimension.OpeningFamily, userAnswer.clusters.map(_.x)) :: question.filters
        ),
        ratingCategOf(user)
      )
      userTotalNbGames = userAnswer.clusters.foldLeft(0)(_ + _.size).toFloat
      peerTotalNbGames = peerAnswer.clusters.foldLeft(0)(_ + _.size).toFloat
      families = userAnswer.clusters.collect { case Cluster(family, Insight.Single(point), nbGames, _) =>
        val peerData = peerAnswer.clusters collectFirst {
          case Cluster(fam, Insight.Single(point), nbGames, _) if fam == family =>
            (point.y.toInt, nbGames / peerTotalNbGames)
        }
        TutorOpeningFamily(
          family,
          games = TutorMetric(
            TutorRatio(nbGames / userTotalNbGames),
            TutorRatio(peerData.??(_._2))
          ),
          performance = TutorMetric(point.y.toInt, peerData.??(_._1)),
          acpl = TutorMetric(0, 0)
        )
      }
    } yield TutorColorOpenings(families)
  }

  private def ratingCategOf(user: User) = RatingCateg of user.perfs.standard.intRating

  private val perfTypes =
    List(PerfType.Bullet, PerfType.Blitz, PerfType.Rapid, PerfType.Classical, PerfType.Correspondence)

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
