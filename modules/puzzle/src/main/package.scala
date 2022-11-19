package lila.puzzle

import lila.rating.Glicko

export lila.Lila.{ *, given }

private def logger = lila.log("puzzle")

case class PuzzleResult(win: Boolean) extends AnyVal:
  def loss = !win
