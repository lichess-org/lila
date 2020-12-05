package lila.puzzle

sealed abstract private class PuzzleTier(val key: String) {

  override def toString = key
}

private object PuzzleTier {

  case object Top extends PuzzleTier("top")
  case object All extends PuzzleTier("all")

  def from(tier: String) = if (tier == Top.key) Top else All
}
