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
import lila.user.{ User, UserRepo, Perfs }

final class Evaluator(
    coll: Coll,
    script: String,
    reporter: ActorSelection,
    analyser: ActorSelection,
    marker: ActorSelection) {

  import Evaluation._

  def findOrGenerate(user: User, deep: Boolean): Fu[Option[Evaluation]] = find(user) flatMap {
    case x@Some(eval) if (!deep || eval.isDeep) => fuccess(x)
    case _                                      => generate(user, deep)
  }

  def find(user: User): Fu[Option[Evaluation]] =
    coll.find(BSONDocument("_id" -> user.id)).one[JsObject] map { _ map readEvaluation }

  def evaluatedAt(user: User): Fu[Option[DateTime]] =
    coll.find(
      BSONDocument("_id" -> user.id),
      BSONDocument("date" -> true)
    ).one[BSONDocument] map { _ flatMap (_.getAs[DateTime]("date")) }

  def generate(user: User, deep: Boolean): Fu[Option[Evaluation]] = generate(user.id, user.perfs, deep)

  def generate(userId: String, perfs: Perfs, deep: Boolean): Fu[Option[Evaluation]] = {
    run(userId, deep) match {
      case Failure(e: Exception) if e.getMessage.contains("exit value: 1") => fuccess(none)
      case Failure(e: Exception) if e.getMessage.contains("exit value: 2") => fuccess(none)
      case Failure(e: Exception) => fufail(e)
      case Success(output) => for {
        evalJs ← (Json parse output).transform(evaluationTransformer) match {
          case JsSuccess(v, _) => fuccess(v)
          case JsError(e)      => fufail(lila.common.LilaException(s"Can't parse evaluator output: $e on $output"))
        }
        eval ← scala.concurrent.Future(readEvaluation(evalJs))
        _ ← coll.update(Json.obj("_id" -> userId), evalJs, upsert = true)
      } yield eval.some
    }
  } andThen {
    case Success(Some(eval)) if eval.mark(perfs) => UserRepo byId userId foreach {
      _ filterNot (_.engine) foreach { user =>
        marker ! lila.hub.actorApi.mod.MarkCheater(user.id)
        reporter ! lila.hub.actorApi.report.Check(user.id)
      }
    }
    case Failure(e) => logger.warn(s"generate: $e")
  }

  private[evaluation] def autoGenerate(user: User, important: Boolean, forceRefresh: Boolean) {
    if (!user.engine && (
      important ||
      (deviationIsLow(user.perfs) && (progressIsHigh(user) || ratingIsHigh(user.perfs)))
    )) {
      evaluatedAt(user) foreach { date =>
        def freshness = if (progressIsVeryHigh(user)) DateTime.now minusMinutes 30
        else if (progressIsHigh(user)) DateTime.now minusHours 1
        else DateTime.now minusDays 3
        if (forceRefresh || date.fold(true)(_ isBefore freshness)) {
          logger.info(s"auto evaluate $user")
          generate(user.id, user.perfs, true) foreach {
            _ foreach { eval =>
              eval.gameIdsToAnalyse foreach { gameId =>
                implicit val tm = makeTimeout minutes 120
                analyser ! lila.hub.actorApi.ai.AutoAnalyse(gameId)
                if (eval report user.perfs)
                  reporter ! lila.hub.actorApi.report.Cheater(user.id, eval reportText 3)
              }
            }
          }
        }
      }
    }
  }
  private[evaluation] def autoGenerate(userId: String, important: Boolean, forceRefresh: Boolean) {
    UserRepo byId userId foreach {
      _ foreach { autoGenerate(_, important, forceRefresh) }
    }
  }

  private def readEvaluation(js: JsValue): Evaluation =
    (readDate('date) andThen Evaluation.reader) reads js match {
      case JsSuccess(v, _) => v
      case JsError(e)      => throw lila.common.LilaException(s"Can't parse evaluator json: $e on $js")
    }

  private def run(userId: String, deep: Boolean): Try[String] = {
    val command = s"""$script $userId ${deep ?? "true"}"""
    Try {
      import scala.sys.process._
      command!!
    } match {
      case Failure(e) => Failure(new Exception(s"$command $e"))
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
