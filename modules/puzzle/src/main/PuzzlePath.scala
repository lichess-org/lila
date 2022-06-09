package lila.puzzle

import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.user.User
import org.joda.time.DateTime

object PuzzlePath {

  val sep = '|'

  case class Id(value: String) {

    val parts = value split sep

    private[puzzle] def tier = PuzzleTier.from(~parts.lift(1))

    def angle = PuzzleAngle.findOrMix(~parts.headOption)
  }

  implicit val pathIdIso = lila.common.Iso.string[Id](Id.apply, _.value)
}

final private class PuzzlePathApi(colls: PuzzleColls)(implicit ec: ExecutionContext) {

  import BsonHandlers._
  import PuzzlePath._

  def nextFor(
      user: User,
      angle: PuzzleAngle,
      tier: PuzzleTier,
      difficulty: PuzzleDifficulty,
      previousPaths: Set[Id],
      compromise: Int = 0
  ): Fu[Option[Id]] = {
    val actualTier =
      if (tier == PuzzleTier.Top && PuzzleDifficulty.isExtreme(difficulty)) PuzzleTier.Good
      else tier
    colls
      .path {
        _.aggregateOne() { framework =>
          import framework._
          val rating     = user.perfs.puzzle.glicko.intRating + difficulty.ratingDelta
          val ratingFlex = (100 + math.abs(1500 - rating) / 4) * compromise.atMost(4)
          Match(
            select(angle, actualTier, (rating - ratingFlex) to (rating + ratingFlex)) ++
              ((compromise != 5 && previousPaths.nonEmpty) ?? $doc("_id" $nin previousPaths))
          ) -> List(
            Sample(1),
            Project($id(true))
          )
        }.dmap(_.flatMap(_.getAsOpt[Id]("_id")))
      }
      .flatMap {
        case Some(path) => fuccess(path.some)
        case _ if actualTier == PuzzleTier.Top =>
          nextFor(user, angle, PuzzleTier.Good, difficulty, previousPaths)
        case _ if actualTier == PuzzleTier.Good && compromise == 2 =>
          nextFor(user, angle, PuzzleTier.All, difficulty, previousPaths, compromise = 1)
        case _ if compromise < 5 =>
          nextFor(user, angle, actualTier, difficulty, previousPaths, compromise + 1)
        case _ => fuccess(none)
      }
  }.mon(_.puzzle.path.nextFor(angle.key, tier.key, difficulty.key, previousPaths.size, compromise))

  def select(angle: PuzzleAngle, tier: PuzzleTier, rating: Range) = $doc(
    "min" $lte f"${angle.key}${sep}${tier}${sep}${rating.max}%04d",
    "max" $gte f"${angle.key}${sep}${tier}${sep}${rating.min}%04d"
  )

  def isStale = colls.path(_.primitiveOne[Long]($empty, "gen")).map {
    _.fold(true)(_ < DateTime.now.minusDays(1).getMillis)
  }

}
