package lila.user

import chess.Speed

import lila.common.Heapsort.*
import lila.rating.{ Glicko, Perf, PerfType }

case class UserPerfs(
    id: UserId,
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
):

  def perfs: List[(PerfType, Perf)] = List(
    PerfType.Standard       -> standard,
    PerfType.Chess960       -> chess960,
    PerfType.KingOfTheHill  -> kingOfTheHill,
    PerfType.ThreeCheck     -> threeCheck,
    PerfType.Antichess      -> antichess,
    PerfType.Atomic         -> atomic,
    PerfType.Horde          -> horde,
    PerfType.RacingKings    -> racingKings,
    PerfType.Crazyhouse     -> crazyhouse,
    PerfType.UltraBullet    -> ultraBullet,
    PerfType.Bullet         -> bullet,
    PerfType.Blitz          -> blitz,
    PerfType.Rapid          -> rapid,
    PerfType.Classical      -> classical,
    PerfType.Correspondence -> correspondence,
    PerfType.Puzzle         -> puzzle
  )

  def typed(pt: PerfType) = Perf.Typed(apply(pt), pt)

  def best8Perfs: List[PerfType]    = UserPerfs.firstRow ::: bestOf(UserPerfs.secondRow, 4)
  def best6Perfs: List[PerfType]    = UserPerfs.firstRow ::: bestOf(UserPerfs.secondRow, 2)
  def best4Perfs: List[PerfType]    = UserPerfs.firstRow
  def bestAny3Perfs: List[PerfType] = bestOf(UserPerfs.firstRow ::: UserPerfs.secondRow, 3)
  def bestPerf: Option[PerfType]    = bestOf(UserPerfs.firstRow ::: UserPerfs.secondRow, 1).headOption
  private def bestOf(perfTypes: List[PerfType], nb: Int) = perfTypes
    .sortBy: pt =>
      -(apply(pt).nb * PerfType.totalTimeRoughEstimation.get(pt).so(_.roundSeconds))
    .take(nb)
  def hasEstablishedRating(pt: PerfType) = apply(pt).established

  def bestRatedPerf: Option[Perf.Typed] =
    val ps    = perfs.filter(_._1 != PerfType.Puzzle)
    val minNb = math.max(1, ps.foldLeft(0)(_ + _._2.nb) / 10)
    ps
      .foldLeft(none[(PerfType, Perf)]):
        case (ro, p) if p._2.nb >= minNb =>
          ro.fold(p.some): r =>
            Some(if p._2.intRating > r._2.intRating then p else r)
        case (ro, _) => ro
      .map(Perf.typed)

  private given Ordering[(PerfType, Perf)] = Ordering.by[(PerfType, Perf), Int](_._2.intRating.value)

  def bestPerfs(nb: Int): List[Perf.Typed] =
    val ps = PerfType.nonPuzzle.map: pt =>
      pt -> apply(pt)
    val minNb = math.max(1, ps.foldLeft(0)(_ + _._2.nb) / 15)
    ps.filter(p => p._2.nb >= minNb).topN(nb).map(Perf.typed)

  def bestRating: IntRating = bestRatingIn(PerfType.leaderboardable)

  def bestStandardRating: IntRating = bestRatingIn(PerfType.standard)

  def bestRatingIn(types: List[PerfType]): IntRating =
    val ps = types map apply match
      case Nil => List(standard)
      case x   => x
    val minNb = ps.foldLeft(0)(_ + _.nb) / 10
    ps.foldLeft(none[IntRating]):
      case (ro, p) if p.nb >= minNb =>
        ro.fold(p.intRating) { r =>
          if p.intRating > r then p.intRating else r
        }.some
      case (ro, _) => ro
    .getOrElse(Perf.default.intRating)

  def bestPerf(types: List[PerfType]): Perf =
    types
      .map(apply)
      .foldLeft(none[Perf]):
        case (ro, p) if ro.forall(_.intRating < p.intRating) => p.some
        case (ro, _)                                         => ro
      .getOrElse(Perf.default)

  def bestRatingInWithMinGames(types: List[PerfType], nbGames: Int): Option[IntRating] =
    types
      .map(apply)
      .foldLeft(none[IntRating]):
        case (ro, p) if p.nb >= nbGames && ro.forall(_ < p.intRating) => p.intRating.some
        case (ro, _)                                                  => ro

  def bestProgress: IntRatingDiff = bestProgressIn(PerfType.leaderboardable)

  def bestProgressIn(types: List[PerfType]): IntRatingDiff =
    types.foldLeft(IntRatingDiff(0)): (max, t) =>
      val p = apply(t).progress
      if p > max then p else max

  lazy val perfsMap: Map[Perf.Key, Perf] = Map(
    Perf.Key("chess960")       -> chess960,
    Perf.Key("kingOfTheHill")  -> kingOfTheHill,
    Perf.Key("threeCheck")     -> threeCheck,
    Perf.Key("antichess")      -> antichess,
    Perf.Key("atomic")         -> atomic,
    Perf.Key("horde")          -> horde,
    Perf.Key("racingKings")    -> racingKings,
    Perf.Key("crazyhouse")     -> crazyhouse,
    Perf.Key("ultraBullet")    -> ultraBullet,
    Perf.Key("bullet")         -> bullet,
    Perf.Key("blitz")          -> blitz,
    Perf.Key("rapid")          -> rapid,
    Perf.Key("classical")      -> classical,
    Perf.Key("correspondence") -> correspondence,
    Perf.Key("puzzle")         -> puzzle
  )

  def ratingOf(pt: Perf.Key): Option[IntRating] = perfsMap get pt map (_.intRating)

  def apply(key: Perf.Key): Option[Perf] = perfsMap get key

  def apply(perfType: PerfType): Perf = perfType match
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

  def inShort = perfs
    .map: (name, perf) =>
      s"$name:${perf.intRating}"
    .mkString(", ")

  def updateStandard =
    copy(
      standard =
        val subs = List(bullet, blitz, rapid, classical, correspondence).filter(_.provisional.no)
        subs.maxByOption(_.latest.fold(0L)(_.toMillis)).flatMap(_.latest).fold(standard) { date =>
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
    )

  def latest: Option[Instant] =
    perfsMap.values
      .flatMap(_.latest)
      .foldLeft(none[Instant]):
        case (None, date)                          => date.some
        case (Some(acc), date) if date isAfter acc => date.some
        case (acc, _)                              => acc

  def dubiousPuzzle = UserPerfs.dubiousPuzzle(puzzle, standard)

object UserPerfs:

  given UserIdOf[User] = _.id

  def dubiousPuzzle(puzzle: Perf, standard: Perf) =
    puzzle.glicko.rating > 3000 && !standard.glicko.establishedIntRating.exists(_ > 2100) ||
      puzzle.glicko.rating > 2900 && !standard.glicko.establishedIntRating.exists(_ > 2000) ||
      puzzle.glicko.rating > 2700 && !standard.glicko.establishedIntRating.exists(_ > 1900) ||
      puzzle.glicko.rating > 2500 && !standard.glicko.establishedIntRating.exists(_ > 1800)

  def default(id: UserId) =
    val p = Perf.default
    UserPerfs(
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
      Perf.Storm.default,
      Perf.Racer.default,
      Perf.Streak.default
    )

  def defaultManaged(id: UserId) =
    val managed       = Perf.defaultManaged
    val managedPuzzle = Perf.defaultManagedPuzzle
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
    val bot = Perf.defaultBot
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

  def idPerfReader(pt: PerfType) = BSONDocumentReader.option[(UserId, Perf)] { doc =>
    import Perf.given
    for
      id   <- doc.getAsOpt[UserId]("_id")
      perf <- doc.getAsOpt[Perf](pt.key.value)
    yield (id, perf)
  }

  given BSONDocumentHandler[UserPerfs] = new BSON[UserPerfs]:

    import Perf.given

    def reads(r: BSON.Reader): UserPerfs =
      inline def perf(key: String) = r.getO[Perf](key) getOrElse Perf.default
      UserPerfs(
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
        storm = r.getO[Perf.Storm]("storm") getOrElse Perf.Storm.default,
        racer = r.getO[Perf.Racer]("racer") getOrElse Perf.Racer.default,
        streak = r.getO[Perf.Streak]("streak") getOrElse Perf.Streak.default
      )

    private inline def notNew(p: Perf): Option[Perf] = p.nonEmpty option p

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

  private val firstRow: List[PerfType] =
    List(PerfType.Bullet, PerfType.Blitz, PerfType.Rapid, PerfType.Classical)
  private val secondRow: List[PerfType] = List(
    PerfType.Correspondence,
    PerfType.UltraBullet,
    PerfType.Crazyhouse,
    PerfType.Chess960,
    PerfType.KingOfTheHill,
    PerfType.ThreeCheck,
    PerfType.Antichess,
    PerfType.Atomic,
    PerfType.Horde,
    PerfType.RacingKings
  )
