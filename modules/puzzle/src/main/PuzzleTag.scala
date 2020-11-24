package lila.puzzle

import lila.i18n.I18nKey
import lila.i18n.I18nKeys.{ puzzleTheme => i }

case class PuzzleTag(key: String, trans: I18nKey)

object PuzzleTag {

  val sorted: List[PuzzleTag] = List(
    PuzzleTag("advancedPawn", i.advancedPawn),
    PuzzleTag("attackingF2F7", i.attackingF2F7),
    PuzzleTag("attraction", i.attraction),
    PuzzleTag("blocking", i.blocking),
    PuzzleTag("capturingDefender", i.capturingDefender),
    PuzzleTag("clearance", i.clearance),
    PuzzleTag("coercion", i.coercion),
    PuzzleTag("defensiveMove", i.defensiveMove),
    PuzzleTag("deflection", i.deflection),
    PuzzleTag("discoveredAttack", i.discoveredAttack),
    PuzzleTag("doubleCheck", i.doubleCheck),
    PuzzleTag("enPassant", i.enPassant),
    PuzzleTag("exposedKing", i.exposedKing),
    PuzzleTag("fork", i.fork),
    PuzzleTag("hangingPiece", i.hangingPiece),
    PuzzleTag("interference", i.interference),
    PuzzleTag("long", i.long),
    PuzzleTag("mateIn1", i.mateIn1),
    PuzzleTag("mateIn2", i.mateIn2),
    PuzzleTag("mateIn3", i.mateIn3),
    PuzzleTag("mateIn4", i.mateIn4),
    PuzzleTag("mateIn5", i.mateIn5),
    PuzzleTag("oneMove", i.oneMove),
    PuzzleTag("overloading", i.overloading),
    PuzzleTag("pin", i.pin),
    PuzzleTag("promotion", i.promotion),
    PuzzleTag("quietMove", i.quietMove),
    PuzzleTag("sacrifice", i.sacrifice),
    PuzzleTag("short", i.short),
    PuzzleTag("simplification", i.simplification),
    PuzzleTag("skewer", i.skewer),
    PuzzleTag("trappedPiece", i.trappedPiece),
    PuzzleTag("veryLong", i.veryLong),
    PuzzleTag("zugzwang", i.zugzwang)
  )

  val byKey: Map[String, PuzzleTag] = sorted.view.map { t =>
    t.key -> t
  }.toMap
}
