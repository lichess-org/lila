package lila.badge

import lila.rating.PerfType

case class Badge(
    serie: String,
    level: Int,
    name: String,
    description: String,
    image: String) {

  def id = s"$serie:$level"

  def supersedes(other: Badge): Boolean = serie == other.serie && level > other.level
}

object Badge {

  val patron = Badge(
    serie = "patron",
    level = 1,
    name = "Lichess Patron",
    description = "Proud supporter of chess and freedom",
    image = "curly-wing.svg")

  private def makeFriend(nb: Int) = Badge(
    serie = "friend",
    level = nb,
    name = pluralize("friend", nb),
    description = s"Mutually following ${pluralize("player", nb)}",
    image = "linked-rings")

  private def makeFollowing(nb: Int) = Badge(
    serie = "following",
    level = nb,
    name = pluralize("follow", nb),
    description = s"Following ${pluralize("player", nb)}",
    image = "thumb-up.svg")

  private def makeFollowers(nb: Int) = Badge(
    serie = "followers",
    level = nb,
    name = pluralize("followers", nb),
    description = s"Followed by ${pluralize("player", nb)}",
    image = "rainbow-star.svg")

  private val perfImageMap: Map[PerfType, String] = Map(
    PerfType.Bullet -> "focused-lightning.svg",
    PerfType.Blitz -> "small-fire.svg",
    PerfType.Classical -> "clockwork.svg",
    PerfType.Correspondence -> "paper-plane.svg",
    PerfType.Crazyhouse -> "diamonds-smile.svg",
    PerfType.Chess960 -> "rolling-dices.svg")

  private def makePerfNb(pt: PerfType, nb: Int): Option[Badge] = perfImageMap get pt map { image =>
    Badge(
      serie = s"perf:${pt.key}",
      level = nb,
      name = pluralize(s"${pt.name} game", nb),
      description = s"""Played ${pluralize(s"${pt.name} game", nb)}""",
      image = image)
  }
  private def makeAllPerfsNb(nb: Int): List[Badge] = PerfType.nonPuzzle.flatMap { makePerfNb(_, nb) }

  val all: List[Badge] = List(
    List(patron),
    List(1, 10, 25, 50) map makeFriend,
    List(1, 10, 50, 100) map makeFollowing,
    List(1, 10, 50, 100, 200, 500, 1000, 1500, 2000) map makeFollowers,
    List(1, 10, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000) flatMap makeAllPerfsNb
  ).flatten

  private val idsMap: Map[String, Badge] = all.map { b => b.id -> b }.toMap

  def byId(id: String): Option[Badge] = idsMap get id

  private def pluralize(s: String, n: Int) =
    s"${if (n == 1) "one" else n.toString} $s${if (n > 1) "s" else ""}"
}
