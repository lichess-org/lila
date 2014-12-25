package lila.rating

import chess.{ Variant, Speed }

sealed abstract class PerfType(
  val key: Perf.Key,
  val name: String,
  val title: String,
  val icon: String)

object PerfType {

  case object Bullet extends PerfType(
    key = "bullet",
    name = Speed.Bullet.name,
    title = Speed.Bullet.title,
    icon = "T")

  case object Blitz extends PerfType(
    key = "blitz",
    name = Speed.Blitz.name,
    title = Speed.Blitz.title,
    icon = ")")

  case object Classical extends PerfType(
    key = "classical",
    name = Speed.Classical.name,
    title = Speed.Classical.title,
    icon = "+")

  case object Correspondence extends PerfType(
    key = "correspondence",
    name = "Corresp.",
    title = "Correspondence (days per turn)",
    icon = ";")

  case object Standard extends PerfType(
    key = "standard",
    name = Variant.Standard.name,
    title = "Standard rules of chess",
    icon = "8")

  case object Chess960 extends PerfType(
    key = "chess960",
    name = Variant.Chess960.name,
    title = "Chess960 variant",
    icon = "'")

  case object KingOfTheHill extends PerfType(
    key = "kingOfTheHill",
    name = Variant.KingOfTheHill.name,
    title = "King of the Hill variant",
    icon = "(")

  case object Antichess extends PerfType(
    key = "antichess",
    name = Variant.Antichess.name,
    title = "Antichess variant",
    icon = "&gt;"
  )

  case object ThreeCheck extends PerfType(
    key = "threeCheck",
    name = Variant.ThreeCheck.name,
    title = "Three-check variant",
    icon = ".")

  case object Puzzle extends PerfType(
    key = "puzzle",
    name = "Training",
    title = "Training puzzles",
    icon = "-")

  val all: List[PerfType] = List(Bullet, Blitz, Classical, Correspondence, Standard, Chess960, KingOfTheHill, ThreeCheck, Antichess, Puzzle)
  val byKey = all map { p => (p.key, p) } toMap

  val default = Standard

  def apply(key: Perf.Key): Option[PerfType] = byKey get key
  def orDefault(key: Perf.Key): PerfType = apply(key) | default

  def name(key: Perf.Key): Option[String] = apply(key) map (_.name)

  val nonPuzzle: List[PerfType] = List(Bullet, Blitz, Classical, Correspondence, Chess960, KingOfTheHill, ThreeCheck, Antichess)
  val leaderboardable: List[PerfType] = List(Bullet, Blitz, Classical, Chess960, KingOfTheHill, ThreeCheck)
}
