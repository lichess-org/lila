package lila.puzzle

import org.joda.time.DateTime

case class Vote(
    _id: String, // puzzleId/userId
    vote: Boolean) {

  def id = _id
}

object Vote {

  def makeId(puzzleId: PuzzleId, userId: String) = s"$puzzleId/$userId"

  implicit val voteBSONHandler = reactivemongo.bson.Macros.handler[Vote]
}
