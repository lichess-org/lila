package lila.puzzle

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.ThreadLocalRandom
import lila.db.dsl._
import lila.memo.CacheApi

final class PuzzleAnon(
    colls: PuzzleColls,
    cacheApi: CacheApi,
    pathApi: PuzzlePathApi,
    countApi: PuzzleCountApi
)(implicit ec: ExecutionContext) {

  import BsonHandlers._

  def getOneFor(theme: PuzzleTheme.Key): Fu[Option[Puzzle]] =
    pool
      .get(theme)
      .map(ThreadLocalRandom.oneOf)
      .mon(_.puzzle.selector.anon.time(theme.value))
      .addEffect {
        _ foreach { puzzle =>
          lila.mon.puzzle.selector.anon.vote(theme.value).record(100 + math.round(puzzle.vote * 100))
        }
      }

  def getBatchFor(nb: Int): Fu[Vector[Puzzle]] = {
    pool get PuzzleTheme.mix.key map (_ take nb)
  }.mon(_.puzzle.selector.anon.batch(nb))

  private val poolSize = 150

  private val pool =
    cacheApi[PuzzleTheme.Key, Vector[Puzzle]](initialCapacity = 64, name = "puzzle.byTheme.anon") {
      _.expireAfterWrite(1 minute)
        .buildAsyncFuture { theme =>
          countApi byTheme theme flatMap { count =>
            val tier =
              if (count > 5000) PuzzleTier.Top
              else if (count > 2000) PuzzleTier.Good
              else PuzzleTier.All
            val ratingRange: Range =
              if (count > 9000) 1300 to 1600
              else if (count > 5000) 1100 to 1800
              else 0 to 9999
            val pathSampleSize =
              if (count > 9000) 3
              else if (count > 5000) 5
              else if (count > 2000) 8
              else 15
            colls.path {
              _.aggregateList(poolSize) { framework =>
                import framework._
                Match(pathApi.select(theme, tier, ratingRange)) -> List(
                  Sample(pathSampleSize),
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
