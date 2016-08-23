package lila.puzzle

case class Learning(
  _id: String, // userId
  stack: List[PuzzleId]) { // puzzleIds being learnt

  def id = _id

  def nextPuzzleId = stack.lastOption

  def failed(puzzleId: PuzzleId): Learning = Learning(id, puzzleId :: stack.filter(_ != puzzleId).take(50))

  def solved(puzzleId: PuzzleId): Learning = Learning(id, stack.filter(_ != puzzleId))
}

object Learning {

  object BSONFields {
    val id = "_id"
    val stack = "stack"
  }

  import reactivemongo.bson._
  import lila.db.BSON

  implicit val learningBSONHandler = Macros.handler[Learning]
}
