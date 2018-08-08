package lidraughts.rating

import draughts.Centis

import draughts.Speed

sealed abstract class PerfType(
    val id: Perf.ID,
    val key: Perf.Key,
    val name: String,
    val title: String,
    val iconChar: Char
) {

  def iconString = iconChar.toString
}

object PerfType {

  case object UltraBullet extends PerfType(
    0,
    key = "ultraBullet",
    name = Speed.UltraBullet.name,
    title = Speed.UltraBullet.title,
    iconChar = '{'
  )

  case object Bullet extends PerfType(
    1,
    key = "bullet",
    name = Speed.Bullet.name,
    title = Speed.Bullet.title,
    iconChar = 'T'
  )

  case object Blitz extends PerfType(
    2,
    key = "blitz",
    name = Speed.Blitz.name,
    title = Speed.Blitz.title,
    iconChar = ')'
  )

  case object Rapid extends PerfType(
    6,
    key = "rapid",
    name = Speed.Rapid.name,
    title = Speed.Rapid.title,
    iconChar = '#'
  )

  case object Classical extends PerfType(
    3,
    key = "classical",
    name = Speed.Classical.name,
    title = Speed.Classical.title,
    iconChar = '+'
  )

  case object Correspondence extends PerfType(
    4,
    key = "correspondence",
    name = "Correspondence",
    title = "Correspondence (days per turn)",
    iconChar = ';'
  )

  case object Standard extends PerfType(
    5,
    key = "standard",
    name = draughts.variant.Standard.name,
    title = "Standard rules of international draughts",
    iconChar = '8'
  )

  case object Frisian extends PerfType(
    11,
    key = "frisian",
    name = draughts.variant.Frisian.name,
    title = "Frisian variant",
    iconChar = '''
  )

  case object Puzzle extends PerfType(
    20,
    key = "puzzle",
    name = "Training",
    title = "Training puzzles",
    iconChar = '-'
  )

  val all: List[PerfType] = List(UltraBullet, Bullet, Blitz, Rapid, Classical, Correspondence, Standard, Frisian, Puzzle)
  val byKey = all map { p => (p.key, p) } toMap
  val byId = all map { p => (p.id, p) } toMap

  val default = Standard

  def apply(key: Perf.Key): Option[PerfType] = byKey get key
  def orDefault(key: Perf.Key): PerfType = apply(key) | default

  def apply(id: Perf.ID): Option[PerfType] = byId get id

  def name(key: Perf.Key): Option[String] = apply(key) map (_.name)

  def id2key(id: Perf.ID): Option[Perf.Key] = byId get id map (_.key)

  val nonPuzzle: List[PerfType] = List(UltraBullet, Bullet, Blitz, Rapid, Classical, Correspondence, Frisian)
  val nonGame: List[PerfType] = List(Puzzle)
  val leaderboardable: List[PerfType] = List(Bullet, Blitz, Rapid, Classical, UltraBullet, Frisian)
  val variants: List[PerfType] = List(Frisian)
  val variantsPlus: List[PerfType] = List(Standard, Frisian)
  val standard: List[PerfType] = List(Bullet, Blitz, Rapid, Classical, Correspondence)

  def isGame(pt: PerfType) = !nonGame.contains(pt)

  val nonPuzzleIconByName = nonPuzzle.map { pt =>
    pt.name -> pt.iconString
  } toMap

  def variantOf(pt: PerfType): draughts.variant.Variant = pt match {
    case Frisian => draughts.variant.Frisian
    case _ => draughts.variant.Standard
  }

  def byVariant(variant: draughts.variant.Variant): Option[PerfType] = variant match {
    case draughts.variant.Frisian => Frisian.some
    case _ => none
  }

  def checkStandard(variant: draughts.variant.Variant): Option[PerfType] = variant match {
    case draughts.variant.Standard => Standard.some
    case _ => none
  }

  lazy val totalTimeRoughEstimation: Map[PerfType, Centis] = nonPuzzle.map { pt =>
    pt -> Centis(pt match {
      case UltraBullet => 25 * 100
      case Bullet => 90 * 100
      case Blitz => 7 * 60 * 100
      case Rapid => 12 * 60 * 100
      case Classical => 30 * 60 * 100
      case Correspondence => 60 * 60 * 100
      case _ => 7 * 60 * 100
    })
  }(scala.collection.breakOut)

  def iconByVariant(variant: draughts.variant.Variant): Char =
    byVariant(variant).fold('C')(_.iconChar)
}
