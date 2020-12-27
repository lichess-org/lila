package lila.puzzle

import lila.i18n.I18nKeys.{ puzzleTheme => i }
import lila.i18n.{ I18nKey, I18nKeys => trans }

case class PuzzleTheme(key: PuzzleTheme.Key, name: I18nKey, description: I18nKey)

object PuzzleTheme {

  case class Key(value: String) extends AnyVal with StringValue

  case class WithCount(theme: PuzzleTheme, count: Int)

  val mix           = PuzzleTheme(Key("mix"), i.healthyMix, i.healthyMixDescription)
  val advancedPawn  = PuzzleTheme(Key("advancedPawn"), i.advancedPawn, i.advancedPawnDescription)
  val advantage     = PuzzleTheme(Key("advantage"), i.advantage, i.advantageDescription)
  val attackingF2F7 = PuzzleTheme(Key("attackingF2F7"), i.attackingF2F7, i.attackingF2F7Description)
  val attraction    = PuzzleTheme(Key("attraction"), i.attraction, i.attractionDescription)
  val backRankMate  = PuzzleTheme(Key("backRankMate"), i.backRankMate, i.backRankMateDescription)
  val bishopEndgame = PuzzleTheme(Key("bishopEndgame"), i.bishopEndgame, i.bishopEndgameDescription)
  val capturingDefender =
    PuzzleTheme(Key("capturingDefender"), i.capturingDefender, i.capturingDefenderDescription)
  val castling      = PuzzleTheme(Key("castling"), i.castling, i.castlingDescription)
  val clearance     = PuzzleTheme(Key("clearance"), i.clearance, i.clearanceDescription)
  val crushing      = PuzzleTheme(Key("crushing"), i.crushing, i.crushingDescription)
  val defensiveMove = PuzzleTheme(Key("defensiveMove"), i.defensiveMove, i.defensiveMoveDescription)
  val deflection    = PuzzleTheme(Key("deflection"), i.deflection, i.deflectionDescription)
  val discoveredAttack =
    PuzzleTheme(Key("discoveredAttack"), i.discoveredAttack, i.discoveredAttackDescription)
  val doubleCheck    = PuzzleTheme(Key("doubleCheck"), i.doubleCheck, i.doubleCheckDescription)
  val equality       = PuzzleTheme(Key("equality"), i.equality, i.equalityDescription)
  val endgame        = PuzzleTheme(Key("endgame"), i.endgame, i.endgameDescription)
  val enPassant      = PuzzleTheme(Key("enPassant"), new I18nKey("En passant"), i.enPassantDescription)
  val exposedKing    = PuzzleTheme(Key("exposedKing"), i.exposedKing, i.exposedKingDescription)
  val fork           = PuzzleTheme(Key("fork"), i.fork, i.forkDescription)
  val hangingPiece   = PuzzleTheme(Key("hangingPiece"), i.hangingPiece, i.hangingPieceDescription)
  val interference   = PuzzleTheme(Key("interference"), i.interference, i.interferenceDescription)
  val intermezzo     = PuzzleTheme(Key("intermezzo"), i.intermezzo, i.intermezzoDescription)
  val kingsideAttack = PuzzleTheme(Key("kingsideAttack"), i.kingsideAttack, i.kingsideAttackDescription)
  val knightEndgame  = PuzzleTheme(Key("knightEndgame"), i.knightEndgame, i.knightEndgameDescription)
  val long           = PuzzleTheme(Key("long"), i.long, i.longDescription)
  val mate           = PuzzleTheme(Key("mate"), i.mate, i.mateDescription)
  val mateIn1        = PuzzleTheme(Key("mateIn1"), i.mateIn1, i.mateIn1Description)
  val mateIn2        = PuzzleTheme(Key("mateIn2"), i.mateIn2, i.mateIn2Description)
  val mateIn3        = PuzzleTheme(Key("mateIn3"), i.mateIn3, i.mateIn3Description)
  val mateIn4        = PuzzleTheme(Key("mateIn4"), i.mateIn4, i.mateIn4Description)
  val mateIn5        = PuzzleTheme(Key("mateIn5"), i.mateIn5, i.mateIn5Description)
  val smotheredMate  = PuzzleTheme(Key("smotheredMate"), i.smotheredMate, i.smotheredMateDescription)
  val middlegame     = PuzzleTheme(Key("middlegame"), i.middlegame, i.middlegameDescription)
  val oneMove        = PuzzleTheme(Key("oneMove"), i.oneMove, i.oneMoveDescription)
  val opening        = PuzzleTheme(Key("opening"), i.opening, i.openingDescription)
  val overloading    = PuzzleTheme(Key("overloading"), i.overloading, i.overloadingDescription)
  val pawnEndgame    = PuzzleTheme(Key("pawnEndgame"), i.pawnEndgame, i.pawnEndgameDescription)
  val pin            = PuzzleTheme(Key("pin"), i.pin, i.pinDescription)
  val promotion      = PuzzleTheme(Key("promotion"), i.promotion, i.promotionDescription)
  val queenEndgame   = PuzzleTheme(Key("queenEndgame"), i.queenEndgame, i.queenEndgameDescription)
  val queenRookEndgame =
    PuzzleTheme(Key("queenRookEndgame"), i.queenRookEndgame, i.queenRookEndgameDescription)
  val queensideAttack = PuzzleTheme(Key("queensideAttack"), i.queensideAttack, i.queensideAttackDescription)
  val quietMove       = PuzzleTheme(Key("quietMove"), i.quietMove, i.quietMoveDescription)
  val rookEndgame     = PuzzleTheme(Key("rookEndgame"), i.rookEndgame, i.rookEndgameDescription)
  val sacrifice       = PuzzleTheme(Key("sacrifice"), i.sacrifice, i.sacrificeDescription)
  val short           = PuzzleTheme(Key("short"), i.short, i.shortDescription)
  val skewer          = PuzzleTheme(Key("skewer"), i.skewer, i.skewerDescription)
  val trappedPiece    = PuzzleTheme(Key("trappedPiece"), i.trappedPiece, i.trappedPieceDescription)
  val underPromotion  = PuzzleTheme(Key("underPromotion"), i.underPromotion, i.underPromotionDescription)
  val veryLong        = PuzzleTheme(Key("veryLong"), i.veryLong, i.veryLongDescription)
  val xRayAttack      = PuzzleTheme(Key("xRayAttack"), i.xRayAttack, i.xRayAttackDescription)
  val zugzwang        = PuzzleTheme(Key("zugzwang"), i.zugzwang, i.zugzwangDescription)

