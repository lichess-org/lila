package lila.db

import dsl._

object Util {

  def findNextId(coll: Coll): Fu[Int] =
    coll.find($empty, $id(true).some)
      .sort($sort desc "_id")
      .uno[Bdoc] map {
        _ flatMap { doc => doc.getAsOpt[Int]("_id") map (1+) } getOrElse 1
      }
}
