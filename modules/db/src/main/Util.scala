package lila.db

import dsl._

object Util {

  def findNextId(coll: Coll)(implicit ec: scala.concurrent.ExecutionContext): Fu[Int] =
    coll
      .find($empty, $id(true).some)
      .sort($sort desc "_id")
      .one[Bdoc] dmap {
      _ flatMap { doc =>
        doc.getAsOpt[Int]("_id") map (1 +)
      } getOrElse 1
    }
}
