package lila.user

import chess.Speed
import org.joda.time.DateTime

import lila.common.Heapsort.implicits._
import lila.db.BSON
import lila.rating.{ Glicko, Perf, PerfType }

case class Perfs(
    standard: Perf,
    chess960: Perf,
    kingOfTheHill: Perf,
    threeCheck: Perf,
    antichess: Perf,
    atomic: Perf,
    horde: Perf,
    racingKings: Perf,
    crazyhouse: Perf,
    ultraBullet: Perf,
    bullet: Perf,
    blitz: Perf,
    rapid: Perf,
    classical: Perf,
    correspondence: Perf,
    puzzle: Perf,
    storm: Perf.Storm,
    racer: Perf.Racer,
    streak: Perf.Streak
) {

  def perfs =
    List(
      "standard"       -> standard,
      "chess960"       -> chess960,
      "kingOfTheHill"  -> kingOfTheHill,
      "threeCheck"     -> threeCheck,
      "antichess"      -> antichess,
      "atomic"         -> atomic,
      "horde"          -> horde,
      "racingKings"    -> racingKings,
      "crazyhouse"     -> crazyhouse,
      "ultraBullet"    -> ultraBullet,
      "bullet"         -> bullet,
      "blitz"          -> blitz,
      "rapid"          -> rapid,
      "classical"      -> classical,
      "correspondence" -> correspondence,
      "puzzle"         -> puzzle
    )

  def bestPerf: Option[(PerfType, Perf)] = {
    val ps = PerfType.nonPuzzle map { pt =>
      pt -> apply(pt)
    }
    val minNb = math.max(1, ps.foldLeft(0)(_ + _._2.nb) / 10)
    ps.foldLeft(none[(PerfType, Perf)]) {
      case (ro, p) if p._2.nb >= minNb =>
        ro.fold(p.some) { r =>
          Some(if (p._2.intRating > r._2.intRating) p else r)
        }
      case (ro, _) => ro
    }
  }

  implicit private val ratingOrdering = Ordering.by[(PerfType, Perf), Int](_._2.intRating)

  def bestPerfs(nb: Int): List[(PerfType, Perf)] = {
    val ps = PerfType.nonPuzzle map { pt =>
      pt -> apply(pt)
    }
    val minNb = math.max(1, ps.foldLeft(0)(_ + _._2.nb) / 15)
    ps.filter(p => p._2.nb >= minNb).topN(nb)
  }

  def bestPerfType: Option[PerfType] = bestPerf.map(_._1)

  def bestRating: Int = bestRatingIn(PerfType.leaderboardable)

  def bestStandardRating: Int = bestRatingIn(PerfType.standard)

  def bestRatingIn(types: List[PerfType]): Int = {
    val ps = types map apply match {
      case Nil => List(standard)
      case x   => x
    }
    val minNb = ps.foldLeft(0)(_ + _.nb) / 10
    ps.foldLeft(none[Int]) {
      case (ro, p) if p.nb >= minNb =>
        ro.fold(p.intRating.some) { r =>
          Some(if (p.intRating > r) p.intRating else r)
        }
      case (ro, _) => ro
    } | Perf.default.intRating
  }

  def bestRatingInWithMinGames(types: List[PerfType], nbGames: Int): Option[Int] =
    types.map(apply).foldLeft(none[Int]) {
      case (ro, p) if p.nb >= nbGames && ro.fold(true)(_ < p.intRating) => p.intRating.some
      case (ro, _)                                                      => ro
    }

  def bestProgress: Int = bestProgressIn(PerfType.leaderboardable)

  def bestProgressIn(types: List[PerfType]): Int =
    types.foldLeft(0) { case (max, t) =>
      val p = apply(t).progress
      if (p > max) p else max
    }

  lazy val perfsMap: Map[String, Perf] = Map(
    "chess960"       -> chess960,
    "kingOfTheHill"  -> kingOfTheHill,
    "threeCheck"     -> threeCheck,
    "antichess"      -> antichess,
    "atomic"         -> atomic,
    "horde"          -> horde,
    "racingKings"    -> racingKings,
    "crazyhouse"     -> crazyhouse,
    "ultraBullet"    -> ultraBullet,
    "bullet"         -> bullet,
    "blitz"          -> blitz,
    "rapid"          -> rapid,
    "classical"      -> classical,
    "correspondence" -> correspondence,
    "puzzle"         -> puzzle
  )

  def ratingMap: Map[String, Int] = perfsMap.view.mapValues(_.intRating).toMap

  def ratingOf(pt: String): Option[Int] = perfsMap get pt map (_.intRating)

  def apply(key: String): Option[Perf] = perfsMap get key

  def apply(perfType: PerfType): Perf =
    perfType match {
      case PerfType.Standard       => standard
      case PerfType.UltraBullet    => ultraBullet
      case PerfType.Bullet         => bullet
      case PerfType.Blitz          => blitz
      case PerfType.Rapid          => rapid
      case PerfType.Classical      => classical
      case PerfType.Correspondence => correspondence
      case PerfType.Chess960       => chess960
      case PerfType.KingOfTheHill  => kingOfTheHill
      case PerfType.ThreeCheck     => threeCheck
      case PerfType.Antichess      => antichess
      case PerfType.Atomic         => atomic
      case PerfType.Horde          => horde
      case PerfType.RacingKings    => racingKings
      case PerfType.Crazyhouse     => crazyhouse
      case PerfType.Puzzle         => puzzle
    }

  def inShort =
    perfs map { case (name, perf) =>
      s"$name:${perf.intRating}"
    } mkString ", "

  def updateStandard =
    copy(
      standard = {
        val subs = List(bullet, blitz, rapid, classical, correspondence).filterNot(_.provisional)
        subs.maxByOption(_.latest.fold(0L)(_.getMillis)).flatMap(_.latest).fold(standard) { date =>
          val nb = subs.map(_.nb).sum
          val glicko = Glicko(
            rating = subs.map(s => s.glicko.rating * (s.nb / nb.toDouble)).sum,
            deviation = subs.map(s => s.glicko.deviation * (s.nb / nb.toDouble)).sum,
            volatility = subs.map(s => s.glicko.volatility * (s.nb / nb.toDouble)).sum
          )
          Perf(
            glicko = glicko,
            nb = nb,
            recent = Nil,
            latest = date.some
          )
        }
      }
    )

  def latest: Option[DateTime] =
    perfsMap.values.flatMap(_.latest).foldLeft(none[DateTime]) {
      case (None, date)                          => date.some
      case (Some(acc), date) if date isAfter acc => date.some
      case (acc, _)                              => acc
    }

  def dubiousPuzzle = {
    puzzle.glicko.rating > 3000 && !standard.glicko.establishedIntRating.exists(_ > 2100) ||
    puzzle.glicko.rating > 2500 && !standard.glicko.establishedIntRating.exists(_ > 1800)
  }
}

