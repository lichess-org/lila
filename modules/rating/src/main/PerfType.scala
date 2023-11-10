package lila.rating

import cats.derived.*

import chess.Centis
import chess.Speed
import play.api.i18n.Lang

import lila.common.licon
import lila.i18n.I18nKeys

enum PerfType(
    val id: Perf.Id,
    val key: Perf.Key,
    private val name: String,
    private val title: String,
    val icon: licon.Icon
) derives Eq:

  def iconString                = icon.toString
  def trans(using Lang): String = PerfType.trans(this)
  def desc(using Lang): String  = PerfType.desc(this)

  case UltraBullet
      extends PerfType(
        Perf.Id(0),
        key = Perf.Key("ultraBullet"),
        name = Speed.UltraBullet.name,
        title = Speed.UltraBullet.title,
        icon = licon.UltraBullet
      )

  case Bullet
      extends PerfType(
        Perf.Id(1),
        key = Perf.Key("bullet"),
        name = Speed.Bullet.name,
        title = Speed.Bullet.title,
        icon = licon.Bullet
      )

  case Blitz
      extends PerfType(
        Perf.Id(2),
        key = Perf.Key("blitz"),
        name = Speed.Blitz.name,
        title = Speed.Blitz.title,
        icon = licon.FlameBlitz
      )

  case Rapid
      extends PerfType(
        Perf.Id(6),
        key = Perf.Key("rapid"),
        name = Speed.Rapid.name,
        title = Speed.Rapid.title,
        icon = licon.Rabbit
      )

  case Classical
      extends PerfType(
        Perf.Id(3),
        key = Perf.Key("classical"),
        name = Speed.Classical.name,
        title = Speed.Classical.title,
        icon = licon.Turtle
      )

  case Correspondence
      extends PerfType(
        Perf.Id(4),
        key = Perf.Key("correspondence"),
        name = "Correspondence",
        title = Speed.Correspondence.title,
        icon = licon.PaperAirplane
      )

  case Standard
      extends PerfType(
        Perf.Id(5),
        key = Perf.Key("standard"),
        name = chess.variant.Standard.name,
        title = "Standard rules of chess",
        icon = licon.Crown
      )

  case Chess960
      extends PerfType(
        Perf.Id(11),
        key = Perf.Key("chess960"),
        name = chess.variant.Chess960.name,
        title = "Chess960 variant",
        icon = licon.DieSix
      )

  case KingOfTheHill
      extends PerfType(
        Perf.Id(12),
        key = Perf.Key("kingOfTheHill"),
        name = chess.variant.KingOfTheHill.name,
        title = "King of the Hill variant",
        icon = licon.FlagKingHill
      )

  case Antichess
      extends PerfType(
        Perf.Id(13),
        key = Perf.Key("antichess"),
        name = chess.variant.Antichess.name,
        title = "Antichess variant",
        icon = licon.Antichess
      )

  case Atomic
      extends PerfType(
        Perf.Id(14),
        key = Perf.Key("atomic"),
        name = chess.variant.Atomic.name,
        title = "Atomic variant",
        icon = licon.Atom
      )

  case ThreeCheck
      extends PerfType(
        Perf.Id(15),
        key = Perf.Key("threeCheck"),
        name = chess.variant.ThreeCheck.name,
        title = "Three-check variant",
        icon = licon.ThreeCheckStack
      )

  case Horde
      extends PerfType(
        Perf.Id(16),
        key = Perf.Key("horde"),
        name = chess.variant.Horde.name,
        title = "Horde variant",
        icon = licon.Keypad
      )

  case RacingKings
      extends PerfType(
        Perf.Id(17),
        key = Perf.Key("racingKings"),
        name = chess.variant.RacingKings.name,
        title = "Racing kings variant",
        icon = licon.FlagRacingKings
      )

  case Crazyhouse
      extends PerfType(
        Perf.Id(18),
        key = Perf.Key("crazyhouse"),
        name = chess.variant.Crazyhouse.name,
        title = "Crazyhouse variant",
        icon = licon.Crazyhouse
      )

  case Puzzle
      extends PerfType(
        Perf.Id(20),
        key = Perf.Key("puzzle"),
        name = "Training",
        title = "Chess tactics trainer",
        icon = licon.ArcheryTarget
      )

