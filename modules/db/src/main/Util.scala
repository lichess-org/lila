package lila.db

import dsl._

object Util {

  def findNextId(coll: Coll): Fu[Int] =
    coll.find($empty, Some($id(true))).sort($sort desc "_id").one[Bdoc] map {
      _ flatMap { doc => doc.getAs[Int]("_id") map (1+) } getOrElse 1
    }
}
