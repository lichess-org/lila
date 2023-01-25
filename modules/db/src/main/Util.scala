package lila.db

import dsl.*

object Util:

  def findNextId(coll: Coll)(using Executor): Fu[Int] =
    coll
      .find($empty, $id(true).some)
      .sort($sort desc "_id")
      .one[Bdoc] dmap {
      _ flatMap { doc =>
        doc.getAsOpt[Int]("_id") map (1 +)
      } getOrElse 1
    }
