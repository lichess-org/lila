package lila.rating

import cats.derived.*
import chess.variant
import chess.{ Centis, Speed }
import lila.core.i18n.{ I18nKey, Translate }
import lila.core.perf.{ PerfId, PerfKeyStr }
import lila.ui.Icon

enum PerfType(
    val id: PerfId,
    val key: PerfKey,
    val icon: Icon,
    val nameKey: I18nKey,
    val descKey: I18nKey
) derives Eq:

  def trans(using translate: Translate): String = nameKey.txt()
  def desc(using translate: Translate): String  = descKey.txt()

  case UltraBullet
      extends PerfType(
        PerfId(0),
        key = PerfKey.ultraBullet,
        icon = Icon.UltraBullet,
        nameKey = I18nKey(Speed.UltraBullet.name),
        descKey = I18nKey.site.ultraBulletDesc
      )

  case Bullet
      extends PerfType(
        PerfId(1),
        key = PerfKey.bullet,
        icon = Icon.Bullet,
        nameKey = I18nKey.site.bullet,
        descKey = I18nKey.site.bulletDesc
      )

  case Blitz
      extends PerfType(
        PerfId(2),
        key = PerfKey.blitz,
        icon = Icon.FlameBlitz,
        nameKey = I18nKey.site.blitz,
        descKey = I18nKey.site.blitzDesc
      )

  case Rapid
      extends PerfType(
        PerfId(6),
        key = PerfKey.rapid,
        icon = Icon.Rabbit,
        nameKey = I18nKey.site.rapid,
        descKey = I18nKey.site.rapidDesc
      )

  case Classical
      extends PerfType(
        PerfId(3),
        key = PerfKey.classical,
        icon = Icon.Turtle,
        nameKey = I18nKey.site.classical,
        descKey = I18nKey.site.classicalDesc
      )

  case Correspondence
      extends PerfType(
        PerfId(4),
        key = PerfKey.correspondence,
        icon = Icon.PaperAirplane,
        nameKey = I18nKey.site.correspondence,
        descKey = I18nKey.site.correspondenceDesc
      )

  case Standard
      extends PerfType(
        PerfId(5),
        key = PerfKey.standard,
        icon = Icon.Crown,
        nameKey = I18nKey(variant.Standard.name),
        descKey = I18nKey("Standard rules of chess")
      )

  case Chess960
      extends PerfType(
        PerfId(11),
        key = PerfKey.chess960,
        icon = Icon.DieSix,
        nameKey = I18nKey(variant.Chess960.name),
        descKey = I18nKey("Chess960 variant")
      )

  case KingOfTheHill
      extends PerfType(
        PerfId(12),
        key = PerfKey.kingOfTheHill,
        icon = Icon.FlagKingHill,
        nameKey = I18nKey(variant.KingOfTheHill.name),
        descKey = I18nKey("King of the Hill variant")
      )

  case Antichess
      extends PerfType(
        PerfId(13),
        key = PerfKey.antichess,
        icon = Icon.Antichess,
        nameKey = I18nKey(variant.Antichess.name),
        descKey = I18nKey("Antichess variant")
      )

  case Atomic
      extends PerfType(
        PerfId(14),
        key = PerfKey.atomic,
        icon = Icon.Atom,
        nameKey = I18nKey(variant.Atomic.name),
        descKey = I18nKey("Atomic variant")
      )

  case ThreeCheck
      extends PerfType(
        PerfId(15),
        key = PerfKey.threeCheck,
        icon = Icon.ThreeCheckStack,
        nameKey = I18nKey(variant.ThreeCheck.name),
        descKey = I18nKey("Three-check variant")
      )

  case Horde
      extends PerfType(
        PerfId(16),
        key = PerfKey.horde,
        icon = Icon.Keypad,
        nameKey = I18nKey(variant.Horde.name),
        descKey = I18nKey("Horde variant")
      )

  case RacingKings
      extends PerfType(
        PerfId(17),
        key = PerfKey.racingKings,
        icon = Icon.FlagRacingKings,
        nameKey = I18nKey(variant.RacingKings.name),
        descKey = I18nKey("Racing kings variant")
      )

  case Crazyhouse
      extends PerfType(
        PerfId(18),
        key = PerfKey.crazyhouse,
        icon = Icon.Crazyhouse,
        nameKey = I18nKey(variant.Crazyhouse.name),
        descKey = I18nKey("Crazyhouse variant")
      )

  case Puzzle
      extends PerfType(
        PerfId(20),
        key = PerfKey.puzzle,
        icon = Icon.ArcheryTarget,
        nameKey = I18nKey.site.puzzles,
        descKey = I18nKey.site.puzzleDesc
      )

object PerfType:
  given Conversion[PerfType, PerfKey] = _.key
  given Conversion[PerfType, PerfId]  = _.id
  given Conversion[PerfKey, PerfType] = apply(_)
  val all: List[PerfType]             = values.toList
  val byKey                           = all.mapBy(_.key)
  val byId                            = all.mapBy(_.id)

  def apply(key: PerfKey): PerfType =
    byKey.getOrElse(key, sys.error(s"Impossible: $key couldn't have been instantiated"))

  def apply(id: PerfId): Option[PerfType] = byId.get(id)

  def read(key: PerfKeyStr): Option[PerfType] = PerfKey.read(key).map(apply)

  val nonPuzzle: List[PerfType] = all.filter(_ != Puzzle)

  val standard: List[PerfKey] =
    List(PerfKey.bullet, PerfKey.blitz, PerfKey.rapid, PerfKey.classical, PerfKey.correspondence)
  val standardSet: Set[PerfKey]        = standard.toSet
  val standardWithUltra: List[PerfKey] = PerfKey.ultraBullet :: standard
  val leaderboardable: List[PerfKey] = List(
    PerfKey.bullet,
    PerfKey.blitz,
    PerfKey.rapid,
    PerfKey.classical,
    PerfKey.ultraBullet,
    PerfKey.crazyhouse,
    PerfKey.chess960,
    PerfKey.kingOfTheHill,
    PerfKey.threeCheck,
    PerfKey.antichess,
    PerfKey.atomic,
    PerfKey.horde,
    PerfKey.racingKings
  )
  val isLeaderboardable: Set[PerfKey] = leaderboardable.toSet
  val variants: List[PerfKey] =
    List(
      PerfKey.crazyhouse,
      PerfKey.chess960,
      PerfKey.kingOfTheHill,
      PerfKey.threeCheck,
      PerfKey.antichess,
      PerfKey.atomic,
      PerfKey.horde,
      PerfKey.racingKings
    )

  def variantOf(pk: PerfKey): chess.variant.Variant = pk match
    case PerfKey.crazyhouse    => chess.variant.Crazyhouse
    case PerfKey.chess960      => chess.variant.Chess960
    case PerfKey.kingOfTheHill => chess.variant.KingOfTheHill
    case PerfKey.threeCheck    => chess.variant.ThreeCheck
    case PerfKey.antichess     => chess.variant.Antichess
    case PerfKey.atomic        => chess.variant.Atomic
    case PerfKey.horde         => chess.variant.Horde
    case PerfKey.racingKings   => chess.variant.RacingKings
    case _                     => chess.variant.Standard

  def apply(variant: chess.variant.Variant, speed: Speed): PerfType = PerfType(PerfKey(variant, speed))

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

  def iconByVariant(variant: chess.variant.Variant): Icon =
    PerfKey.byVariant(variant).fold(Icon.CrownElite)(_.icon)

  val translated: Set[PerfType] = Set(Bullet, Blitz, Rapid, Classical, Correspondence, Puzzle)
