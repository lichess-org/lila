package lila.problem

import scala.util.{ Try, Success, Failure }

import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import reactivemongo.bson.BSONDocument

import lila.db.Types.Coll
import lila.user.{ User, UserRepo }

private[problem] final class ProblemApi(
    coll: Coll,
    apiToken: String) {

  import Problem.problemBSONHandler

  def find(id: ProblemId): Fu[Option[Problem]] =
    coll.find(BSONDocument("_id" -> id)).one[Problem]

  def latest(nb: Int): Fu[List[Problem]] =
    coll.find(BSONDocument())
      .sort(BSONDocument("date" -> -1))
      .cursor[Problem]
      .collect[List](nb)

  def importBatch(json: JsValue, token: String): Try[Funit] =
    if (token != apiToken) Failure(new Exception("Invalid API token"))
    else {
      import Generated.generatedJSONRead
      for {
        gens ← Try(json.as[List[Generated]])
        problems ← gens map (_.toProblem) sequence
      } yield coll bulkInsert Enumerator.enumerate(problems) void
    }
}
