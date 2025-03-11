package lila.puzzle

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*
export lila.core.id.PuzzleId

private val logger = lila.log("puzzle")

opaque type PuzzleWin = Boolean
object PuzzleWin extends YesNo[PuzzleWin]

val difficultyCookie = "puz-diff"
