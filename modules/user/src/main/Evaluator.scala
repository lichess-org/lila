package lila.user

import scala.util.Try

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Json
import play.modules.reactivemongo.json.ImplicitBSONHandlers._

import lila.db.api._
import lila.db.JsTube.Helpers.{ rename, writeDate, readDate }
import lila.db.Types._

private[user] final class Evaluator(coll: Coll, script: String) {

  def findOrGenerate(user: User, deep: Boolean): Fu[Option[Evaluation]] = find(user) flatMap {
    case x@Some(eval) if (!deep || eval.isDeep) ⇒ fuccess(x)
    case _                                      ⇒ generate(user, deep)
  }

  def find(user: User): Fu[Option[Evaluation]] =
    coll.find(Json.obj("_id" -> user.id)).one[JsObject] map { _ map readEvaluation }

  def generate(user: User, deep: Boolean): Fu[Option[Evaluation]] = UserRepo byId user.id flatMap {
    _ ?? { user ⇒
      for {
        output ← run(user.id, deep).future
        evalJs ← (Json parse output).transform(evaluationTransformer) match {
          case JsSuccess(v, _) ⇒ fuccess(v)
          case JsError(e)      ⇒ fufail(lila.common.LilaException(s"Can't parse evaluator output: $e on $output"))
        }
        eval ← scala.concurrent.Future(readEvaluation(evalJs))
        _ ← coll.update(Json.obj("_id" -> user.id), evalJs, upsert = true)
      } yield eval.some
    }
  }

  private def readEvaluation(js: JsValue): Evaluation =
    (readDate('date) andThen Evaluation.reader) reads js match {
      case JsSuccess(v, _) ⇒ v
      case JsError(e)      ⇒ throw lila.common.LilaException(s"Can't parse evaluator json: $e on $js")
    }

  private def run(userId: String, deep: Boolean): Try[String] = Try {
    import scala.sys.process._
    s"""$script $userId ${deep ?? "true"}"""!!
  }

  def evaluationTransformer =
    rename('userId, '_id) andThen
      rename('cheatIndex, 'shallow) andThen
      rename('deepIndex, 'deep) andThen
      rename('computerAnalysis, 'analysis) andThen
      rename('knownEngineIP, 'sharedIP) andThen
      __.json.update(
        __.read[JsObject].map { o ⇒ o ++ Json.obj("date" -> $date(DateTime.now)) }
      ) andThen
        (__ \ 'Error).json.prune
}
