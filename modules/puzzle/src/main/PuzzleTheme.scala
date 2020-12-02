package lila.puzzle

import lila.i18n.I18nKeys.{ puzzleTheme => i }
import lila.i18n.{ I18nKey, I18nKeys => trans }

case class PuzzleTheme(key: PuzzleTheme.Key, name: I18nKey, description: I18nKey)

object PuzzleTheme {

  case class Key(value: String) extends AnyVal with StringValue

  case class WithCount(theme: PuzzleTheme, count: Int)

  val any           = PuzzleTheme(Key("any"), i.healthyMix, i.healthyMixDescription)
  val advancedPawn  = PuzzleTheme(Key("advancedPawn"), i.advancedPawn, i.advancedPawnDescription)
  val attackingF2F7 = PuzzleTheme(Key("attackingF2F7"), i.attackingF2F7, i.attackingF2F7Description)
  val attraction    = PuzzleTheme(Key("attraction"), i.attraction, i.attractionDescription)
  val bishopEndgame = PuzzleTheme(Key("bishopEndgame"), i.bishopEndgame, i.bishopEndgameDescription)
  val blocking      = PuzzleTheme(Key("blocking"), i.blocking, i.blockingDescription)
  val capturingDefender =
    PuzzleTheme(Key("capturingDefender"), i.capturingDefender, i.capturingDefenderDescription)
  val clearance     = PuzzleTheme(Key("clearance"), i.clearance, i.clearanceDescription)
  val coercion      = PuzzleTheme(Key("coercion"), i.coercion, i.coercionDescription)
  val defensiveMove = PuzzleTheme(Key("defensiveMove"), i.defensiveMove, i.defensiveMoveDescription)
  val deflection    = PuzzleTheme(Key("deflection"), i.deflection, i.deflectionDescription)
  val discoveredAttack =
    PuzzleTheme(Key("discoveredAttack"), i.discoveredAttack, i.discoveredAttackDescription)
  val doubleCheck    = PuzzleTheme(Key("doubleCheck"), i.doubleCheck, i.doubleCheckDescription)
  val enPassant      = PuzzleTheme(Key("enPassant"), i.enPassant, i.enPassantDescription)
  val exposedKing    = PuzzleTheme(Key("exposedKing"), i.exposedKing, i.exposedKingDescription)
  val fork           = PuzzleTheme(Key("fork"), i.fork, i.forkDescription)
  val hangingPiece   = PuzzleTheme(Key("hangingPiece"), i.hangingPiece, i.hangingPieceDescription)
  val interference   = PuzzleTheme(Key("interference"), i.interference, i.interferenceDescription)
  val long           = PuzzleTheme(Key("long"), i.long, i.longDescription)
  val mateIn1        = PuzzleTheme(Key("mateIn1"), i.mateIn1, i.mateIn1Description)
  val mateIn2        = PuzzleTheme(Key("mateIn2"), i.mateIn2, i.mateIn2Description)
  val mateIn3        = PuzzleTheme(Key("mateIn3"), i.mateIn3, i.mateIn3Description)
  val mateIn4        = PuzzleTheme(Key("mateIn4"), i.mateIn4, i.mateIn4Description)
  val mateIn5        = PuzzleTheme(Key("mateIn5"), i.mateIn5, i.mateIn5Description)
  val oneMove        = PuzzleTheme(Key("oneMove"), i.oneMove, i.oneMoveDescription)
  val overloading    = PuzzleTheme(Key("overloading"), i.overloading, i.overloadingDescription)
  val pawnEndgame    = PuzzleTheme(Key("pawnEndgame"), i.pawnEndgame, i.pawnEndgameDescription)
  val pin            = PuzzleTheme(Key("pin"), i.pin, i.pinDescription)
  val promotion      = PuzzleTheme(Key("promotion"), i.promotion, i.promotionDescription)
  val quietMove      = PuzzleTheme(Key("quietMove"), i.quietMove, i.quietMoveDescription)
  val rookEndgame    = PuzzleTheme(Key("rookEndgame"), i.rookEndgame, i.rookEndgameDescription)
  val sacrifice      = PuzzleTheme(Key("sacrifice"), i.sacrifice, i.sacrificeDescription)
  val short          = PuzzleTheme(Key("short"), i.short, i.shortDescription)
  val simplification = PuzzleTheme(Key("simplification"), i.simplification, i.simplificationDescription)
  val skewer         = PuzzleTheme(Key("skewer"), i.skewer, i.skewerDescription)
  val trappedPiece   = PuzzleTheme(Key("trappedPiece"), i.trappedPiece, i.trappedPieceDescription)
  val veryLong       = PuzzleTheme(Key("veryLong"), i.veryLong, i.veryLongDescription)
  val zugzwang       = PuzzleTheme(Key("zugzwang"), i.zugzwang, i.zugzwangDescription)

  val categorized = List[(I18nKey, List[PuzzleTheme])](
    trans.puzzle.recommended -> List(
      any
    ),
    trans.puzzle.motifs -> List(
      advancedPawn,
      attackingF2F7,
      attraction,
      bishopEndgame,
      blocking,
      capturingDefender,
      clearance,
      coercion,
      defensiveMove,
      deflection,
      discoveredAttack,
      doubleCheck,
      enPassant,
      exposedKing,
      fork,
      hangingPiece,
      interference,
      overloading,
      pawnEndgame,
      pin,
      promotion,
      quietMove,
      rookEndgame,
      sacrifice,
      simplification,
      skewer,
      trappedPiece,
      zugzwang
    ),
    trans.puzzle.mates -> List(
      mateIn1,
      mateIn2,
      mateIn3,
      mateIn4,
      mateIn5
    ),
    trans.puzzle.lengths -> List(
      oneMove,
      short,
      long,
      veryLong
    )
  )

  lazy val all: List[PuzzleTheme] = categorized.flatMap(_._2)

  lazy val allTranslationKeys = all.flatMap { t =>
    List(t.name, t.description)
  }

  lazy val byKey: Map[Key, PuzzleTheme] = all.view.map { t =>
    t.key -> t
  }.toMap

  def find(key: String) = byKey get Key(key)

  def findOrAny(key: String) = find(key) | any

  implicit val keyIso = lila.common.Iso.string[Key](Key.apply, _.value)
}
