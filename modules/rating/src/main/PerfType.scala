package lila.rating

import cats.derived.*
import chess.{ Centis, Speed }
import lila.core.i18n.{ I18nKey, Translate }

import lila.common.licon
import lila.core.Icon
import lila.core.perf.{ PerfType, PerfId, PerfKey }

object PerfType:
  import lila.core.perf.PerfType.*

  val default = Standard

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

  def iconByVariant(variant: chess.variant.Variant): Icon =
    byVariant(variant).fold(licon.CrownElite)(_.icon)

  val translated: Set[PerfType] = Set(Bullet, Blitz, Rapid, Classical, Correspondence, Puzzle)
