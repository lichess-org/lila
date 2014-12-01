package lila.evaluation

import scala.util.{ Try, Success, Failure }

import akka.actor.ActorSelection
import akka.pattern.ask
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Json
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.bson._

import lila.db.api._
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.JsTube.Helpers.{ rename, writeDate, readDate }
import lila.db.Types._
import lila.rating.{ Perf, PerfType }
import lila.user.{ User, UserRepo, Perfs }

final class Evaluator(
    coll: Coll,
    execPath: String,
    reporter: ActorSelection,
    analyser: ActorSelection,
    marker: ActorSelection,
    token: String,
    apiUrl: String) {

  import Evaluation._, heuristics._

  def find(user: User): Fu[Option[Evaluation]] =
    coll.find(BSONDocument("_id" -> user.id)).one[JsObject] map { _ map readEvaluation }

  def evaluatedAt(user: User): Fu[Option[DateTime]] =
    coll.find(
      BSONDocument("_id" -> user.id),
      BSONDocument("date" -> true)
    ).one[BSONDocument] map { _ flatMap (_.getAs[DateTime]("date")) }

  def generate(userId: String, deep: Boolean): Fu[Option[Evaluation]] =
    UserRepo byId userId flatMap {
      _ ?? { user =>
        (run(userId, deep) match {
          case Failure(e: Exception) if e.getMessage.contains("exit value: 1") => fuccess(none)
          case Failure(e: Exception) if e.getMessage.contains("exit value: 2") => fuccess(none)
          case Failure(e: Exception) => fufail(e)
          case Success(output) => for {
            evalJs ← (Json parse output).transform(evaluationTransformer) match {
              case JsSuccess(v, _) => fuccess(v)
              case JsError(e)      => fufail(lila.common.LilaException(s"Can't parse evaluator output: $e on $output"))
            }
            eval = readEvaluation(evalJs)
            _ ← coll.update(Json.obj("_id" -> userId), evalJs, upsert = true)
          } yield eval.some
        }) andThen {
          case Success(Some(eval)) if Evaluation.watchPerfs(user.perfs) exists eval.mark =>
            UserRepo byId userId foreach {
              _ filterNot (_.engine) foreach { user =>
                marker ! lila.hub.actorApi.mod.MarkCheater(user.id)
                reporter ! lila.hub.actorApi.report.Check(user.id)
              }
            }
          case Failure(e) => logger.warn(s"generate: $e")
        }
      }
    }

  private[evaluation] def autoGenerate(
    user: User,
    perfType: PerfType,
    important: Boolean,
    forceRefresh: Boolean,
    suspiciousHold: Boolean) {
    val perf = user.perfs(perfType)
    if (!user.engine && (
      important || suspiciousHold ||
      (deviationIsLow(perf) && (progressIsHigh(perf) || ratingIsHigh(perf)))
    )) {
      evaluatedAt(user) foreach { date =>
        def freshness = if (progressIsVeryHigh(perf)) DateTime.now minusMinutes 20
        else if (progressIsHigh(perf)) DateTime.now minusHours 1
        else DateTime.now minusDays 2
        if (suspiciousHold || forceRefresh || date.fold(true)(_ isBefore freshness)) {
          generate(user.id, true) foreach {
            _ foreach { eval =>
              eval.gameIdsToAnalyse foreach { gameId =>
                analyser ! lila.hub.actorApi.ai.AutoAnalyse(gameId)
                if (eval report perf)
                  reporter ! lila.hub.actorApi.report.Cheater(user.id, eval reportText 3)
              }
            }
          }
        }
      }
    }
  }
  private[evaluation] def autoGenerate(
    user: User,
    important: Boolean,
    forceRefresh: Boolean,
    suspiciousHold: Boolean) {
    user.perfs.bestPerf foreach {
      case (pt, _) => autoGenerate(user, pt, important, forceRefresh, suspiciousHold)
    }
  }
  private[evaluation] def autoGenerate(userId: String, important: Boolean, forceRefresh: Boolean) {
    UserRepo byId userId foreach {
      _ foreach { autoGenerate(_, important, forceRefresh, false) }
    }
  }

  private def readEvaluation(js: JsValue): Evaluation =
    (readDate('date) andThen Evaluation.reader) reads js match {
      case JsSuccess(v, _) => v
      case JsError(e)      => throw lila.common.LilaException(s"Can't parse evaluator json: $e on $js")
    }

  private def run(userId: String, deep: Boolean): Try[String] = {
    import scala.sys.process._
    import java.io.File
    val exec = Process(Seq("php", "engine-evaluator.php", userId, deep.fold("true", "false"), token, s"$apiUrl/"), new File(execPath))
    Try {
      exec.!!
    } match {
      case Failure(e) => Failure(new Exception(s"$exec $e"))
      case x          => x
    }
  }

  private def evaluationTransformer =
    rename('userId, '_id) andThen
      rename('cheatIndex, 'shallow) andThen
      rename('deepIndex, 'deep) andThen
      rename('computerAnalysis, 'analysis) andThen
      rename('knownEngineIP, 'sharedIP) andThen
      __.json.update(
        __.read[JsObject].map { o => o ++ Json.obj("date" -> $date(DateTime.now)) }
      ) andThen
        (__ \ 'Error).json.prune

  private val logger = play.api.Logger("evaluator")
}
