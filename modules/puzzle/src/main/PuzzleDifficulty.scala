package lila.puzzle

import lila.i18n.{ I18nKey, I18nKeys => trans }

sealed abstract class PuzzleDifficulty(val ratingDelta: Int, val name: I18nKey) {

  lazy val key = toString.toLowerCase
}

object PuzzleDifficulty {
  case object Easiest extends PuzzleDifficulty(-600, trans.puzzle.easiest)
  case object Easier  extends PuzzleDifficulty(-300, trans.puzzle.easier)
  case object Normal  extends PuzzleDifficulty(0, trans.puzzle.normal)
  case object Harder  extends PuzzleDifficulty(300, trans.puzzle.harder)
  case object Hardest extends PuzzleDifficulty(600, trans.puzzle.hardest)

  val all     = List(Easiest, Easier, Normal, Harder, Hardest)
  val default = Normal

  def isExtreme(d: PuzzleDifficulty) = d == Easiest || d == Hardest

  def find(str: String) = all.find(_.key == str)
}
