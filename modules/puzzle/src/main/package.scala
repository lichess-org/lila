package lila.puzzle

import lila.rating.Glicko

export lila.Lila.{ *, given }

private val logger = lila.log("puzzle")

opaque type PuzzleWin = Boolean
object PuzzleWin extends YesNo[PuzzleWin]
