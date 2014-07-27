package lila.user

import reactivemongo.bson.BSONDocument

import chess.{ Variant, Speed }
import lila.db.BSON
import lila.rating.Perf

case class Perfs(
    standard: Perf,
    chess960: Perf,
    kingOfTheHill: Perf,
    bullet: Perf,
    blitz: Perf,
    slow: Perf,
    puzzle: Perf,
    pools: Map[String, Perf]) {

  def perfs = List(
    "standard" -> standard,
    "chess960" -> chess960,
    "kingOfTheHill" -> kingOfTheHill,
    "bullet" -> bullet,
    "blitz" -> blitz,
    "slow" -> slow,
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
    Perfs(p, p, p, p, p, p, p, Map.empty)
  }

  val titles = Map(
    "bullet" -> "Very fast games: less than 3 minutes",
    "blitz" -> "Fast games: less than 8 minutes",
    "slow" -> "Slow games: more than 8 minutes",
    "standard" -> "Standard rules of chess",
    "chess960" -> "Chess960 variant",
    "kingOfTheHill" -> "King Of The Hill variant",
    "puzzle" -> "Training puzzles")

  def variantLens(variant: Variant): Option[Perfs => Perf] = variant match {
    case Variant.Standard      => Some(_.standard)
    case Variant.Chess960      => Some(_.chess960)
    case Variant.KingOfTheHill => Some(_.kingOfTheHill)
    case Variant.FromPosition  => none
  }

  def speedLens(speed: Speed): Perfs => Perf = speed match {
    case Speed.Bullet                 => perfs => perfs.bullet
    case Speed.Blitz                  => perfs => perfs.blitz
    case Speed.Slow | Speed.Unlimited => perfs => perfs.slow
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
        kingOfTheHill = perf("koth"),
        bullet = perf("bullet"),
        blitz = perf("blitz"),
        slow = perf("slow"),
        puzzle = perf("puzzle"),
        pools = r.getO[Map[String, Perf]]("pools") getOrElse Map.empty)
    }

    private def notNew(p: Perf) = p.nb > 0 option p

    def writes(w: BSON.Writer, o: Perfs) = BSONDocument(
      "standard" -> notNew(o.standard),
      "chess960" -> notNew(o.chess960),
      "koth" -> notNew(o.kingOfTheHill),
      "bullet" -> notNew(o.bullet),
      "blitz" -> notNew(o.blitz),
      "slow" -> notNew(o.slow),
      "puzzle" -> notNew(o.puzzle),
      "pools" -> o.pools.flatMap {
        case (k, r) => notNew(r) map (k -> _)
      }.toMap)
  }

  lazy val tube = lila.db.BsTube(PerfsBSONHandler)
}
