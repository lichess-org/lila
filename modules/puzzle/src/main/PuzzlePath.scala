package lila.puzzle

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.memo.CacheApi
import lila.user.User

object PuzzlePath {

  case class Id(value: String) {

    val parts = value split '_'

    private[puzzle] def tier = PuzzleTier.from(~parts.lift(1))

    def theme = PuzzleTheme.findOrAny(~parts.headOption).key
  }

  implicit val pathIdIso = lila.common.Iso.string[Id](Id.apply, _.value)
}

final private class PuzzlePathApi(
    colls: PuzzleColls,
    cacheApi: CacheApi
)(implicit ec: ExecutionContext) {

  import BsonHandlers._

  def countsByTheme: Fu[Map[PuzzleTheme.Key, Int]] =
    countByThemeCache get {}

  def countPuzzlesByTheme(theme: PuzzleTheme.Key): Fu[Int] =
    countsByTheme dmap { _.getOrElse(theme, 0) }

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
          val rating = user.perfs.puzzle.glicko.intRating + difficulty.ratingDelta
          val ratingDelta = compromise match {
            case 0 => 0
            case 1 => 300
            case 2 => 800
            case _ => 2000
          }
          Match(select(theme, actualTier, (rating - ratingDelta) to (rating + ratingDelta))) -> List(
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
  }

  def select(theme: PuzzleTheme.Key, tier: PuzzleTier, rating: Range) = $doc(
    "min" $lte f"${theme}_${tier}_${rating.max}%04d",
    "max" $gt f"${theme}_${tier}_${rating.min}%04d"
  )

  private val countByThemeCache =
    cacheApi.unit[Map[PuzzleTheme.Key, Int]] {
      _.refreshAfterWrite(10 minutes)
        .buildAsyncFuture { _ =>
          colls.path {
            _.aggregateList(Int.MaxValue) { framework =>
              import framework._
              Match($doc("tier" -> "all", "theme" $ne PuzzleTheme.any.key)) -> List(
                GroupField("theme")(
                  "count" -> SumField("size")
                )
              )
            }.map {
              _.flatMap { obj =>
                for {
                  key   <- obj string "_id"
                  count <- obj int "count"
                } yield PuzzleTheme.Key(key) -> count
              }.toMap
            }.flatMap { themed =>
              colls.puzzle(_.countAll) map { all =>
                themed + (PuzzleTheme.any.key -> all.toInt)
              }
            }
          }
        }
    }
}
