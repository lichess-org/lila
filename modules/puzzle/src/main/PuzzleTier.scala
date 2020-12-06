package lila.puzzle

sealed abstract private class PuzzleTier(val key: String) {

  def stepDown = PuzzleTier stepDown this

  override def toString = key
}

private object PuzzleTier {

  case object Top  extends PuzzleTier("top")
  case object Good extends PuzzleTier("good")
  case object All  extends PuzzleTier("all")

  def stepDown(tier: PuzzleTier): Option[PuzzleTier] =
    if (tier == Top) Good.some
    else if (tier == Good) All.some
    else none

  def from(tier: String) =
    if (tier == Top.key) Top
    else if (tier == Good.key) Good
    else All
}
