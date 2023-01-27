package lila.puzzle

enum PuzzleTier:
  def key      = toString
  def stepDown = PuzzleTier stepDown this
  case top, good, all

object PuzzleTier:

  def stepDown(tier: PuzzleTier): Option[PuzzleTier] =
    if (tier == top) good.some
    else if (tier == good) all.some
    else none

  def from(tier: String) =
    if (tier == top.toString) top
    else if (tier == good.toString) good
    else all
