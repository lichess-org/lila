package lila.puzzle

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.ThreadLocalRandom
import lila.db.dsl._
import lila.memo.CacheApi
import lila.rating.{ Perf, PerfType }
import lila.user.{ User, UserRepo }

final class PuzzleAnon(colls: PuzzleColls, cacheApi: CacheApi, pathApi: PuzzlePathApi)(implicit
    ec: ExecutionContext
) {

  import BsonHandlers._

  def getOneFor(theme: PuzzleTheme.Key): Fu[Option[Puzzle]] =
    pool get theme map ThreadLocalRandom.oneOf

  private val poolSize = 50

  private val pool =
    cacheApi[PuzzleTheme.Key, Vector[Puzzle]](initialCapacity = 32, name = "puzzle.byTheme.anon") {
      _.refreshAfterWrite(2 minutes)
        .buildAsyncFuture { theme =>
          pathApi countPuzzlesByTheme theme flatMap { count =>
            val tier = if (count > 3000) PuzzleTier.Top else PuzzleTier.All
            val ratingRange: Range =
              if (count > 9000) 1200 to 1600
              else if (count > 5000) 1000 to 1800
              else 0 to 9999
            val selector =
              $doc(
                "_id" $startsWith s"${theme}_${tier}_",
                "min" $gte ratingRange.min,
                "max" $lte ratingRange.max
              )
            colls.path {
              _.aggregateList(poolSize) { framework =>
                import framework._
                Match(selector) -> List(
                  Sample(1),
                  Project($doc("puzzleId" -> "$ids", "_id" -> false)),
                  Unwind("puzzleId"),
                  Sample(poolSize),
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
