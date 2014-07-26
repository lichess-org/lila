package lila.user

import reactivemongo.bson.BSONDocument

import chess.Variant
import lila.db.BSON
import lila.rating.Perf

case class Perfs(
    standard: Perf,
    chess960: Perf,
    bullet: Perf,
    blitz: Perf,
    slow: Perf,
    white: Perf,
    black: Perf,
    puzzle: Perf,
    pools: Map[String, Perf]) {

  def perfs = List(
    "standard" -> standard,
    "chess960" -> chess960,
    "bullet" -> bullet,
    "blitz" -> blitz,
    "slow" -> slow,
    "white" -> white,
    "black" -> black,
    "puzzle" -> puzzle) ::: pools.toList.map {
      case (id, perf) => s"$id pool" -> perf
    }

  def pool(key: String) = pools get key getOrElse Perf.default

  def standardAndPools = standard :: pools.values.toList

  def inShort = perfs map {
    case (name, perf) => s"$name:${perf.intRating}"
  } mkString ", "
}

case object Perfs {

  val default = {
    val p = Perf.default
    Perfs(p, p, p, p, p, p, p, p, Map.empty)
  }

  val titles = Map(
    "bullet" -> "Very fast games: less than 3 minutes",
    "blitz" -> "Fast games: less than 8 minutes",
    "slow" -> "Slow games: more than 8 minutes",
    "standard" -> "Standard rules of chess",
    "chess960" -> "Chess960 variant",
    "white" -> "With white pieces",
    "black" -> "With black pieces",
    "puzzle" -> "Training puzzles")

  def variantLens(variant: Variant): Option[Perfs => Perf] = variant match {
    case Variant.Standard     => Some(perfs => perfs.standard)
    case Variant.Chess960     => Some(perfs => perfs.chess960)
    case Variant.FromPosition => none
    case Variant.Center       => none
  }

  def poolLens(poolId: Option[String]): Perfs => Option[Perf] =
    (perfs: Perfs) => poolId flatMap perfs.pools.get

  private def PerfsBSONHandler = new BSON[Perfs] {

    implicit def perfHandler = Perf.tube.handler
    import BSON.Map._

    def reads(r: BSON.Reader): Perfs = {
      def perf(key: String) = r.getO[Perf](key) getOrElse Perf.default
      Perfs(
        standard = perf("standard"),
        chess960 = perf("chess960"),
        bullet = perf("bullet"),
        blitz = perf("blitz"),
        slow = perf("slow"),
        white = perf("white"),
        black = perf("black"),
        puzzle = perf("puzzle"),
        pools = r.getO[Map[String, Perf]]("pools") getOrElse Map.empty)
    }

    def writes(w: BSON.Writer, o: Perfs) = BSONDocument(
      "standard" -> o.standard,
      "chess960" -> o.chess960,
      "bullet" -> o.bullet,
      "blitz" -> o.blitz,
      "slow" -> o.slow,
      "white" -> o.white,
      "black" -> o.black,
      "puzzle" -> o.puzzle,
      "pools" -> o.pools)
  }

  lazy val tube = lila.db.BsTube(PerfsBSONHandler)
}
