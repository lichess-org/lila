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

  def nextFor(
      angle: PuzzleAngle,
      tier: PuzzleTier,
      difficulty: PuzzleDifficulty,
      previousPaths: Set[Id],
      compromise: Int = 0
  )(using me: Me, perf: Perf): Fu[Option[Id]] = {
    val actualTier =
      if tier == PuzzleTier.top && PuzzleDifficulty.isExtreme(difficulty)
      then PuzzleTier.good
      else tier
    colls
      .path:
        _.aggregateOne(): framework =>
          import framework.*
          val rating     = perf.glicko.intRating.map(_ + difficulty.ratingDelta)
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
          nextFor(angle, PuzzleTier.good, difficulty, previousPaths)
        case _ if actualTier == PuzzleTier.good && compromise == 2 =>
          nextFor(angle, PuzzleTier.all, difficulty, previousPaths, compromise = 1)
        case _ if compromise < 5 =>
          nextFor(angle, actualTier, difficulty, previousPaths, compromise + 1)
        case _ => fuccess(none)
  }.mon:
    _.puzzle.path.nextFor(angle.categ)

  def select(angle: PuzzleAngle, tier: PuzzleTier, rating: Range) = $doc(
    "min".$lte(f"${angle.key}${sep}${tier}${sep}${rating.max}%04d"),
    "max".$gte(f"${angle.key}${sep}${tier}${sep}${rating.min}%04d")
  )

  def isStale = colls
    .path(_.primitiveOne[Long]($empty, "gen"))
    .map:
      _.forall(_ < nowInstant.minusDays(1).toMillis)
