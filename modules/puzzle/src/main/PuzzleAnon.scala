package lila.puzzle

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.ThreadLocalRandom
import lila.db.dsl._
import lila.memo.CacheApi
import lila.rating.{ Perf, PerfType }
import lila.user.{ User, UserRepo }

final class PuzzleAnon(colls: PuzzleColls, cacheApi: CacheApi)(implicit ec: ExecutionContext) {

  import BsonHandlers._

  def getOne: Fu[Option[Puzzle]] = pool get {} map ThreadLocalRandom.oneOf

  private val poolSize = 50

  private val pool = cacheApi.unit[Vector[Puzzle]] {
    _.refreshAfterWrite(1 minute)
      .buildAsyncFuture { _ =>
        colls.path {
          _.aggregateList(poolSize) { framework =>
            import framework._
            Match(
              $doc(
                "tier" -> "top",
                "min" $gt 1200,
                "max" $lt 1500
              )
            ) -> List(
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
