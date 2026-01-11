package lila.puzzle

import lila.core.i18n.I18nKey
import lila.core.i18n.I18nKey.puzzleTheme as i

case class PuzzleTheme(key: PuzzleTheme.Key, name: I18nKey, description: I18nKey)

object PuzzleTheme:

  private def apply(name: I18nKey, desc: I18nKey): PuzzleTheme =
    PuzzleTheme(Key(name.value.split(":", 2).lift(1).getOrElse(name.value)), name, desc)

  opaque type Key = String
  object Key extends OpaqueString[Key]

  case class WithCount(theme: PuzzleTheme, count: Int)

  enum VoteError:
    case Fail(msg: String) extends VoteError
    case Unchanged extends VoteError
    def message: String = this match
      case Fail(msg) => msg
      case Unchanged => "unchanged"

  val mix = PuzzleTheme(i.mix, i.mixDescription)
  val advancedPawn = PuzzleTheme(i.advancedPawn, i.advancedPawnDescription)
  val advantage = PuzzleTheme(i.advantage, i.advantageDescription)
  val anastasiaMate = PuzzleTheme(i.anastasiaMate, i.anastasiaMateDescription)
  val arabianMate = PuzzleTheme(i.arabianMate, i.arabianMateDescription)
  val attackingF2F7 = PuzzleTheme(i.attackingF2F7, i.attackingF2F7Description)
  val attraction = PuzzleTheme(i.attraction, i.attractionDescription)
  val backRankMate = PuzzleTheme(i.backRankMate, i.backRankMateDescription)
  val balestraMate = PuzzleTheme(i.balestraMate, i.balestraMateDescription)
  val blindSwineMate = PuzzleTheme(i.blindSwineMate, i.blindSwineMateDescription)
  val triangleMate = PuzzleTheme(i.triangleMate, i.triangleMateDescription)
  val bishopEndgame = PuzzleTheme(i.bishopEndgame, i.bishopEndgameDescription)
  val bodenMate = PuzzleTheme(i.bodenMate, i.bodenMateDescription)
  val capturingDefender =
    PuzzleTheme(i.capturingDefender, i.capturingDefenderDescription)
  val castling = PuzzleTheme(i.castling, i.castlingDescription)
  val clearance = PuzzleTheme(i.clearance, i.clearanceDescription)
  val cornerMate = PuzzleTheme(i.cornerMate, i.cornerMateDescription)
  val crushing = PuzzleTheme(i.crushing, i.crushingDescription)
  val defensiveMove = PuzzleTheme(i.defensiveMove, i.defensiveMoveDescription)
  val deflection = PuzzleTheme(i.deflection, i.deflectionDescription)
  val discoveredAttack =
    PuzzleTheme(i.discoveredAttack, i.discoveredAttackDescription)
  val discoveredCheck = PuzzleTheme(i.discoveredCheck, i.discoveredCheckDescription)
  val doubleBishopMate =
    PuzzleTheme(i.doubleBishopMate, i.doubleBishopMateDescription)
  val doubleCheck = PuzzleTheme(i.doubleCheck, i.doubleCheckDescription)
  val dovetailMate =
    PuzzleTheme(i.dovetailMate, i.dovetailMateDescription)
  val equality = PuzzleTheme(i.equality, i.equalityDescription)
  val endgame = PuzzleTheme(i.endgame, i.endgameDescription)
  val enPassant = PuzzleTheme(I18nKey.site.enPassant, i.enPassantDescription)
  val exposedKing = PuzzleTheme(i.exposedKing, i.exposedKingDescription)
  val fork = PuzzleTheme(i.fork, i.forkDescription)
  val hangingPiece = PuzzleTheme(i.hangingPiece, i.hangingPieceDescription)
  val hookMate = PuzzleTheme(i.hookMate, i.hookMateDescription)
  val interference = PuzzleTheme(i.interference, i.interferenceDescription)
  val intermezzo = PuzzleTheme(i.intermezzo, i.intermezzoDescription)
  val kingsideAttack = PuzzleTheme(i.kingsideAttack, i.kingsideAttackDescription)
  val killBoxMate = PuzzleTheme(i.killBoxMate, i.killBoxMateDescription)
  val pillsburysMate = PuzzleTheme(i.pillsburysMate, i.pillsburysMateDescription)
  val morphysMate = PuzzleTheme(i.morphysMate, i.morphysMateDescription)
  val vukovicMate = PuzzleTheme(i.vukovicMate, i.vukovicMateDescription)
  val knightEndgame = PuzzleTheme(i.knightEndgame, i.knightEndgameDescription)
  val long = PuzzleTheme(i.long, i.longDescription)
  val master = PuzzleTheme(i.master, i.masterDescription)
  val masterVsMaster = PuzzleTheme(i.masterVsMaster, i.masterVsMasterDescription)
  val mate = PuzzleTheme(i.mate, i.mateDescription)
  val mateIn1 = PuzzleTheme(i.mateIn1, i.mateIn1Description)
  val mateIn2 = PuzzleTheme(i.mateIn2, i.mateIn2Description)
  val mateIn3 = PuzzleTheme(i.mateIn3, i.mateIn3Description)
  val mateIn4 = PuzzleTheme(i.mateIn4, i.mateIn4Description)
  val mateIn5 = PuzzleTheme(i.mateIn5, i.mateIn5Description)
  val smotheredMate = PuzzleTheme(i.smotheredMate, i.smotheredMateDescription)
  val middlegame = PuzzleTheme(i.middlegame, i.middlegameDescription)
  val oneMove = PuzzleTheme(i.oneMove, i.oneMoveDescription)
  val opening = PuzzleTheme(i.opening, i.openingDescription)
  val operaMate = PuzzleTheme(i.operaMate, i.operaMateDescription)
  val pawnEndgame = PuzzleTheme(i.pawnEndgame, i.pawnEndgameDescription)
  val pin = PuzzleTheme(i.pin, i.pinDescription)
  val promotion = PuzzleTheme(i.promotion, i.promotionDescription)
  val queenEndgame = PuzzleTheme(i.queenEndgame, i.queenEndgameDescription)
  val queenRookEndgame =
    PuzzleTheme(i.queenRookEndgame, i.queenRookEndgameDescription)
  val queensideAttack = PuzzleTheme(i.queensideAttack, i.queensideAttackDescription)
  val quietMove = PuzzleTheme(i.quietMove, i.quietMoveDescription)
  val rookEndgame = PuzzleTheme(i.rookEndgame, i.rookEndgameDescription)
  val sacrifice = PuzzleTheme(i.sacrifice, i.sacrificeDescription)
  val short = PuzzleTheme(i.short, i.shortDescription)
  val skewer = PuzzleTheme(i.skewer, i.skewerDescription)
  val superGM = PuzzleTheme(i.superGM, i.superGMDescription)
  val trappedPiece = PuzzleTheme(i.trappedPiece, i.trappedPieceDescription)
  val underPromotion = PuzzleTheme(i.underPromotion, i.underPromotionDescription)
  val veryLong = PuzzleTheme(i.veryLong, i.veryLongDescription)
  val xRayAttack = PuzzleTheme(i.xRayAttack, i.xRayAttackDescription)
  val zugzwang = PuzzleTheme(i.zugzwang, i.zugzwangDescription)
  val checkFirst = PuzzleTheme(Key("checkFirst"), I18nKey("Check first"), I18nKey("Check first"))

  val categorized = List[(I18nKey, List[PuzzleTheme])](
    I18nKey.puzzle.recommended -> List(
      mix
    ),
    I18nKey.puzzle.phases -> List(
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
    I18nKey.puzzle.motifs -> List(
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
    I18nKey.puzzle.advanced -> List(
      attraction,
      clearance,
      discoveredCheck,
      defensiveMove,
      deflection,
      interference,
      intermezzo,
      quietMove,
      xRayAttack,
      zugzwang
    ),
    I18nKey.puzzle.mates -> List(
      mate,
      mateIn1,
      mateIn2,
      mateIn3,
      mateIn4,
      mateIn5
    ),
    I18nKey.puzzle.mateThemes -> List(
      anastasiaMate,
      arabianMate,
      backRankMate,
      balestraMate,
      blindSwineMate,
      bodenMate,
      cornerMate,
      doubleBishopMate,
      dovetailMate,
      hookMate,
      killBoxMate,
      pillsburysMate,
      morphysMate,
      operaMate,
      triangleMate,
      vukovicMate,
      smotheredMate
    ),
    I18nKey.puzzle.specialMoves -> List(
      castling,
      enPassant,
      promotion,
      underPromotion
    ),
    I18nKey.puzzle.goals -> List(
      equality,
      advantage,
      crushing,
      mate
    ),
    I18nKey.puzzle.lengths -> List(
      oneMove,
      short,
      long,
      veryLong
    ),
    I18nKey.puzzle.origin -> List(
      master,
      masterVsMaster,
      superGM
    )
  )

  val visible: List[PuzzleTheme] = categorized.flatMap(_._2)
  // themes that can't be viewed by players
  private[puzzle] val hiddenThemes: List[PuzzleTheme] = List(checkFirst)

  private val all: List[PuzzleTheme] = visible ::: hiddenThemes
  val hiddenThemesKey: Set[Key] = hiddenThemes.map(_.key).toSet

  private val byKey: Map[Key, PuzzleTheme] = all.mapBy(_.key)

  private val byLowerKey: Map[String, PuzzleTheme] = all.mapBy(_.key.value.toLowerCase)

  // themes that can't be voted by players
  val staticThemes: Set[Key] = Set(
    advantage,
    castling,
    crushing,
    enPassant,
    endgame,
    equality,
    long,
    master,
    masterVsMaster,
    superGM,
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
    veryLong,
    checkFirst
  ).map(_.key)

  val allMates: Set[Key] = visible.filter(_.key.value.endsWith("Mate")).map(_.key).toSet

  val studyChapterIds: Map[PuzzleTheme.Key, String] = List(
    advancedPawn -> "sw8VyTe1",
    attackingF2F7 -> "r1ZAcrjZ",
    attraction -> "3arGcr8n",
    backRankMate -> "VVzwe5vV",
    capturingDefender -> "2s7CaC2h",
    castling -> "edXPYM70",
    discoveredAttack -> "DYcrqEPt",
    doubleCheck -> "EXAQJVNm",
    enPassant -> "G7ILIqhG",
    exposedKing -> "K882yZgm",
    fork -> "AUQW7PKS",
    hangingPiece -> "y65GVqXf",
    kingsideAttack -> "f62Rz8Qb",
    pin -> "WCTmpBFb",
    promotion -> "BNuCO8JO",
    skewer -> "iF38PGid",
    clearance -> "ZZsl7iCi",
    trappedPiece -> "ZJQkwFP6",
    sacrifice -> "ezFdOVtv",
    interference -> "nAojbDwV"
  ).view.map { (theme, id) =>
    theme.key -> id
  }.toMap

  def apply(key: Key): PuzzleTheme = byKey.getOrElse(key, mix)

  def findAny(key: String) = byLowerKey.get(key.toLowerCase)
  def findVisible(key: String) = findAny(key).filterNot(hiddenThemes.contains)

  def findOrMix(key: String) = findVisible(key) | mix

  def findDynamic(key: String) = findVisible(key).filterNot(t => staticThemes(t.key))
