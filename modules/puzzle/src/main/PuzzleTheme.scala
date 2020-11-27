package lila.puzzle

import lila.i18n.I18nKey
import lila.i18n.I18nKeys.{ puzzleTheme => i }

case class PuzzleTheme(key: PuzzleTheme.Key, name: I18nKey, description: I18nKey)

object PuzzleTheme {

  case class Key(value: String) extends AnyVal with StringValue

  case class WithCount(theme: PuzzleTheme, count: Int)

  val anyKey = Key("any")

  val sorted: List[PuzzleTheme] = List(
    PuzzleTheme(Key("advancedPawn"), i.advancedPawn, i.advancedPawnDescription),
    PuzzleTheme(Key("attackingF2F7"), i.attackingF2F7, i.attackingF2F7Description),
    PuzzleTheme(Key("attraction"), i.attraction, i.attractionDescription),
    PuzzleTheme(Key("blocking"), i.blocking, i.blockingDescription),
    PuzzleTheme(Key("capturingDefender"), i.capturingDefender, i.capturingDefenderDescription),
    PuzzleTheme(Key("clearance"), i.clearance, i.clearanceDescription),
    PuzzleTheme(Key("coercion"), i.coercion, i.coercionDescription),
    PuzzleTheme(Key("defensiveMove"), i.defensiveMove, i.defensiveMoveDescription),
    PuzzleTheme(Key("deflection"), i.deflection, i.deflectionDescription),
    PuzzleTheme(Key("discoveredAttack"), i.discoveredAttack, i.discoveredAttackDescription),
    PuzzleTheme(Key("doubleCheck"), i.doubleCheck, i.doubleCheckDescription),
    PuzzleTheme(Key("enPassant"), i.enPassant, i.enPassantDescription),
    PuzzleTheme(Key("exposedKing"), i.exposedKing, i.exposedKingDescription),
    PuzzleTheme(Key("fork"), i.fork, i.forkDescription),
    PuzzleTheme(Key("hangingPiece"), i.hangingPiece, i.hangingPieceDescription),
    PuzzleTheme(Key("interference"), i.interference, i.interferenceDescription),
    PuzzleTheme(Key("long"), i.long, i.longDescription),
    PuzzleTheme(Key("mateIn1"), i.mateIn1, i.mateIn1Description),
    PuzzleTheme(Key("mateIn2"), i.mateIn2, i.mateIn2Description),
    PuzzleTheme(Key("mateIn3"), i.mateIn3, i.mateIn3Description),
    PuzzleTheme(Key("mateIn4"), i.mateIn4, i.mateIn4Description),
    PuzzleTheme(Key("mateIn5"), i.mateIn5, i.mateIn5Description),
    PuzzleTheme(Key("oneMove"), i.oneMove, i.oneMoveDescription),
    PuzzleTheme(Key("overloading"), i.overloading, i.overloadingDescription),
    PuzzleTheme(Key("pin"), i.pin, i.pinDescription),
    PuzzleTheme(Key("promotion"), i.promotion, i.promotionDescription),
    PuzzleTheme(Key("quietMove"), i.quietMove, i.quietMoveDescription),
    PuzzleTheme(Key("sacrifice"), i.sacrifice, i.sacrificeDescription),
    PuzzleTheme(Key("short"), i.short, i.shortDescription),
    PuzzleTheme(Key("simplification"), i.simplification, i.simplificationDescription),
    PuzzleTheme(Key("skewer"), i.skewer, i.skewerDescription),
    PuzzleTheme(Key("trappedPiece"), i.trappedPiece, i.trappedPieceDescription),
    PuzzleTheme(Key("veryLong"), i.veryLong, i.veryLongDescription),
    PuzzleTheme(Key("zugzwang"), i.zugzwang, i.zugzwangDescription)
  )

  val byKey: Map[Key, PuzzleTheme] = sorted.view.map { t =>
    t.key -> t
  }.toMap

  def find(key: String) = byKey get Key(key)
}
