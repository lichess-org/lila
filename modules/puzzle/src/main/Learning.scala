package lila.puzzle

case class Learning(
  _id: String, // userId
  stack: List[PuzzleId]) { // puzzleIds being learnt

  def id = _id

  def nextPuzzleId = stack.lastOption

  def addPuzzle(puzzleId: PuzzleId): List[PuzzleId] = puzzleId :: stack.filter(_ != puzzleId).take(50)
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
