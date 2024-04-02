package lila.rating

import cats.derived.*
import chess.{ Centis, Speed }
import lila.core.i18n.{ I18nKey, Translate }

import lila.common.licon
import lila.core.rating.{ PerfId, PerfKey }

enum PerfType(
    val id: PerfId,
    val key: PerfKey,
    private val name: String,
    private val title: String,
    val icon: licon.Icon
) derives Eq:

  def iconString                     = icon.toString
  def trans(using Translate): String = PerfType.trans(this)
  def desc(using Translate): String  = PerfType.desc(this)

  case UltraBullet
      extends PerfType(
        PerfId(0),
        key = PerfKey("ultraBullet"),
        name = Speed.UltraBullet.name,
        title = Speed.UltraBullet.title,
        icon = licon.UltraBullet
      )

  case Bullet
      extends PerfType(
        PerfId(1),
        key = PerfKey("bullet"),
        name = Speed.Bullet.name,
        title = Speed.Bullet.title,
        icon = licon.Bullet
      )

  case Blitz
      extends PerfType(
        PerfId(2),
        key = PerfKey("blitz"),
        name = Speed.Blitz.name,
        title = Speed.Blitz.title,
        icon = licon.FlameBlitz
      )

  case Rapid
      extends PerfType(
        PerfId(6),
        key = PerfKey("rapid"),
        name = Speed.Rapid.name,
        title = Speed.Rapid.title,
        icon = licon.Rabbit
      )

  case Classical
      extends PerfType(
        PerfId(3),
        key = PerfKey("classical"),
        name = Speed.Classical.name,
        title = Speed.Classical.title,
        icon = licon.Turtle
      )

  case Correspondence
      extends PerfType(
        PerfId(4),
        key = PerfKey("correspondence"),
        name = "Correspondence",
        title = Speed.Correspondence.title,
        icon = licon.PaperAirplane
      )

  case Standard
      extends PerfType(
        PerfId(5),
        key = PerfKey("standard"),
        name = chess.variant.Standard.name,
        title = "Standard rules of chess",
        icon = licon.Crown
      )

  case Chess960
      extends PerfType(
        PerfId(11),
        key = PerfKey("chess960"),
        name = chess.variant.Chess960.name,
        title = "Chess960 variant",
        icon = licon.DieSix
      )

  case KingOfTheHill
      extends PerfType(
        PerfId(12),
        key = PerfKey("kingOfTheHill"),
        name = chess.variant.KingOfTheHill.name,
        title = "King of the Hill variant",
        icon = licon.FlagKingHill
      )

  case Antichess
      extends PerfType(
        PerfId(13),
        key = PerfKey("antichess"),
        name = chess.variant.Antichess.name,
        title = "Antichess variant",
        icon = licon.Antichess
      )

  case Atomic
      extends PerfType(
        PerfId(14),
        key = PerfKey("atomic"),
        name = chess.variant.Atomic.name,
        title = "Atomic variant",
        icon = licon.Atom
      )

  case ThreeCheck
      extends PerfType(
        PerfId(15),
        key = PerfKey("threeCheck"),
        name = chess.variant.ThreeCheck.name,
        title = "Three-check variant",
        icon = licon.ThreeCheckStack
      )

  case Horde
      extends PerfType(
        PerfId(16),
        key = PerfKey("horde"),
        name = chess.variant.Horde.name,
        title = "Horde variant",
        icon = licon.Keypad
      )

  case RacingKings
      extends PerfType(
        PerfId(17),
        key = PerfKey("racingKings"),
        name = chess.variant.RacingKings.name,
        title = "Racing kings variant",
        icon = licon.FlagRacingKings
      )

  case Crazyhouse
      extends PerfType(
        PerfId(18),
        key = PerfKey("crazyhouse"),
        name = chess.variant.Crazyhouse.name,
        title = "Crazyhouse variant",
        icon = licon.Crazyhouse
      )

  case Puzzle
      extends PerfType(
        PerfId(20),
        key = PerfKey("puzzle"),
        name = "Training",
        title = "Chess tactics trainer",
        icon = licon.ArcheryTarget
      )

object PerfType:
  val all: List[PerfType] = values.toList
  val byKey               = all.mapBy(_.key)
  val byId                = all.mapBy(_.id)

  val default = Standard

  def apply(key: PerfKey): Option[PerfType] = byKey.get(key)
  def orDefault(key: PerfKey): PerfType     = apply(key) | default

  def apply(id: PerfId): Option[PerfType] = byId.get(id)

  def id2key(id: PerfId): Option[PerfKey] = byId.get(id).map(_.key)

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
    byVariant(variant).getOrElse(standardBySpeed(speed))

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

  val translated: Set[PerfType] = Set(Bullet, Blitz, Rapid, Classical, Correspondence, Puzzle)

  def trans(pt: PerfType)(using Translate): String = pt match
    case Bullet         => I18nKey.site.bullet.txt()
    case Blitz          => I18nKey.site.blitz.txt()
    case Rapid          => I18nKey.site.rapid.txt()
    case Classical      => I18nKey.site.classical.txt()
    case Correspondence => I18nKey.site.correspondence.txt()
    case Puzzle         => I18nKey.site.puzzles.txt()
    case pt             => pt.name
  def desc(pt: PerfType)(using Translate): String = pt match
    case UltraBullet    => I18nKey.site.ultraBulletDesc.txt()
    case Bullet         => I18nKey.site.bulletDesc.txt()
    case Blitz          => I18nKey.site.blitzDesc.txt()
    case Rapid          => I18nKey.site.rapidDesc.txt()
    case Classical      => I18nKey.site.classicalDesc.txt()
    case Correspondence => I18nKey.site.correspondenceDesc.txt()
    case Puzzle         => I18nKey.site.puzzleDesc.txt()
    case pt             => pt.title
