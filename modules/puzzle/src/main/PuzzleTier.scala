package lila.puzzle

enum PuzzleTier:
  def key      = toString
  def stepDown = PuzzleTier.stepDown(this)
  case top, good, all

object PuzzleTier:

  def stepDown(tier: PuzzleTier): Option[PuzzleTier] =
    if tier == top then good.some
    else if tier == good then all.some
    else none

  def from(tier: String) =
    if tier == top.toString then top
    else if tier == good.toString then good
    else all
