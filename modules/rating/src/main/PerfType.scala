package lila.rating

import chess.Centis
import chess.Speed
import play.api.i18n.Lang

import lila.i18n.I18nKeys

sealed abstract class PerfType(
    val id: Perf.ID,
    val key: Perf.Key,
    private val name: String,
    private val title: String,
    val iconChar: Char
) {

  def iconString = iconChar.toString

  def trans(implicit lang: Lang): String = PerfType.trans(this)

  def desc(implicit lang: Lang): String = PerfType.desc(this)
}

object PerfType {

  case object UltraBullet
      extends PerfType(
        0,
        key = "ultraBullet",
        name = Speed.UltraBullet.name,
        title = Speed.UltraBullet.title,
        iconChar = '{'
      )

  case object Bullet
      extends PerfType(
        1,
        key = "bullet",
        name = Speed.Bullet.name,
        title = Speed.Bullet.title,
        iconChar = 'T'
      )

  case object Blitz
      extends PerfType(
        2,
        key = "blitz",
        name = Speed.Blitz.name,
        title = Speed.Blitz.title,
        iconChar = ')'
      )

  case object Rapid
      extends PerfType(
        6,
        key = "rapid",
        name = Speed.Rapid.name,
        title = Speed.Rapid.title,
        iconChar = '#'
      )

  case object Classical
      extends PerfType(
        3,
        key = "classical",
        name = Speed.Classical.name,
        title = Speed.Classical.title,
        iconChar = '+'
      )

  case object Correspondence
      extends PerfType(
        4,
        key = "correspondence",
        name = "Correspondence",
        title = Speed.Correspondence.title,
        iconChar = ';'
      )

  case object Standard
      extends PerfType(
        5,
        key = "standard",
        name = chess.variant.Standard.name,
        title = "Standard rules of chess",
        iconChar = '8'
      )

  case object Chess960
      extends PerfType(
        11,
        key = "chess960",
        name = chess.variant.Chess960.name,
        title = "Chess960 variant",
        iconChar = '\''
      )

  case object KingOfTheHill
      extends PerfType(
        12,
        key = "kingOfTheHill",
        name = chess.variant.KingOfTheHill.name,
        title = "King of the Hill variant",
        iconChar = '('
      )

  case object Antichess
      extends PerfType(
        13,
        key = "antichess",
        name = chess.variant.Antichess.name,
        title = "Antichess variant",
        iconChar = '@'
      )

  case object Atomic
      extends PerfType(
        14,
        key = "atomic",
        name = chess.variant.Atomic.name,
        title = "Atomic variant",
        iconChar = '>'
      )

  case object ThreeCheck
      extends PerfType(
        15,
        key = "threeCheck",
        name = chess.variant.ThreeCheck.name,
        title = "Three-check variant",
        iconChar = '.'
      )

  case object Horde
      extends PerfType(
        16,
        key = "horde",
        name = chess.variant.Horde.name,
        title = "Horde variant",
        iconChar = '_'
      )

  case object RacingKings
      extends PerfType(
        17,
        key = "racingKings",
        name = chess.variant.RacingKings.name,
        title = "Racing kings variant",
        iconChar = ''
      )

  case object Crazyhouse
      extends PerfType(
        18,
        key = "crazyhouse",
        name = chess.variant.Crazyhouse.name,
        title = "Crazyhouse variant",
        iconChar = ''
      )

  case object Puzzle
      extends PerfType(
        20,
        key = "puzzle",
        name = "Training",
        title = "Chess tactics trainer",
        iconChar = '-'
      )

  val all: List[PerfType] = List(
    UltraBullet,
    Bullet,
    Blitz,
    Rapid,
    Classical,
    Correspondence,
    Standard,
    Crazyhouse,
    Chess960,
    KingOfTheHill,
    ThreeCheck,
    Antichess,
    Atomic,
    Horde,
    RacingKings,
    Puzzle
  )
  val byKey = all map { p =>
    (p.key, p)
  } toMap
  val byId = all map { p =>
    (p.id, p)
  } toMap

  val default = Standard

  def apply(key: Perf.Key): Option[PerfType] = byKey get key
  def orDefault(key: Perf.Key): PerfType     = apply(key) | default

  def apply(id: Perf.ID): Option[PerfType] = byId get id

  // def name(key: Perf.Key): Option[String] = apply(key) map (_.name)

