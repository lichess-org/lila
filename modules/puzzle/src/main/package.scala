package lila.puzzle

export lila.Lila.{ *, given }

private val logger = lila.log("puzzle")

opaque type PuzzleWin = Boolean
object PuzzleWin extends YesNo[PuzzleWin]
