package lila.puzzle

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.memo.CacheApi
import lila.user.User

private object PuzzlePath {

  case class Id(value: String) {

    val parts = value split '_'

    private[puzzle] def tier = PuzzleTier.from(~parts.lift(1))

    def theme = PuzzleTheme.findOrAny(~parts.headOption).key
  }

  implicit val pathIdIso = lila.common.Iso.string[Id](Id.apply, _.value)
}

final private class PuzzlePathApi(
    colls: PuzzleColls
)(implicit ec: ExecutionContext) {

  import BsonHandlers._

  def nextFor(
      user: User,
      theme: PuzzleTheme.Key,
      tier: PuzzleTier,
      difficulty: PuzzleDifficulty,
      previousPaths: Set[PuzzlePath.Id],
      compromise: Int = 0
  ): Fu[Option[PuzzlePath.Id]] = {
    val actualTier =
      if (tier == PuzzleTier.Top && PuzzleDifficulty.isExtreme(difficulty)) PuzzleTier.Good
      else tier
    colls
      .path {
        _.aggregateOne() { framework =>
          import framework._
          val rating          = user.perfs.puzzle.glicko.intRating + difficulty.ratingDelta
          val ratingDeltaBase = 100 + math.abs(1500 - rating) / 4
          val ratingDelta     = ratingDeltaBase * compromise
          Match(
            select(theme, actualTier, (rating - ratingDelta) to (rating + ratingDelta)) ++
              (previousPaths.nonEmpty ?? $doc("_id" $nin previousPaths))
          ) -> List(
            Sample(1),
            Project($id(true))
          )
        }.dmap(_.flatMap(_.getAsOpt[PuzzlePath.Id]("_id")))
      }
      .flatMap {
        case Some(path) => fuccess(path.some)
        case _ if actualTier == PuzzleTier.Top =>
          nextFor(user, theme, PuzzleTier.Good, difficulty, previousPaths)
        case _ if actualTier == PuzzleTier.Good && compromise == 2 =>
          nextFor(user, theme, PuzzleTier.All, difficulty, previousPaths, compromise = 1)
        case _ if compromise < 4 =>
          nextFor(user, theme, actualTier, difficulty, previousPaths, compromise + 1)
        case _ => fuccess(none)
      }
  }.mon(_.puzzle.path.nextFor(theme.value, tier.key, difficulty.key, previousPaths.size, compromise))

  def select(theme: PuzzleTheme.Key, tier: PuzzleTier, rating: Range) = $doc(
    "min" $lte f"${theme}_${tier}_${rating.max}%04d",
    "max" $gt f"${theme}_${tier}_${rating.min}%04d"
  )
}