  val categorized = List[(I18nKey, List[PuzzleTheme])](
    trans.puzzle.recommended -> List(
      mix
    ),
    trans.puzzle.phases -> List(
      opening,
      middlegame,
      endgame,
      rookEndgame,
      bishopEndgame,
      pawnEndgame,
      knightEndgame,
      queenEndgame,
      queenRookEndgame
    ),
    trans.puzzle.motifs -> List(
      advancedPawn,
      attackingF2F7,
      capturingDefender,
      discoveredAttack,
      doubleCheck,
      exposedKing,
      fork,
      hangingPiece,
      kingsideAttack,
      pin,
      queensideAttack,
      sacrifice,
      skewer,
      trappedPiece
    ),
    trans.puzzle.advanced -> List(
      attraction,
      clearance,
      defensiveMove,
      deflection,
      interference,
      intermezzo,
      overloading,
      quietMove,
      xRayAttack,
      zugzwang
    ),
    trans.puzzle.mates -> List(
      mateIn1,
      mateIn2,
      mateIn3,
      mateIn4,
      mateIn5,
      smotheredMate,
      backRankMate
    ),
    trans.puzzle.specialMoves -> List(
      castling,
      enPassant,
      promotion,
      underPromotion
    ),
    trans.puzzle.goals -> List(
      equality,
      advantage,
      crushing,
      mate
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

  private lazy val byLowerKey: Map[String, PuzzleTheme] = all.view.map { t =>
    t.key.value.toLowerCase -> t
  }.toMap

  // themes that can't be voted by players
  val staticThemes: Set[Key] = Set(
    advantage,
    castling,
    crushing,
    enPassant,
    endgame,
    equality,
    long,
    mate,
    mateIn1,
    mateIn2,
    mateIn3,
    mateIn4,
    mateIn5,
    middlegame,
    oneMove,
    opening,
    short,
    smotheredMate,
    veryLong
  ).map(_.key)

  val studyChapterIds: Map[PuzzleTheme.Key, String] = List(
    advancedPawn      -> "sw8VyTe1",
    attackingF2F7     -> "r1ZAcrjZ",
    attraction        -> "3arGcr8n",
    backRankMate      -> "VVzwe5vV",
    capturingDefender -> "2s7CaC2h",
    castling          -> "edXPYM70",
    discoveredAttack  -> "DYcrqEPt",
    doubleCheck       -> "EXAQJVNm",
    enPassant         -> "G7ILIqhG",
    exposedKing       -> "K882yZgm",
    fork              -> "AUQW7PKS",
    hangingPiece      -> "y65GVqXf",
    kingsideAttack    -> "f62Rz8Qb",
    pin               -> "WCTmpBFb",
    promotion         -> "aCGe2oRZ",
    skewer            -> "iF38PGid",
    clearance         -> "ZZsl7iCi",
    trappedPiece      -> "ZJQkwFP6",
    sacrifice         -> "ezFdOVtv",
    interference      -> "nAojbDwV"
  ).view.map { case (theme, id) =>
    theme.key -> id
  }.toMap

  def find(key: String) = byLowerKey get key.toLowerCase

  def findOrAny(key: String) = find(key) | mix

  def findDynamic(key: String) = find(key).filterNot(t => staticThemes(t.key))

  implicit val keyIso = lila.common.Iso.string[Key](Key.apply, _.value)
}