object PerfType:
  val all: List[PerfType] = values.toList
  val byKey               = all.mapBy(_.key)
  val byId                = all.mapBy(_.id)

  val default = Standard

  def apply(key: Perf.Key): Option[PerfType] = byKey get key
  def orDefault(key: Perf.Key): PerfType     = apply(key) | default

  def apply(id: Perf.Id): Option[PerfType] = byId get id

  def id2key(id: Perf.Id): Option[Perf.Key] = byId get id map (_.key)

  val nonPuzzle: List[PerfType] = all.filter(_ != Puzzle)

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
  val isLeaderboardable: Set[PerfType] = leaderboardable.toSet
  val variants: List[PerfType] =
    List(Crazyhouse, Chess960, KingOfTheHill, ThreeCheck, Antichess, Atomic, Horde, RacingKings)
  val standard: List[PerfType]          = List(Bullet, Blitz, Rapid, Classical, Correspondence)
  val standardWithUltra: List[PerfType] = UltraBullet :: standard

  def variantOf(pt: PerfType): chess.variant.Variant = pt match
    case Crazyhouse    => chess.variant.Crazyhouse
    case Chess960      => chess.variant.Chess960
    case KingOfTheHill => chess.variant.KingOfTheHill
    case ThreeCheck    => chess.variant.ThreeCheck
    case Antichess     => chess.variant.Antichess
    case Atomic        => chess.variant.Atomic
    case Horde         => chess.variant.Horde
    case RacingKings   => chess.variant.RacingKings
    case _             => chess.variant.Standard

  def byVariant(variant: chess.variant.Variant): Option[PerfType] = variant match
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

  def standardBySpeed(speed: Speed): PerfType = speed match
    case Speed.UltraBullet    => UltraBullet
    case Speed.Bullet         => Bullet
    case Speed.Blitz          => Blitz
    case Speed.Rapid          => Rapid
    case Speed.Classical      => Classical
    case Speed.Correspondence => Correspondence

  def apply(variant: chess.variant.Variant, speed: Speed): PerfType =
    byVariant(variant) getOrElse standardBySpeed(speed)

  lazy val totalTimeRoughEstimation: Map[PerfType, Centis] =
    nonPuzzle.view
      .map: pt =>
        pt -> Centis:
          pt.match
            case UltraBullet    => 25 * 100
            case Bullet         => 90 * 100
            case Blitz          => 7 * 60 * 100
            case Rapid          => 12 * 60 * 100
            case Classical      => 30 * 60 * 100
            case Correspondence => 60 * 60 * 100
            case _              => 7 * 60 * 100
      .to(Map)

  def iconByVariant(variant: chess.variant.Variant): licon.Icon =
    byVariant(variant).fold(licon.CrownElite)(_.icon)

  def trans(pt: PerfType)(using Lang): String = pt match
    case Blitz          => I18nKeys.blitz.txt()
    case Rapid          => I18nKeys.rapid.txt()
    case Classical      => I18nKeys.classical.txt()
    case Correspondence => I18nKeys.correspondence.txt()
    case Puzzle         => I18nKeys.puzzles.txt()
    case pt             => pt.name

  val translated: Set[PerfType] = Set(Rapid, Classical, Correspondence, Puzzle)

  def desc(pt: PerfType)(using Lang): String = pt match
    case UltraBullet    => I18nKeys.ultraBulletDesc.txt()
    case Bullet         => I18nKeys.bulletDesc.txt()
    case Blitz          => I18nKeys.blitzDesc.txt()
    case Rapid          => I18nKeys.rapidDesc.txt()
    case Classical      => I18nKeys.classicalDesc.txt()
    case Correspondence => I18nKeys.correspondenceDesc.txt()
    case Puzzle         => I18nKeys.puzzleDesc.txt()
    case pt             => pt.title
