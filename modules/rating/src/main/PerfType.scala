package lila.rating

import chess.{ Variant, Speed }

sealed abstract class PerfType(
  val key: Perf.Key,
  val name: String,
  val title: String,
  val iconChar: Char)

object PerfType {

  case object Bullet extends PerfType(
    key = "bullet",
    name = Speed.Bullet.name,
    title = Speed.Bullet.title,
    iconChar = 'T')

  case object Blitz extends PerfType(
    key = "blitz",
    name = Speed.Blitz.name,
    title = Speed.Blitz.title,
    iconChar = ')')

  case object Classical extends PerfType(
    key = "classical",
    name = Speed.Classical.name,
    title = Speed.Classical.title,
    iconChar = '+')

  case object Correspondence extends PerfType(
    key = "correspondence",
    name = "Corresp.",
    title = "Correspondence (days per turn)",
    iconChar = ';')

  case object Standard extends PerfType(
    key = "standard",
    name = Variant.Standard.name,
    title = "Standard rules of chess",
    iconChar = '8')

  case object Chess960 extends PerfType(
    key = "chess960",
    name = Variant.Chess960.name,
    title = "Chess960 variant",
    iconChar = ''')

  case object KingOfTheHill extends PerfType(
    key = "kingOfTheHill",
    name = Variant.KingOfTheHill.name,
    title = "King of the Hill variant",
    iconChar = '(')

  case object Antichess extends PerfType(
    key = "suicide",
    name = Variant.Antichess.name,
    title = "Suicide Chess variant",
    iconChar = '+' // todo: use a unique icon
  )

  case object ThreeCheck extends PerfType(
    key = "threeCheck",
    name = Variant.ThreeCheck.name,
    title = "Three-check variant",
    iconChar = '.')

  case object Puzzle extends PerfType(
    key = "puzzle",
    name = "Training",
    title = "Training puzzles",
    iconChar = '-')

  val all: List[PerfType] = List(Bullet, Blitz, Classical, Correspondence, Standard, Chess960, KingOfTheHill, ThreeCheck, Puzzle)
  val byKey = all map { p => (p.key, p) } toMap

  val default = Standard

  def apply(key: Perf.Key): Option[PerfType] = byKey get key
  def orDefault(key: Perf.Key): PerfType = apply(key) | default

  def name(key: Perf.Key): Option[String] = apply(key) map (_.name)

  val nonPuzzle: List[PerfType] = List(Bullet, Blitz, Classical, Correspondence, Chess960, KingOfTheHill, ThreeCheck)
  val leaderboardable: List[PerfType] = List(Bullet, Blitz, Classical, Chess960, KingOfTheHill, ThreeCheck)
}
