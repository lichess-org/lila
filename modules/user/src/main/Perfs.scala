package lila.user

import reactivemongo.bson.BSONDocument

import chess.{ Variant, Speed }
import lila.db.BSON
import lila.rating.{ Perf, PerfType, Glicko }

case class Perfs(
    standard: Perf,
    chess960: Perf,
    kingOfTheHill: Perf,
    threeCheck: Perf,
    antichess: Perf,
    atomicChess: Perf,
    bullet: Perf,
    blitz: Perf,
    classical: Perf,
    correspondence: Perf,
    puzzle: Perf) {

  def perfs = List(
    "standard" -> standard,
    "chess960" -> chess960,
    "kingOfTheHill" -> kingOfTheHill,
    "threeCheck" -> threeCheck,
    "antichess" -> antichess,
    "atomicChess" -> atomicChess,
    "bullet" -> bullet,
    "blitz" -> blitz,
    "classical" -> classical,
    "correspondence" -> correspondence,
    "puzzle" -> puzzle)

  def bestPerf: Option[(PerfType, Perf)] = {
    val ps = PerfType.nonPuzzle map { pt => pt -> apply(pt) }
    val minNb = math.max(1, ps.foldLeft(0)(_ + _._2.nb) / 10)
    ps.foldLeft(none[(PerfType, Perf)]) {
      case (ro, p) if p._2.nb >= minNb => ro.fold(p.some) { r =>
        Some(if (p._2.intRating > r._2.intRating) p else r)
      }
      case (ro, _) => ro
    }
  }

  def bestRating: Int = {
    val ps = List(bullet, blitz, classical, correspondence, chess960, kingOfTheHill, threeCheck, antichess, atomicChess)
    val minNb = ps.foldLeft(0)(_ + _.nb) / 10
    ps.foldLeft(none[Int]) {
      case (ro, p) if p.nb >= minNb => ro.fold(p.intRating.some) { r =>
        Some(if (p.intRating > r) p.intRating else r)
      }
      case (ro, _) => ro
    } | Perf.default.intRating
  }

  lazy val perfsMap: Map[String, Perf] = Map(
    "chess960" -> chess960,
    "kingOfTheHill" -> kingOfTheHill,
    "threeCheck" -> threeCheck,
    "antichess" -> antichess,
    "atomicChess" -> atomicChess,
    "bullet" -> bullet,
    "blitz" -> blitz,
    "classical" -> classical,
    "correspondence" -> correspondence,
    "puzzle" -> puzzle)

  def ratingMap: Map[String, Int] = perfsMap mapValues (_.intRating)

  def ratingOf(pt: String): Option[Int] = perfsMap get pt map (_.intRating)

  def apply(key: String): Option[Perf] = perfsMap get key

  def apply(perfType: PerfType): Perf = perfType match {
    case PerfType.Standard       => standard
    case PerfType.Bullet         => bullet
    case PerfType.Blitz          => blitz
    case PerfType.Classical      => classical
    case PerfType.Correspondence => correspondence
    case PerfType.Chess960       => chess960
    case PerfType.KingOfTheHill  => kingOfTheHill
    case PerfType.ThreeCheck     => threeCheck
    case PerfType.Antichess      => antichess
    case PerfType.AtomicChess    => atomicChess
    case PerfType.Puzzle         => puzzle
  }

  def inShort = perfs map {
    case (name, perf) => s"$name:${perf.intRating}"
  } mkString ", "

  def updateStandard = copy(
    standard = {
      val subs = List(bullet, blitz, classical, correspondence)
      subs.maxBy(_.latest.fold(0l)(_.getMillis)).latest.fold(standard) { date =>
        val nb = subs.map(_.nb).sum
        val glicko = Glicko(
          rating = subs.map(s => s.glicko.rating * (s.nb / nb.toDouble)).sum,
          deviation = subs.map(s => s.glicko.deviation * (s.nb / nb.toDouble)).sum,
          volatility = subs.map(s => s.glicko.volatility * (s.nb / nb.toDouble)).sum)
        Perf(
          glicko = glicko,
          nb = nb,
          recent = Nil,
          latest = date.some)
      }
    }
  )
}

case object Perfs {

  val default = {
    val p = Perf.default
    Perfs(p, p, p, p, p, p, p, p, p, p, p)
  }

  def variantLens(variant: Variant): Option[Perfs => Perf] = variant match {
    case Variant.Standard      => Some(_.standard)
    case Variant.Chess960      => Some(_.chess960)
    case Variant.KingOfTheHill => Some(_.kingOfTheHill)
    case Variant.ThreeCheck    => Some(_.threeCheck)
    case Variant.Antichess     => Some(_.antichess)
    case Variant.AtomicChess   => Some(_.atomicChess)
    case Variant.FromPosition  => none
  }

  def speedLens(speed: Speed): Perfs => Perf = speed match {
    case Speed.Bullet => perfs => perfs.bullet
    case Speed.Blitz => perfs => perfs.blitz
    case Speed.Classical => perfs => perfs.classical
    case Speed.Correspondence => perfs => perfs.correspondence
  }

  private def PerfsBSONHandler = new BSON[Perfs] {

    implicit def perfHandler = Perf.tube.handler
    import BSON.Map._

    def reads(r: BSON.Reader): Perfs = {
      def perf(key: String) = r.getO[Perf](key) getOrElse Perf.default
      Perfs(
        standard = perf("standard"),
        chess960 = perf("chess960"),
        kingOfTheHill = perf("kingOfTheHill"),
        threeCheck = perf("threeCheck"),
        antichess = perf("antichess"),
        atomicChess = perf("atomicChess"),
        bullet = perf("bullet"),
        blitz = perf("blitz"),
        classical = perf("classical"),
        correspondence = perf("correspondence"),
        puzzle = perf("puzzle"))
    }

    private def notNew(p: Perf) = p.nb > 0 option p

    def writes(w: BSON.Writer, o: Perfs) = BSONDocument(
      "standard" -> notNew(o.standard),
      "chess960" -> notNew(o.chess960),
      "kingOfTheHill" -> notNew(o.kingOfTheHill),
      "threeCheck" -> notNew(o.threeCheck),
      "antichess" -> notNew(o.antichess),
      "atomicChess" -> notNew(o.atomicChess),
      "bullet" -> notNew(o.bullet),
      "blitz" -> notNew(o.blitz),
      "classical" -> notNew(o.classical),
      "correspondence" -> notNew(o.correspondence),
      "puzzle" -> notNew(o.puzzle))
  }

  lazy val tube = lila.db.BsTube(PerfsBSONHandler)
}
