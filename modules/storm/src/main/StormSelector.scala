package lila.storm

import scala.concurrent.duration._

import lila.db.AsyncColl
import lila.memo.CacheApi
import lila.db.dsl._
import lila.puzzle.PuzzleColls

final class StormSelector(colls: PuzzleColls, cacheApi: CacheApi) {

  val poolSize = 100

  private val current = cacheApi.unit[List[StormPuzzle]] {
    _.refreshAfterWrite(10 seconds)
      .buildAsyncFuture { _ =>
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

  def apply: Fu[List[StormPuzzle]] = current.get {}
}