case object Perfs {

  val default = {
    val p = Perf.default
    Perfs(
      p,
      p,
      p,
      p,
      p,
      p,
      p,
      p,
      p,
      p,
      p,
      p,
      p,
      p,
      p,
      p,
      Perf.Storm.default,
      Perf.Racer.default,
      Perf.Streak.default
    )
  }

  val defaultManaged = {
    val managed       = Perf.defaultManaged
    val managedPuzzle = Perf.defaultManagedPuzzle
    default.copy(
      standard = managed,
      bullet = managed,
      blitz = managed,
      rapid = managed,
      classical = managed,
      correspondence = managed,
      puzzle = managedPuzzle
    )
  }

  val defaultBot = {
    val bot = Perf.defaultBot
    default.copy(
      standard = bot,
      bullet = bot,
      blitz = bot,
      rapid = bot,
      classical = bot,
      correspondence = bot
    )
  }

  def variantLens(variant: chess.variant.Variant): Option[Perfs => Perf] =
    variant match {
      case chess.variant.Standard      => Some(_.standard)
      case chess.variant.Chess960      => Some(_.chess960)
      case chess.variant.KingOfTheHill => Some(_.kingOfTheHill)
      case chess.variant.ThreeCheck    => Some(_.threeCheck)
      case chess.variant.Antichess     => Some(_.antichess)
      case chess.variant.Atomic        => Some(_.atomic)
      case chess.variant.Horde         => Some(_.horde)
      case chess.variant.RacingKings   => Some(_.racingKings)
      case chess.variant.Crazyhouse    => Some(_.crazyhouse)
      case _                           => none
    }

  def speedLens(speed: Speed): Perfs => Perf =
    speed match {
      case Speed.Bullet         => perfs => perfs.bullet
      case Speed.Blitz          => perfs => perfs.blitz
      case Speed.Rapid          => perfs => perfs.rapid
      case Speed.Classical      => perfs => perfs.classical
      case Speed.Correspondence => perfs => perfs.correspondence
      case Speed.UltraBullet    => perfs => perfs.ultraBullet
    }

  val perfsBSONHandler = new BSON[Perfs] {

    implicit def perfHandler = Perf.perfBSONHandler

    def reads(r: BSON.Reader): Perfs = {
      @inline def perf(key: String) = r.getO[Perf](key) getOrElse Perf.default
      Perfs(
        standard = perf("standard"),
        chess960 = perf("chess960"),
        kingOfTheHill = perf("kingOfTheHill"),
        threeCheck = perf("threeCheck"),
        antichess = perf("antichess"),
        atomic = perf("atomic"),
        horde = perf("horde"),
        racingKings = perf("racingKings"),
        crazyhouse = perf("crazyhouse"),
        ultraBullet = perf("ultraBullet"),
        bullet = perf("bullet"),
        blitz = perf("blitz"),
        rapid = perf("rapid"),
        classical = perf("classical"),
        correspondence = perf("correspondence"),
        puzzle = perf("puzzle"),
        storm = r.getO[Perf.Storm]("storm") getOrElse Perf.Storm.default,
        racer = r.getO[Perf.Racer]("racer") getOrElse Perf.Racer.default,
        streak = r.getO[Perf.Streak]("streak") getOrElse Perf.Streak.default
      )
    }

    private def notNew(p: Perf): Option[Perf] = p.nonEmpty option p

    def writes(w: BSON.Writer, o: Perfs) =
      reactivemongo.api.bson.BSONDocument(
        "standard"       -> notNew(o.standard),
        "chess960"       -> notNew(o.chess960),
        "kingOfTheHill"  -> notNew(o.kingOfTheHill),
        "threeCheck"     -> notNew(o.threeCheck),
        "antichess"      -> notNew(o.antichess),
        "atomic"         -> notNew(o.atomic),
        "horde"          -> notNew(o.horde),
        "racingKings"    -> notNew(o.racingKings),
        "crazyhouse"     -> notNew(o.crazyhouse),
        "ultraBullet"    -> notNew(o.ultraBullet),
        "bullet"         -> notNew(o.bullet),
        "blitz"          -> notNew(o.blitz),
        "rapid"          -> notNew(o.rapid),
        "classical"      -> notNew(o.classical),
        "correspondence" -> notNew(o.correspondence),
        "puzzle"         -> notNew(o.puzzle),
        "storm"          -> (o.storm.nonEmpty option o.storm),
        "racer"          -> (o.racer.nonEmpty option o.racer),
        "streak"         -> (o.streak.nonEmpty option o.streak)
      )
  }

  case class Leaderboards(
      ultraBullet: List[User.LightPerf],
      bullet: List[User.LightPerf],
      blitz: List[User.LightPerf],
      rapid: List[User.LightPerf],
      classical: List[User.LightPerf],
      crazyhouse: List[User.LightPerf],
      chess960: List[User.LightPerf],
      kingOfTheHill: List[User.LightPerf],
      threeCheck: List[User.LightPerf],
      antichess: List[User.LightPerf],
      atomic: List[User.LightPerf],
      horde: List[User.LightPerf],
      racingKings: List[User.LightPerf]
  )

  val emptyLeaderboards = Leaderboards(Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil)
}
