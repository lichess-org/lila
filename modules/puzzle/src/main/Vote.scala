package lila.puzzle

case class Vote(
    _id: String, // puzzleId/userId
    v: Boolean
) {

  def id = _id

  def value = v
}

object Vote {

  def makeId(puzzleId: PuzzleId, userId: String) = s"$puzzleId/$userId"

  implicit val voteBSONHandler = reactivemongo.api.bson.Macros.handler[Vote]
}
