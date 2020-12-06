package lila.puzzle

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.memo.CacheApi
import lila.user.User

private object PuzzlePath {

  case class Id(value: String) {

    val parts = value split '_'

    def tier = PuzzleTier.from(~parts.lift(1))

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
      previousPaths: Set[PuzzlePath.Id],
      compromise: Int = 0
  ): Fu[Option[PuzzlePath.Id]] =
    colls.path {
      _.aggregateOne() { framework =>
        import framework._
        val rating = user.perfs.puzzle.glicko.intRating
        val ratingDelta = compromise match {
          case 0 => 0
          case 1 => 300
          case 2 => 800
          case _ => 2000
        }
        Match(
          $doc(
            "min" $lte f"${theme}_${tier}_${rating + ratingDelta}%04d",
            "max" $gt f"${theme}_${tier}_${rating - ratingDelta}%04d"
          )
        ) -> List(
          Sample(1),
          Project($id(true))
        )
      }.dmap(_.flatMap(_.getAsOpt[PuzzlePath.Id]("_id")))
    } flatMap {
      case Some(path)                  => fuccess(path.some)
      case _ if tier == PuzzleTier.Top => nextFor(user, theme, PuzzleTier.Good, previousPaths)
      case _ if tier == PuzzleTier.Good && compromise == 2 =>
        nextFor(user, theme, PuzzleTier.All, previousPaths, compromise = 1)
      case _ if compromise < 4 => nextFor(user, theme, tier, previousPaths, compromise + 1)
      case _                   => fuccess(none)
    }

  private val countByThemeCache =
    cacheApi.unit[Map[PuzzleTheme.Key, Int]] {
      _.refreshAfterWrite(10 minutes)
        .buildAsyncFuture { _ =>
          colls.path {
            _.aggregateList(Int.MaxValue) { framework =>
              import framework._
              Match($doc("tier" -> "all")) -> List(
                GroupField("theme")(
                  "count" -> SumField("length")
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
