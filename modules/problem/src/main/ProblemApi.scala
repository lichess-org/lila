package lila.problem

import reactivemongo.bson.BSONDocument

import lila.db.Types.Coll
import lila.user.{ User, UserRepo }

private[problem] final class ProblemApi(coll: Coll) {

  import Problem.problemBSONHandler

  def find(id: ProblemId): Fu[Option[Problem]] =
    coll.find(BSONDocument("_id" -> id)).one[Problem]

  def write(problem: Problem): Funit = coll insert problem void
}
