package lila.puzzle

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("puzzle")

opaque type PuzzleWin = Boolean
object PuzzleWin extends YesNo[PuzzleWin]
