package lila.coordinate

import lila.db.Types.Coll
import reactivemongo.bson._

final class CoordinateApi(scoreColl: Coll) {

  private implicit val scoreBSONHandler = Macros.handler[Score]

  def getScore(userId: String): Fu[Score] =
    scoreColl.find(BSONDocument("_id" -> userId)).one[Score] map (_ | Score(userId))

  def addScore(userId: String, white: Boolean, hits: Int): Funit =
    scoreColl.update(
      BSONDocument("_id" -> userId),
      BSONDocument("$push" -> BSONDocument(
         "white" -> BSONDocument(
          "$each" -> (white ?? List(BSONInteger(hits))),
          "$slice" -> -20),
         "black" -> BSONDocument(
          "$each" -> (!white ?? List(BSONInteger(hits))),
          "$slice" -> -20)
      )),
      upsert = true).void
}
