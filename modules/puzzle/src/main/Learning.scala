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
    case (_, Some(b), _)           => b.some
    case _                         => none
  }

  def failed(puzzleId: PuzzleId): Learning = copy(
    stackA = (puzzleId :: stackA.filter(puzzleId !=)) take 50,
    stackB = stackB.filter(puzzleId !=))

  def solved(puzzleId: PuzzleId): Learning =
    if (stackA contains puzzleId) copy(
      stackA = stackA.filter(puzzleId !=),
      stackB = (puzzleId :: stackB) take 50)
    else if (stackB contains puzzleId)
      copy(stackB = stackB.filter(puzzleId !=))
    else this

  def contains(puzzleId: PuzzleId) =
    stackA.contains(puzzleId) || stackB.contains(puzzleId)
}
