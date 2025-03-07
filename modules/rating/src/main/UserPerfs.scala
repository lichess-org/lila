package lila.rating

import chess.{ Speed, IntRating }
import chess.rating.IntRatingDiff
import scalalib.HeapSort.*

import lila.core.perf.{ KeyedPerf, Perf, PuzPerf, UserPerfs }
import lila.core.user.LightPerf
import lila.rating.PerfExt.*

object UserPerfsExt:

  extension (p: UserPerfs)

    def perfsList: List[(PerfKey, Perf)] = List(
      PerfKey.ultraBullet    -> p.ultraBullet,
      PerfKey.bullet         -> p.bullet,
      PerfKey.blitz          -> p.blitz,
      PerfKey.rapid          -> p.rapid,
      PerfKey.classical      -> p.classical,
      PerfKey.correspondence -> p.correspondence,
      PerfKey.chess960       -> p.chess960,
      PerfKey.kingOfTheHill  -> p.kingOfTheHill,
      PerfKey.threeCheck     -> p.threeCheck,
      PerfKey.antichess      -> p.antichess,
      PerfKey.atomic         -> p.atomic,
      PerfKey.horde          -> p.horde,
      PerfKey.racingKings    -> p.racingKings,
      PerfKey.crazyhouse     -> p.crazyhouse,
      PerfKey.puzzle         -> p.puzzle
    )

    def best8Perfs: List[PerfKey]    = UserPerfs.firstRow ::: bestOf(UserPerfs.secondRow, 4)
    def best6Perfs: List[PerfKey]    = UserPerfs.firstRow ::: bestOf(UserPerfs.secondRow, 2)
    def best4Perfs: List[PerfKey]    = UserPerfs.firstRow
    def bestAny3Perfs: List[PerfKey] = bestOf(UserPerfs.firstRow ::: UserPerfs.secondRow, 3)
    def bestPerf: Option[PerfKey]    = bestOf(UserPerfs.firstRow ::: UserPerfs.secondRow, 1).headOption
    private def bestOf(keys: List[PerfKey], nb: Int) = keys
      .sortBy: pk =>
        -(p(pk).nb * lila.rating.PerfType.totalTimeRoughEstimation.get(PerfType(pk)).so(_.roundSeconds.value))
      .take(nb)
    def hasEstablishedRating(pk: PerfKey) = p(pk).established

    def bestRatedPerf: Option[KeyedPerf] =
      val ps    = perfsList.filter(p => p._1 != PerfKey.puzzle && p._1 != PerfKey.standard)
      val minNb = math.max(1, ps.foldLeft(0)(_ + _._2.nb) / 10)
      ps
        .foldLeft(none[(PerfKey, Perf)]):
          case (ro, p) if p._2.nb >= minNb =>
            ro.fold(p.some): r =>
              Some(if p._2.intRating > r._2.intRating then p else r)
          case (ro, _) => ro
        .map(KeyedPerf.apply)

    def bestPerfs(nb: Int): List[KeyedPerf] =
      val ps = PerfType.nonPuzzle.map: pt =>
        pt.key -> apply(pt)
      val minNb = math.max(1, ps.foldLeft(0)(_ + _._2.nb) / 15)
      ps.filter(p => p._2.nb >= minNb).topN(nb).map(KeyedPerf.apply)

    def bestRating: IntRating = bestRatingIn(PerfType.leaderboardable)

    def bestStandardRating: IntRating = bestRatingIn(PerfType.standard)

    def bestRatingIn(types: List[PerfKey]): IntRating =
      val ps = types.map(p(_)) match
        case Nil => List(p.standard)
        case x   => x
      val minNb = ps.foldLeft(0)(_ + _.nb) / 10
      ps.foldLeft(none[IntRating]):
        case (ro, p) if p.nb >= minNb =>
          ro.fold(p.intRating) { r =>
            if p.intRating > r then p.intRating else r
          }.some
        case (ro, _) => ro
      .getOrElse(lila.rating.Perf.default.intRating)

    def bestPerf(types: List[PerfKey]): Perf =
      types
        .map(p(_))
        .foldLeft(none[Perf]):
          case (ro, p) if ro.forall(_.intRating < p.intRating) => p.some
          case (ro, _)                                         => ro
        .getOrElse(lila.rating.Perf.default)

    def bestRatingInWithMinGames(types: List[PerfKey], nbGames: Int): Option[IntRating] =
      types
        .map(p(_))
        .foldLeft(none[IntRating]):
          case (ro, p) if p.nb >= nbGames && ro.forall(_ < p.intRating) => p.intRating.some
          case (ro, _)                                                  => ro

    def bestProgress: IntRatingDiff = bestProgressIn(PerfType.leaderboardable)

    def bestProgressIn(types: List[PerfKey]): IntRatingDiff =
      types.foldLeft[IntRatingDiff](IntRatingDiff(0)): (max, t) =>
        val p = apply(t).progress
        if p > max then p else max

    def ratingOf(pt: PerfKey): IntRating = p(pt).intRating

    def apply(perfType: PerfType): Perf = p(perfType.key)

    def latest: Option[Instant] =
      p.perfsList
        .flatMap(_._2.latest)
        .foldLeft(none[Instant]):
          case (None, date)                           => date.some
          case (Some(acc), date) if date.isAfter(acc) => date.some
          case (acc, _)                               => acc

    def dubiousPuzzle = UserPerfs.dubiousPuzzle(p)

  private given [A]: Ordering[(A, Perf)] = Ordering.by[(A, Perf), Int](_._2.intRating.value)