  def id2key(id: Perf.ID): Option[Perf.Key] = byId get id map (_.key)

  val nonPuzzle: List[PerfType] = List(
    UltraBullet,
    Bullet,
    Blitz,
    Rapid,
    Classical,
    Correspondence,
    Crazyhouse,
    Chess960,
    KingOfTheHill,
    ThreeCheck,
    Antichess,
    Atomic,
    Horde,
    RacingKings
  )
  val leaderboardable: List[PerfType] = List(
    Bullet,
    Blitz,
    Rapid,
    Classical,
    UltraBullet,
    Crazyhouse,
    Chess960,
    KingOfTheHill,
    ThreeCheck,
    Antichess,
    Atomic,
    Horde,
    RacingKings
  )
  val variants: List[PerfType] =
    List(Crazyhouse, Chess960, KingOfTheHill, ThreeCheck, Antichess, Atomic, Horde, RacingKings)
  val standard: List[PerfType] = List(Bullet, Blitz, Rapid, Classical, Correspondence)

  def variantOf(pt: PerfType): chess.variant.Variant =
    pt match {
      case Crazyhouse    => chess.variant.Crazyhouse
      case Chess960      => chess.variant.Chess960
      case KingOfTheHill => chess.variant.KingOfTheHill
      case ThreeCheck    => chess.variant.ThreeCheck
      case Antichess     => chess.variant.Antichess
      case Atomic        => chess.variant.Atomic
      case Horde         => chess.variant.Horde
      case RacingKings   => chess.variant.RacingKings
      case _             => chess.variant.Standard
    }

  def byVariant(variant: chess.variant.Variant): Option[PerfType] =
    variant match {
      case chess.variant.Standard      => none
      case chess.variant.FromPosition  => none
      case chess.variant.Crazyhouse    => Crazyhouse.some
      case chess.variant.Chess960      => Chess960.some
      case chess.variant.KingOfTheHill => KingOfTheHill.some
      case chess.variant.ThreeCheck    => ThreeCheck.some
      case chess.variant.Antichess     => Antichess.some
      case chess.variant.Atomic        => Atomic.some
      case chess.variant.Horde         => Horde.some
      case chess.variant.RacingKings   => RacingKings.some
    }

  def standardBySpeed(speed: Speed): PerfType = speed match {
    case Speed.UltraBullet    => UltraBullet
    case Speed.Bullet         => Bullet
    case Speed.Blitz          => Blitz
    case Speed.Rapid          => Rapid
    case Speed.Classical      => Classical
    case Speed.Correspondence => Correspondence
  }

  def apply(variant: chess.variant.Variant, speed: Speed): PerfType =
    byVariant(variant) getOrElse standardBySpeed(speed)

  lazy val totalTimeRoughEstimation: Map[PerfType, Centis] = nonPuzzle.view
    .map { pt =>
      pt -> Centis(pt match {
        case UltraBullet    => 25 * 100
        case Bullet         => 90 * 100
        case Blitz          => 7 * 60 * 100
        case Rapid          => 12 * 60 * 100
        case Classical      => 30 * 60 * 100
        case Correspondence => 60 * 60 * 100
        case _              => 7 * 60 * 100
      })
    }
    .to(Map)

  def iconByVariant(variant: chess.variant.Variant): Char =
    byVariant(variant).fold('C')(_.iconChar)

  def trans(pt: PerfType)(implicit lang: Lang): String =
    pt match {
      case Rapid          => I18nKeys.rapid.txt()
      case Classical      => I18nKeys.classical.txt()
      case Correspondence => I18nKeys.correspondence.txt()
      case Puzzle         => I18nKeys.puzzles.txt()
      case pt             => pt.name
    }

  val translated: Set[PerfType] = Set(Rapid, Classical, Correspondence, Puzzle)

  def desc(pt: PerfType)(implicit lang: Lang): String =
    pt match {
      case UltraBullet    => I18nKeys.ultraBulletDesc.txt()
      case Bullet         => I18nKeys.bulletDesc.txt()
      case Blitz          => I18nKeys.blitzDesc.txt()
      case Rapid          => I18nKeys.rapidDesc.txt()
      case Classical      => I18nKeys.classicalDesc.txt()
      case Correspondence => I18nKeys.correspondenceDesc.txt()
      case Puzzle         => I18nKeys.puzzleDesc.txt()
      case pt             => pt.title
    }
}
