package lila.problem

import org.joda.time.DateTime

case class Problem(
  id: ProblemId,
  gameId: String,
  date: DateTime)

object Problem {

  import reactivemongo.bson._
  import lila.db.BSON.BSONJodaDateTimeHandler

  implicit val problemBSONHandler = Macros.handler[Problem]
}
