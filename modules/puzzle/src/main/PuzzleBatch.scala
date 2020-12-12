package lila.puzzle

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.ThreadLocalRandom
import lila.db.dsl._
import lila.memo.CacheApi
import lila.rating.{ Perf, PerfType }
import lila.user.{ User, UserRepo }

// mobile app BC
final class PuzzleBatch(colls: PuzzleColls, anonApi: PuzzleAnon, pathApi: PuzzlePathApi)(implicit
    ec: ExecutionContext
) {

  import BsonHandlers._

  def nextFor(user: Option[User], nb: Int): Fu[Vector[Puzzle]] = (nb > 0) ?? {
    user match {
      case None => anonApi.getBatchFor(nb)
      case Some(user) =>
        val tier =
          if (user.perfs.puzzle.intRating < 1200 || user.perfs.puzzle.intRating > 1800) PuzzleTier.Good
          else PuzzleTier.Top
        pathApi.nextFor(user, PuzzleTheme.mix.key, PuzzleTier.Good, PuzzleDifficulty.Normal, Set.empty) orFail
          s"No puzzle path for ${user.id} $tier" flatMap { pathId =>
            colls.path {
              _.aggregateList(nb) { framework =>
                import framework._
                Match($id(pathId)) -> List(
                  Project($doc("puzzleId" -> "$ids", "_id" -> false)),
                  Unwind("puzzleId"),
                  Sample(nb),
                  PipelineOperator(
                    $doc(
                      "$lookup" -> $doc(
                        "from"         -> colls.puzzle.name.value,
                        "localField"   -> "puzzleId",
                        "foreignField" -> "_id",
                        "as"           -> "puzzle"
                      )
                    )
                  ),
                  PipelineOperator(
                    $doc(
                      "$replaceWith" -> $doc("$arrayElemAt" -> $arr("$puzzle", 0))
                    )
                  )
                )
              }.map {
                _.view.flatMap(PuzzleBSONReader.readOpt).toVector
              }
            }
          }
    }
  }
}
