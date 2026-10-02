package lila.rating

import chess.IntRating
import scalalib.HeapSort.*

import lila.core.perf.{ KeyedPerf, Perf, PuzPerf, UserPerfs }
import lila.core.user.LightPerf
import lila.rating.PerfExt.*

object UserPerfsExt:

  extension (ps: List[Perf]) def sumNb: Int = ps.foldMap(_.nb)

  extension (p: UserPerfs)

    def perfsList: List[(PerfKey, Perf)] = List(
      PerfKey.ultraBullet -> p.ultraBullet,
      PerfKey.bullet -> p.bullet,
      PerfKey.blitz -> p.blitz,
      PerfKey.rapid -> p.rapid,
      PerfKey.classical -> p.classical,
      PerfKey.correspondence -> p.correspondence,
      PerfKey.chess960 -> p.chess960,
      PerfKey.kingOfTheHill -> p.kingOfTheHill,
      PerfKey.threeCheck -> p.threeCheck,
      PerfKey.antichess -> p.antichess,
      PerfKey.atomic -> p.atomic,
      PerfKey.horde -> p.horde,
      PerfKey.racingKings -> p.racingKings,
      PerfKey.crazyhouse -> p.crazyhouse,
      PerfKey.puzzle -> p.puzzle
    )

    def best8Perfs: List[PerfKey] = UserPerfs.firstRow ::: bestOf(UserPerfs.secondRow, 4)
    def best6Perfs: List[PerfKey] = UserPerfs.firstRow ::: bestOf(UserPerfs.secondRow, 2)
    def best4Perfs: List[PerfKey] = UserPerfs.firstRow
    def bestAny3Perfs: List[PerfKey] = bestOf(UserPerfs.firstRow ::: UserPerfs.secondRow, 3)
    def bestPerf: Option[PerfKey] = bestOf(UserPerfs.firstRow ::: UserPerfs.secondRow, 1).headOption
    private def bestOf(keys: List[PerfKey], nb: Int) = keys
      .sortBy: pk =>
        -(p(pk).nb * lila.rating.PerfType.totalTimeRoughEstimation.get(PerfType(pk)).so(_.roundSeconds.value))
      .take(nb)
    def hasEstablishedRating(pk: PerfKey) = p(pk).established

    def bestRatedPerf: Option[KeyedPerf] =
      val ps = perfsList.filter(p => p._1 != PerfKey.puzzle && p._1 != PerfKey.standard)
      val minNb = (ps.map(_._2).sumNb / 10).atLeast(1)
      ps.filter(_._2.nb >= minNb)
        .maxByOption(_._2.intRating)
        .map(KeyedPerf.apply)

    def bestPerfs(nb: Int): List[KeyedPerf] =
      val ps = PerfType.nonPuzzle.map(pt => pt.key -> p(pt))
      val minNb = (ps.map(_._2).sumNb / 15).atLeast(1)
      ps.filter(p => p._2.nb >= minNb).topN(nb).map(KeyedPerf.apply)

    def bestRating: IntRating = bestRatingIn(PerfType.leaderboardable)

    def bestStandardRating: IntRating = bestRatingIn(PerfType.standard)

    def bestRatingIn(types: List[PerfKey]): IntRating =
      val ps = types.nonEmptyOption.map(_.map(p(_))) | List(p.standard)
      val minNb = ps.sumNb / 10
      val minGames = ps.filter(_.nb >= minNb)
      bestFromPerfs(minGames).intRating

    def bestFromPerfs(types: List[Perf]): Perf =
      types.maxByOption(_.intRating).getOrElse(lila.rating.Perf.default)

    def bestPerf(types: List[PerfKey]): Perf = bestFromPerfs(types.map(p(_)))

    def dubiousPuzzle = UserPerfs.dubiousPuzzle(p)

  private given Ordering[IntRating] = intOrdering
  private given [A]: Ordering[(A, Perf)] = Ordering.by[(A, Perf), IntRating](_._2.intRating)

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
    val managed = lila.rating.Perf.defaultManaged
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

  import lila.db.BSON
  import lila.db.dsl.given
  import reactivemongo.api.bson.*

  def idPerfReader(pk: PerfKey) = BSONDocumentReader.option[(UserId, Perf)] { doc =>
    import lila.rating.Perf.given
    for
      id <- doc.getAsOpt[UserId]("_id")
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
        "id" -> o.id,
        "standard" -> notNew(o.standard),
        "chess960" -> notNew(o.chess960),
        "kingOfTheHill" -> notNew(o.kingOfTheHill),
        "threeCheck" -> notNew(o.threeCheck),
        "antichess" -> notNew(o.antichess),
        "atomic" -> notNew(o.atomic),
        "horde" -> notNew(o.horde),
        "racingKings" -> notNew(o.racingKings),
        "crazyhouse" -> notNew(o.crazyhouse),
        "ultraBullet" -> notNew(o.ultraBullet),
        "bullet" -> notNew(o.bullet),
        "blitz" -> notNew(o.blitz),
        "rapid" -> notNew(o.rapid),
        "classical" -> notNew(o.classical),
        "correspondence" -> notNew(o.correspondence),
        "puzzle" -> notNew(o.puzzle),
        "storm" -> o.storm.nonEmpty.option(o.storm),
        "racer" -> o.racer.nonEmpty.option(o.racer),
        "streak" -> o.streak.nonEmpty.option(o.streak)
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
