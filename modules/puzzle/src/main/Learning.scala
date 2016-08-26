package lila.puzzle

case class Learning(
  _id: String, // userId
  stackA: List[PuzzleId],
  stackB: List[PuzzleId]) { // puzzleIds being learnt

  def id = _id

  def nextPuzzleId = (stackA.lastOption, stackB.lastOption, scala.util.Random.nextInt(5)) match {
    case (Some(a), _, r) if r != 0 => a.some
    case (_, Some(b), r) if r == 0 => b.some
    case (Some(a), _, _)           => a.some
    case _                         => none
  }

  def failed(puzzleId: PuzzleId): Learning = copy(
    stackA = puzzleId :: stackA.filter(_ != puzzleId).take(50),
    stackB = stackB.filter(_ != puzzleId).take(50))

  def solved(puzzleId: PuzzleId): Learning = (stackA.contains(puzzleId), stackB.contains(puzzleId)) match {
    case (true, _) => copy(stackA = stackA.filter(_ != puzzleId), stackB = puzzleId :: stackB)
    case (_, true) => copy(stackB = stackB.filter(_ != puzzleId).take(50))
    case _         => this
  }
}

object Learning {

  import reactivemongo.bson._
  import lila.db.BSON

  implicit val learningBSONHandler = Macros.handler[Learning]
}
