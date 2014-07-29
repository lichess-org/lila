package lila.user

import reactivemongo.bson.BSONDocument

import chess.{ Variant, Speed }
import lila.db.BSON
import lila.rating.{ Perf, Glicko }

case class Perfs(
    standard: Perf,
    chess960: Perf,
    kingOfTheHill: Perf,
    bullet: Perf,
    blitz: Perf,
    classical: Perf,
    puzzle: Perf,
    pools: Map[String, Perf]) {

  def perfs = List(
    "standard" -> standard,
    "chess960" -> chess960,
    "kingOfTheHill" -> kingOfTheHill,
    "bullet" -> bullet,
    "blitz" -> blitz,
    "classical" -> classical,
    "puzzle" -> puzzle) ::: pools.toList.map {
      case (id, perf) => s"$id pool" -> perf
    }

  def pool(key: String) = pools get key getOrElse Perf.default

  def standardAndPools = standard :: pools.values.toList

  def inShort = perfs map {
    case (name, perf) => s"$name:${perf.intRating}"
  } mkString ", "

  def updateStandard = copy(
    standard = {
      val subs = List(bullet, blitz, classical)
      subs.maxBy(_.latest.fold(0l)(_.getMillis)).latest.fold(standard) { date =>
        val nb = subs.map(_.nb).sum
        val glicko = Glicko(
          rating = subs.map(s => s.glicko.rating * (s.nb / nb.toDouble)).sum,
          deviation = subs.map(s => s.glicko.deviation * (s.nb / nb.toDouble)).sum,
          volatility = subs.map(s => s.glicko.volatility * (s.nb / nb.toDouble)).sum)
        Perf(
          glicko = glicko,
          nb = nb,
          recent = (glicko.intRating :: standard.recent) take Perf.recentMaxSize,
          latest = date.some)
      }
    }
  )
}

case object Perfs {

  val default = {
    val p = Perf.default
    Perfs(p, p, p, p, p, p, p, Map.empty)
  }

  val names = Map(
    "bullet" -> "Bullet",
    "blitz" -> "Blitz",
    "classical" -> "Classical",
    "standard" -> "Standard",
    "chess960" -> "Chess960",
    "kingOfTheHill" -> "King of the Hill",
    "puzzle" -> "Training")

  val titles = Map(
    "bullet" -> "Very fast games: less than 3 minutes",
    "blitz" -> "Fast games: less than 8 minutes",
    "classical" -> "Classical games: more than 8 minutes",
    "standard" -> "Standard rules of chess",
    "chess960" -> "Chess960 variant",
    "kingOfTheHill" -> "King of the Hill variant",
    "puzzle" -> "Training puzzles")

  def variantLens(variant: Variant): Option[Perfs => Perf] = variant match {
    case Variant.Standard      => Some(_.standard)
    case Variant.Chess960      => Some(_.chess960)
    case Variant.KingOfTheHill => Some(_.kingOfTheHill)
    case Variant.FromPosition  => none
  }

  def speedLens(speed: Speed): Perfs => Perf = speed match {
    case Speed.Bullet => perfs => perfs.bullet
    case Speed.Blitz => perfs => perfs.blitz
    case Speed.Classical | Speed.Unlimited => perfs => perfs.classical
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
        kingOfTheHill = perf("kingOfTheHill"),
        bullet = perf("bullet"),
        blitz = perf("blitz"),
        classical = perf("classical"),
        puzzle = perf("puzzle"),
        pools = r.getO[Map[String, Perf]]("pools") getOrElse Map.empty)
    }

    private def notNew(p: Perf) = p.nb > 0 option p

    def writes(w: BSON.Writer, o: Perfs) = BSONDocument(
      "standard" -> notNew(o.standard),
      "chess960" -> notNew(o.chess960),
      "kingOfTheHill" -> notNew(o.kingOfTheHill),
      "bullet" -> notNew(o.bullet),
      "blitz" -> notNew(o.blitz),
      "classical" -> notNew(o.classical),
      "puzzle" -> notNew(o.puzzle),
      "pools" -> o.pools.flatMap {
        case (k, r) => notNew(r) map (k -> _)
      }.toMap)
  }

  lazy val tube = lila.db.BsTube(PerfsBSONHandler)
}
