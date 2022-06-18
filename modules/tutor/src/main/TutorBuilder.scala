package lila.tutor

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
import scala.concurrent.ExecutionContext

final class TutorBuilder(
    insightApi: InsightApi,
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
  private val requireAnalysisOnLastGames  = 15
  private val timeToWaitForAnalysis       = 1 second
  // private val timeToWaitForAnalysis      = 3 minutes

  private val sequencer = new lila.hub.AskPipelines[(User.ID, IpAddress), TutorReport](
    compute = getOrCompute,
    expiration = 1 hour,
    timeout = 1 hour,
    name = "tutor.fullReport"
  )

  def apply(user: User, ip: IpAddress): Fu[TutorReport] = sequencer(user.id -> ip)

  private def getOrCompute(key: (User.ID, IpAddress)): Fu[TutorReport] = key match {
    case (userId, ip) =>
      for {
        previous <- reportColl.find($doc("user" -> userId)).sort($sort desc "at").one[TutorReport]
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

  private def compute(userId: User.ID, ip: IpAddress, previous: Option[TutorReport]): Fu[TutorReport] = for {
    u <- userRepo.byId(userId) orFail s"Missing tutor user $userId"
    meanRating <- insightApi
      .meanRating(u, List(standardFilter))
      .monSuccess(_.tutor.meanRating) orFailWith InsufficientGames
    user = TutorUser(u, meanRating)
    openings <- TutorOpening compute user
    phases   <- TutorPhases compute user
  } yield TutorReport(
    userId,
    DateTime.now,
    openings,
    phases
  )

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

  def answers[Dim](question: Question[Dim], user: TutorUser)(implicit
      insightApi: InsightApi,
      ec: ExecutionContext
  ) = for {
    mine <- insightApi
      .ask(question, user.user, withPovs = false)
      .monSuccess(_.tutor askMine question.monKey) map Answer.apply
    peer <- insightApi
      .askPeers(question, user.rating)
      .monSuccess(_.tutor askPeer question.monKey) map Answer.apply
  } yield Answers(mine, peer)

  case class Answer[Dim](answer: InsightAnswer[Dim]) {

    val list: List[(Dim, Value, Count)] =
      answer.clusters.view.collect { case Cluster(dimension, Insight.Single(point), nbGames, _) =>
        (dimension, point.y, nbGames)
      }.toList

    lazy val map: Map[Dim, Pair] = list.view.map { case (dim, value, count) =>
      dim -> (value, count)
    }.toMap

    def get = map.get _

    def dimensions = list.map(_._1)

    // lazy val average =
    //   list.foldLeft((0d, 0)) { case ((sum, count), (_, y, nb)) =>
    //     (sum + y * nb, count + nb)
    //   } match { case (sum, count) => sum / count }

    lazy val totalCount = list.map(_._3).sum

    def countRatio(count: Count) = TutorRatio(count, totalCount)

    def alignedQuestion = answer.question add Filter(answer.question.dimension, dimensions)
  }

  case class Answers[Dim](mine: Answer[Dim], peer: Answer[Dim]) {

    def countMetric(dim: Dim, myCount: Count) = TutorMetric(
      mine countRatio myCount,
      peer.get(dim).map(_._2).map(peer.countRatio)
    )

    def valueMetric(dim: Dim, myValue: Value) = TutorMetric(myValue, peer.get(dim).map(_._1))

    def valueMetric(dim: Dim) = TutorMetricOption(mine.get(dim).map(_._1), peer.get(dim).map(_._1))
  }

  val standardFilter            = Filter(InsightDimension.Perf, PerfType.standard)
  def colorFilter(color: Color) = Filter(InsightDimension.Color, List(color))

  case object InsufficientGames extends lila.base.LilaException {
    val message = "Not enough games to analyse. Play some rated games!"
  }
}
