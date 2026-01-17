package lila.puzzle

import scalalib.Iso

import lila.db.dsl.{ *, given }

object PuzzlePath:

  val sep = '|'

  case class Id(value: String):

    val parts = value.split(sep)

    private[puzzle] def tier = PuzzleTier.from(~parts.lift(1))

    def angle = PuzzleAngle.findOrMix(~parts.headOption)

  given Iso.StringIso[Id] = Iso.string(Id.apply, _.value)

final private class PuzzlePathApi(colls: PuzzleColls)(using Executor):

  import BsonHandlers.given
  import PuzzlePath.*

  /* What stresses out the puzzle db
   *
{"t":{"$date":"2025-05-30T07:11:05.938+00:00"},"s":"I",  "c":"COMMAND",  "id":51803,   "ctx":"conn156","msg":"Slow query","attr":{"type":"command","ns":"puzzler.puzzle2_path","command":{"aggregate":"puzzle2_path","pipeline":[{"$match":{"min":{"$lte":"mix|top|1214"},"max":{"$gte":"mix|top|1214"}}},{"$sample":{"size":1}},{"$project":{"_id":true}}],"
explain":false,"allowDiskUse":false,"cursor":{"batchSize":101},"bypassDocumentValidation":false,"readConcern":{"level":"local"},"$db":"puzzler","$readPreference":{"mode":"primary"}},"planSummary":"IXSCAN { min: 1, max: -1 }","planningTimeMicros":81,"keysExamined":18388,"docsExamined":30,"cursorExhausted":true,"numYields":18,"nreturned":1,"queryHas
h":"5B7ADA38","planCacheKey":"7FF0C349","queryFramework":"classic","reslen":286,"locks":{"FeatureCompatibilityVersion":{"acquireCount":{"r":20}},"Global":{"acquireCount":{"r":20}}},"readConcern":{"level":"local","provenance":"clientSupplied"},"writeConcern":{"w":"majority","wtimeout":0,"provenance":"implicitDefault"},"storage":{},"cpuNanos":445997
50,"remote":"172.16.0.45:39998","protocol":"op_msg","durationMillis":44}}
   */
  def nextFor(requester: String)(
      angle: PuzzleAngle,
      tier: PuzzleTier,
      difficulty: PuzzleDifficulty,
      previousPaths: Set[Id],
      compromise: Int = 0
  )(using perf: Perf): Fu[Option[Id]] = {
    val actualTier =
      if tier == PuzzleTier.top && PuzzleDifficulty.isExtreme(difficulty)
      then PuzzleTier.good
      else tier
    colls
      .path:
        _.aggregateOne(_.pri): framework =>
          import framework.*
          val rating = perf.glicko.intRating.map(_ + difficulty.ratingDelta)
          val ratingFlex = (100 + math.abs(1500 - rating.value) / 4) * compromise.atMost(4)
          Match(
            select(angle, actualTier, (rating.value - ratingFlex) to (rating.value + ratingFlex)) ++
              ((compromise != 5 && previousPaths.nonEmpty).so($doc("_id".$nin(previousPaths))))
          ) -> List(
            Sample(1),
            Project($id(true))
          )
        .dmap(_.flatMap(_.getAsOpt[Id]("_id")))
      .flatMap:
        case Some(path) => fuccess(path.some)
        case _ if actualTier == PuzzleTier.top =>
          nextFor(requester)(angle, PuzzleTier.good, difficulty, previousPaths)
        case _ if actualTier == PuzzleTier.good && compromise == 2 =>
          nextFor(requester)(angle, PuzzleTier.all, difficulty, previousPaths, compromise = 1)
        case _ if compromise < 5 =>
          nextFor(requester)(angle, actualTier, difficulty, previousPaths, compromise + 1)
        case _ => fuccess(none)
  }.mon:
    _.puzzle.nextPathFor(angle.categ, requester)

  def select(angle: PuzzleAngle, tier: PuzzleTier, rating: Range) = $doc(
    "min".$lte(f"${angle.key}${sep}${tier}${sep}${rating.max}%04d"),
    "max".$gte(f"${angle.key}${sep}${tier}${sep}${rating.min}%04d")
  )

  def isStale = colls
    .path(_.primitiveOne[Long]($empty, "gen"))
    .map:
      _.forall(_ < nowInstant.minusDays(1).toMillis)
