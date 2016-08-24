package lila.puzzle

import org.joda.time.DateTime

case class Vote(
    _id: String, // userId/puzzleId
    vote: Boolean) {

  def id = _id
}

object Vote {

  def makeId(puzzleId: PuzzleId, userId: String) = s"$puzzleId/$userId"

  object BSONFields {
    val id = "_id"
    val vote = "v"
  }

  import reactivemongo.bson._
  import lila.db.BSON
  
  implicit val voteBSONHandler = Macros.handler[Vote]
}
