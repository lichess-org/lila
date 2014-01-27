package lila.evaluation

import scala.util.{ Try, Success, Failure }

import akka.actor.ActorSelection
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Json
import play.modules.reactivemongo.json.ImplicitBSONHandlers._

import lila.db.api._
import lila.db.JsTube.Helpers.{ rename, writeDate, readDate }
import lila.db.Types._
import lila.game.Player
import lila.user.{ User, UserRepo, Perfs }

final class Evaluator(
    coll: Coll,
    script: String,
    reporter: ActorSelection,
    marker: ActorSelection) {

  import Evaluation._

  def findOrGenerate(user: User, deep: Boolean): Fu[Option[Evaluation]] = find(user) flatMap {
    case x@Some(eval) if (!deep || eval.isDeep) ⇒ fuccess(x)
    case _                                      ⇒ generate(user, deep)
  }

  def find(user: User): Fu[Option[Evaluation]] =
    coll.find(Json.obj("_id" -> user.id)).one[JsObject] map { _ map readEvaluation }

  def generate(user: User, deep: Boolean): Fu[Option[Evaluation]] = generate(user.id, user.perfs, deep)

  def generate(userId: String, perfs: Perfs, deep: Boolean): Fu[Option[Evaluation]] = {
    run(userId, deep) match {
      case Failure(e: Exception) if e.getMessage.contains("exit value: 1") ⇒ fuccess(none)
      case Failure(e: Exception) if e.getMessage.contains("exit value: 2") ⇒ fuccess(none)
      case Failure(e: Exception) ⇒ fufail(e)
      case Success(output) ⇒ for {
        evalJs ← (Json parse output).transform(evaluationTransformer) match {
          case JsSuccess(v, _) ⇒ fuccess(v)
          case JsError(e)      ⇒ fufail(lila.common.LilaException(s"Can't parse evaluator output: $e on $output"))
        }
        eval ← scala.concurrent.Future(readEvaluation(evalJs))
        _ ← coll.update(Json.obj("_id" -> userId), evalJs, upsert = true)
        _ ← UserRepo.setEvaluated(userId, true)
      } yield eval.some
    }
  } andThen {
    case Success(Some(eval)) if eval.mark(perfs) ⇒ {
      marker ! lila.hub.actorApi.mod.MarkCheater(userId)
      reporter ! lila.hub.actorApi.report.Check(userId)
    }
    case Failure(e) ⇒ logger.warn(s"generate: $e")
  }

  def autoGenerate(user: User, player: Player) {
    logger.info(s"auto evaluate $user")
    UserRepo isEvaluated user.id foreach { evaluated ⇒
      if (!evaluated && deviationIsLow(user.perfs) && ratingIsHigh(user.perfs))
        generate(user.id, user.perfs, false) foreach {
          case Some(eval) if eval.report(user.perfs) ⇒ reporter ! lila.hub.actorApi.report.Cheater(user.id, eval reportText 3)
          case _                                     ⇒
        }
    }
  }

  private def readEvaluation(js: JsValue): Evaluation =
    (readDate('date) andThen Evaluation.reader) reads js match {
      case JsSuccess(v, _) ⇒ v
      case JsError(e)      ⇒ throw lila.common.LilaException(s"Can't parse evaluator json: $e on $js")
    }

  private def run(userId: String, deep: Boolean): Try[String] = {
    val command = s"""$script $userId ${deep ?? "true"}"""
    Try {
      import scala.sys.process._
      command!!
    } match {
      case Failure(e) ⇒ Failure(new Exception(s"$command $e"))
      case x          ⇒ x
    }
  }

  private def evaluationTransformer =
    rename('userId, '_id) andThen
      rename('cheatIndex, 'shallow) andThen
      rename('deepIndex, 'deep) andThen
      rename('computerAnalysis, 'analysis) andThen
      rename('knownEngineIP, 'sharedIP) andThen
      __.json.update(
        __.read[JsObject].map { o ⇒ o ++ Json.obj("date" -> $date(DateTime.now)) }
      ) andThen
        (__ \ 'Error).json.prune

  private val logger = play.api.Logger("Evaluator")
}
