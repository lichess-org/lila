package lila.puzzle

import lila.i18n.{ I18nKey, I18nKeys as trans }

enum PuzzleDifficulty(val ratingDelta: Int, val name: I18nKey):
  lazy val key = toString.toLowerCase
  case Easiest extends PuzzleDifficulty(-600, trans.puzzle.easiest)
  case Easier  extends PuzzleDifficulty(-300, trans.puzzle.easier)
  case Normal  extends PuzzleDifficulty(0, trans.puzzle.normal)
  case Harder  extends PuzzleDifficulty(300, trans.puzzle.harder)
  case Hardest extends PuzzleDifficulty(600, trans.puzzle.hardest)

object PuzzleDifficulty:
  val all                            = values.toList
  val default: PuzzleDifficulty      = Normal
  def isExtreme(d: PuzzleDifficulty) = d == Easiest || d == Hardest
  def find(str: String)              = all.find(_.key == str)
  def orDefault(str: String)         = find(str) | default