object UserPerfs:

  def dubiousPuzzle(perfs: UserPerfs): Boolean = dubiousPuzzle(perfs.puzzle, perfs.standard)

  def dubiousPuzzle(puzzle: Perf, standard: Perf): Boolean =
    puzzle.glicko.rating > 3000 && !standard.glicko.establishedIntRating.exists(_ > IntRating(2100)) ||
      puzzle.glicko.rating > 2900 && !standard.glicko.establishedIntRating.exists(_ > IntRating(2000)) ||
      puzzle.glicko.rating > 2700 && !standard.glicko.establishedIntRating.exists(_ > IntRating(1900)) ||
      puzzle.glicko.rating > 2500 && !standard.glicko.establishedIntRating.exists(_ > IntRating(1800))

  private val puzPerfDefault = PuzPerf(0, 0)

  def default(id: UserId) =
    val p = lila.rating.Perf.default
    new UserPerfs(
      id,
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
      puzPerfDefault,
      puzPerfDefault,
      puzPerfDefault
    )
  def defaultManaged(id: UserId) =
    val managed       = lila.rating.Perf.defaultManaged
    val managedPuzzle = lila.rating.Perf.defaultManagedPuzzle
    default(id).copy(
      standard = managed,
      bullet = managed,
      blitz = managed,
      rapid = managed,
      classical = managed,
      correspondence = managed,
      puzzle = managedPuzzle
    )

  def defaultBot(id: UserId) =
    val bot = lila.rating.Perf.defaultBot
    default(id).copy(
      standard = bot,
      bullet = bot,
      blitz = bot,
      rapid = bot,
      classical = bot,
      correspondence = bot,
      chess960 = bot
    )

  def variantLens(variant: chess.variant.Variant): Option[UserPerfs => Perf] =
    variant match
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

  def speedLens(speed: Speed): UserPerfs => Perf = perfs =>
    speed match
      case Speed.Bullet         => perfs.bullet
      case Speed.Blitz          => perfs.blitz
      case Speed.Rapid          => perfs.rapid
      case Speed.Classical      => perfs.classical
      case Speed.Correspondence => perfs.correspondence
      case Speed.UltraBullet    => perfs.ultraBullet

  import lila.db.BSON
  import lila.db.dsl.{ *, given }
  import reactivemongo.api.bson.*

  def idPerfReader(pk: PerfKey) = BSONDocumentReader.option[(UserId, Perf)] { doc =>
    import lila.rating.Perf.given
    for
      id   <- doc.getAsOpt[UserId]("_id")
      perf <- doc.getAsOpt[Perf](pk.value)
    yield (id, perf)
  }

  given userPerfsHandler: BSONDocumentHandler[UserPerfs] = new BSON[UserPerfs]:

    import lila.rating.Perf.given

    def reads(r: BSON.Reader): UserPerfs =
      inline def perf(key: String) = r.getO[Perf](key).getOrElse(lila.rating.Perf.default)
      new UserPerfs(
        id = r.get[UserId]("_id"),
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
        storm = r.getD[PuzPerf]("storm", puzPerfDefault),
        racer = r.getD[PuzPerf]("racer", puzPerfDefault),
        streak = r.getD[PuzPerf]("streak", puzPerfDefault)
      )

    private inline def notNew(p: Perf): Option[Perf] = p.nonEmpty.option(p)

    def writes(w: BSON.Writer, o: UserPerfs) =
      BSONDocument(
        "id"             -> o.id,
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
        "storm"          -> o.storm.nonEmpty.option(o.storm),
        "racer"          -> o.racer.nonEmpty.option(o.racer),
        "streak"         -> o.streak.nonEmpty.option(o.streak)
      )

  case class Leaderboards(
      ultraBullet: List[LightPerf],
      bullet: List[LightPerf],
      blitz: List[LightPerf],
      rapid: List[LightPerf],
      classical: List[LightPerf],
      crazyhouse: List[LightPerf],
      chess960: List[LightPerf],
      kingOfTheHill: List[LightPerf],
      threeCheck: List[LightPerf],
      antichess: List[LightPerf],
      atomic: List[LightPerf],
      horde: List[LightPerf],
      racingKings: List[LightPerf]
  )

  val emptyLeaderboards = Leaderboards(Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil)

  private[rating] val firstRow: List[PerfKey] =
    List(PerfKey.bullet, PerfKey.blitz, PerfKey.rapid, PerfKey.classical)
  private[rating] val secondRow: List[PerfKey] = List(
    PerfKey.correspondence,
    PerfKey.ultraBullet,
    PerfKey.crazyhouse,
    PerfKey.chess960,
    PerfKey.kingOfTheHill,
    PerfKey.threeCheck,
    PerfKey.antichess,
    PerfKey.atomic,
    PerfKey.horde,
    PerfKey.racingKings
  )
