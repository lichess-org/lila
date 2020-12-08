package lila.rating

import play.api.i18n.Lang

import chess.Centis
import chess.Speed
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
  )
  val nonGame: List[PerfType] = List(Puzzle)
  val leaderboardable: List[PerfType] = List(
    Bullet,
    Blitz,
    Rapid,
    Classical,
    UltraBullet,
    Correspondence,
  )
  val variants: List[PerfType] = Nil
  val standard: List[PerfType] = List(Bullet, Blitz, Rapid, Classical, Correspondence)

  def isGame(pt: PerfType) = !nonGame.contains(pt)

  def variantOf(pt: PerfType): chess.variant.Variant =
    pt match {
      case _             => chess.variant.Standard // todo variant
    }

  def byVariant(variant: chess.variant.Variant): Option[PerfType] =
    variant match {
      case _                           => none
    }

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
