package lila.puzzle

import lila.core.i18n.I18nKey

enum PuzzleDifficulty(val ratingDelta: Int, val name: I18nKey):
  lazy val key = toString.toLowerCase
  case Easiest extends PuzzleDifficulty(-600, I18nKey.puzzle.easiest)
  case Easier extends PuzzleDifficulty(-300, I18nKey.puzzle.easier)
  case Normal extends PuzzleDifficulty(0, I18nKey.puzzle.normal)
  case Harder extends PuzzleDifficulty(300, I18nKey.puzzle.harder)
  case Hardest extends PuzzleDifficulty(600, I18nKey.puzzle.hardest)

object PuzzleDifficulty:
  val all = values.toList
  val default: PuzzleDifficulty = Normal
  def isExtreme(d: PuzzleDifficulty) = d == Easiest || d == Hardest
  def find(str: String) = all.find(_.key == str)
  def orDefault(str: String) = find(str) | default

  import play.api.mvc.RequestHeader
  def fromReqSession(req: RequestHeader): Option[PuzzleDifficulty] =
    req.session.get(difficultyCookie).flatMap(PuzzleDifficulty.find)
