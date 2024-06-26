package lila.puzzle

import lila.i18n.I18nKeys.{ puzzleTheme => i }
import lila.i18n.{ I18nKey, I18nKeys => trans }

case class PuzzleTheme(key: PuzzleTheme.Key, name: I18nKey, description: I18nKey)

object PuzzleTheme {

  case class Key(value: String) extends AnyVal with StringValue

  case class WithCount(theme: PuzzleTheme, count: Int)

  val mix = PuzzleTheme(Key("mix"), i.healthyMix, i.healthyMixDescription)

  val advantage = PuzzleTheme(Key("advantage"), i.advantage, i.advantageDescription)
  val equality  = PuzzleTheme(Key("equality"), i.equality, i.equalityDescription)
  val crushing  = PuzzleTheme(Key("crushing"), i.crushing, i.crushingDescription)

  val opening    = PuzzleTheme(Key("opening"), i.opening, i.openingDescription)
  val middlegame = PuzzleTheme(Key("middlegame"), i.middlegame, i.middlegameDescription)
  val endgame    = PuzzleTheme(Key("endgame"), i.endgame, i.endgameDescription)

  val oneMove  = PuzzleTheme(Key("oneMove"), i.oneMove, i.oneMoveDescription)
  val short    = PuzzleTheme(Key("short"), i.short, i.shortDescription)
  val long     = PuzzleTheme(Key("long"), i.long, i.longDescription)
  val veryLong = PuzzleTheme(Key("veryLong"), i.veryLong, i.veryLongDescription)

  val fork         = PuzzleTheme(Key("fork"), i.fork, i.forkDescription)
  val pin          = PuzzleTheme(Key("pin"), i.pin, i.pinDescription)
  val sacrifice    = PuzzleTheme(Key("sacrifice"), i.sacrifice, i.sacrificeDescription)
  val strikingPawn = PuzzleTheme(Key("strikingPawn"), i.strikingPawn, i.strikingPawnDescription)
  val joiningPawn  = PuzzleTheme(Key("joiningPawn"), i.joiningPawn, i.joiningPawnDescription)
  val edgeAttack   = PuzzleTheme(Key("edgeAttack"), i.edgeAttack, i.edgeAttackDescription)

  val mate    = PuzzleTheme(Key("mate"), i.mate, i.mateDescription)
  val mateIn1 = PuzzleTheme(Key("mateIn1"), i.mateIn1, i.mateIn1Description)
  val mateIn3 = PuzzleTheme(Key("mateIn3"), i.mateIn3, i.mateIn3Description)
  val mateIn5 = PuzzleTheme(Key("mateIn5"), i.mateIn5, i.mateIn5Description)
  val mateIn7 = PuzzleTheme(Key("mateIn7"), i.mateIn7, i.mateIn7Description)
  val mateIn9 = PuzzleTheme(Key("mateIn9"), i.mateIn9, i.mateIn9Description)

  val tsume        = PuzzleTheme(Key("tsume"), i.tsume, i.tsumeDescription)
  val lishogiGames = PuzzleTheme(Key("lishogiGames"), i.lishogiGames, i.lishogiGamesDescription)
  val otherSources = PuzzleTheme(Key("otherSources"), i.otherSources, i.otherSourcesDescription)

  val categorized = List[(I18nKey, List[PuzzleTheme])](
    trans.puzzle.recommended -> List(
      mix
    ),
    trans.puzzle.phases -> List(
      opening,
      middlegame,
      endgame
    ),
    trans.puzzle.motifs -> List(
      tsume,
      fork,
      pin,
      sacrifice,
      strikingPawn,
      joiningPawn,
      edgeAttack
    ),
    trans.puzzle.mates -> List(
      mate,
      mateIn1,
      mateIn3,
      mateIn5,
      mateIn7,
      mateIn9
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
    ),
    trans.puzzle.origin -> List(
      lishogiGames,
      otherSources
    )
  )

  lazy val all: List[PuzzleTheme] = categorized.flatMap(_._2)

  lazy val allTranslationKeys = all.flatMap { t =>
    List(t.name, t.description)
  }

  private lazy val byKey: Map[Key, PuzzleTheme] = all.view.map { t =>
    t.key -> t
  }.toMap

  private lazy val byLowerKey: Map[String, PuzzleTheme] = all.view.map { t =>
    t.key.value.toLowerCase -> t
  }.toMap

  // themes that can't be voted by players
  val staticThemes: Set[Key] = Set(
    advantage,
    equality,
    crushing,
    mate,
    mateIn1,
    mateIn3,
    mateIn5,
    mateIn7,
    mateIn9,
    oneMove,
    short,
    long,
    veryLong,
    opening,
    middlegame,
    endgame,
    tsume,
    otherSources,
    lishogiGames
  ).map(_.key)

  val studyChapterIds: Map[PuzzleTheme.Key, String] = List(
    fork         -> "AUQW7PKS",
    pin          -> "WCTmpBFb",
    sacrifice    -> "ezFdOVtv",
    strikingPawn -> "addthiss",
    joiningPawn  -> "addthiss",
    edgeAttack   -> "addthiss"
  ).view.map { case (theme, id) =>
    theme.key -> id
  }.toMap

  def apply(key: Key): PuzzleTheme = byKey.getOrElse(key, mix)

  def find(key: String): Option[PuzzleTheme] = byLowerKey get key.toLowerCase

  def findOrAny(key: String) = find(key) | mix

  def findDynamic(key: String) = find(key).filterNot(t => staticThemes(t.key))

  implicit val keyIso = lila.common.Iso.string[Key](Key.apply, _.value)
}
