package lila.db

import reactivemongo.bson._

import dsl.Coll

object Util {

  def findNextId(coll: Coll): Fu[Int] =
    coll.find(BSONDocument(), BSONDocument("_id" -> true))
      .sort(BSONDocument("_id" -> -1))
      .one[BSONDocument] map {
        _ flatMap { doc => doc.getAs[Int]("_id") map (1+) } getOrElse 1
      }
}
