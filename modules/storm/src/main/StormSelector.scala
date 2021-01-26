package lila.storm

import reactivemongo.api.bson.BSONNull
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.AsyncColl
import lila.db.dsl._
import lila.memo.CacheApi
import lila.puzzle.PuzzleColls

/* The difficulty of storm should remain constant!
 * Be very careful when adjusting the selector.
 * Use the grafana average rating per slice chart.
 */
final class StormSelector(colls: PuzzleColls, cacheApi: CacheApi)(implicit ec: ExecutionContext) {

  import StormBsonHandlers._

  def apply: Fu[List[StormPuzzle]] = current.get {}

  private val poolSize     = 130
  private val theme        = lila.puzzle.PuzzleTheme.mix.key.value
  private val tier         = lila.puzzle.PuzzleTier.Good.key
  private val maxDeviation = 90

  private val ratings          = (1000 to 2800 by 150).toList
  private val ratingBuckets    = ratings.size
  private val puzzlesPerBucket = poolSize / ratingBuckets

  private def puzzlesForBucket(bucket: Int) =
    if (bucket < ratingBuckets / 2) puzzlesPerBucket - 1
    else puzzlesPerBucket + 1

  private val current = cacheApi.unit[List[StormPuzzle]] {
    _.refreshAfterWrite(6 seconds)
      .buildAsyncFuture { _ =>
        colls
          .path {
            _.aggregateList(poolSize) { framework =>
              import framework._
              val lookupDoc =
                $doc(
                  "$lookup" -> $doc(
                    "from" -> colls.puzzle.name.value,
                    "as"   -> "puzzle",
                    "let"  -> $doc("id" -> "$ids"),
                    "pipeline" -> $arr(
                      $doc(
                        "$match" -> $doc(
                          "$expr" -> $doc(
                            "$and" -> $arr(
                              $doc("$eq"  -> $arr("$_id", "$$id")),
                              $doc("$lte" -> $arr("$glicko.d", maxDeviation)),
                              $doc(
                                "$regexMatch" -> $doc(
                                  "input" -> "$fen",
                                  "regex" -> { if (scala.util.Random.nextBoolean()) " w " else " b " }
                                )
                              )
                            )
                          )
                        )
                      ),
                      $doc(
                        "$project" -> $doc(
                          "fen"    -> true,
                          "line"   -> true,
                          "rating" -> $doc("$toInt" -> "$glicko.r")
                        )
                      )
                    )
                  )
                )
              Facet(
                ratings.zipWithIndex.map { case (rating, bucket) =>
                  rating.toString -> List(
                    Match(
                      $doc(
                        "min" $lte f"${theme}_${tier}_${rating}%04d",
                        "max" $gte f"${theme}_${tier}_${rating}%04d"
                      )
                    ),
                    Project($doc("_id" -> false, "ids" -> true)),
                    Sample(1),
                    UnwindField("ids"),
                    // ensure we have enough after filtering deviation & color
                    Sample(puzzlesForBucket(bucket) * 6),
                    PipelineOperator(lookupDoc),
                    UnwindField("puzzle"),
                    Sample(puzzlesForBucket(bucket)),
                    ReplaceRootField("puzzle")
                  )
                }
              ) -> List(
                Project($doc("all" -> $doc("$setUnion" -> ratings.map(r => s"$$$r")))),
                UnwindField("all"),
                ReplaceRootField("all"),
                Sort(Ascending("rating"))
              )
            }.map {
              _.flatMap(StormPuzzleBSONReader.readOpt)
            }
          }
          .mon(_.storm.selector.time)
          .logTime("selector")
          .addEffect { puzzles =>
            monitor(puzzles.toVector, poolSize)
          }
      }
  }

  private def monitor(puzzles: Vector[StormPuzzle], poolSize: Int): Unit = {
    val nb = puzzles.size
    lila.mon.storm.selector.count.record(nb)
    if (nb < poolSize)
      logger.warn(s"Selector wanted $poolSize puzzles, only got $nb")
    if (nb > 1) {
      val rest = puzzles.toVector drop 1
      lila.common.Maths.mean(rest.map(_.rating)) foreach { r =>
        lila.mon.storm.selector.rating.record(r.toInt).unit
      }
      (0 to poolSize by 10) foreach { i =>
        val slice = rest drop i take 10
        lila.common.Maths.mean(slice.map(_.rating)) foreach { r =>
          lila.mon.storm.selector.ratingSlice(i).record(r.toInt)
        }
      }
    }
  }
}
