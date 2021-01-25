package lila.storm

import reactivemongo.api.bson.BSONNull
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.AsyncColl
import lila.db.dsl._
import lila.memo.CacheApi
import lila.puzzle.PuzzleColls

final class StormSelector(colls: PuzzleColls, cacheApi: CacheApi)(implicit ec: ExecutionContext) {

  import StormBsonHandlers._

  def apply: Fu[List[StormPuzzle]] = current.get {}

  private val poolSize = 120
  private val theme    = lila.puzzle.PuzzleTheme.mix.key.value
  private val tier     = lila.puzzle.PuzzleTier.Good.key

  private val ratings       = (1000 to 2650 by 150).toList
  private val ratingBuckets = ratings.size

  private val current = cacheApi.unit[List[StormPuzzle]] {
    _.refreshAfterWrite(6 seconds)
      .buildAsyncFuture { _ =>
        colls
          .path {
            _.aggregateList(poolSize) { framework =>
              import framework._
              Facet(
                ratings.map { rating =>
                  rating.toString -> List(
                    Match(
                      $doc(
                        "min" $lte f"${theme}_${tier}_${rating}%04d",
                        "max" $gt f"${theme}_${tier}_${rating}%04d"
                      )
                    ),
                    Project($doc("_id" -> false, "ids" -> true)),
                    Sample(1),
                    UnwindField("ids"),
                    Sample((poolSize * 5) / ratingBuckets),
                    Group(BSONNull)("ids" -> PushField("ids"))
                  )
                }
              ) -> List(
                Project($doc("all" -> $doc("$setUnion" -> ratings.map(r => s"$$$r")))),
                UnwindField("all"),
                UnwindField("all.ids"),
                Project($doc("id" -> "$all.ids")),
                PipelineOperator(
                  $doc(
                    "$lookup" -> $doc(
                      "from" -> colls.puzzle.name.value,
                      "as"   -> "puzzle",
                      "let"  -> $doc("id" -> "$id"),
                      "pipeline" -> $arr(
                        $doc(
                          "$match" -> $doc(
                            "$expr" -> $doc(
                              "$and" -> $arr(
                                $doc("$eq" -> $arr("$_id", "$$id")),
                                $doc("$lt" -> $arr("$glicko.d", 90)),
                                $doc(
                                  "$regexMatch" -> $doc(
                                    "input" -> "$fen",
                                    "regex" -> {
                                      if (scala.util.Random.nextBoolean()) " w " else " b "
                                    }
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
                ),
                UnwindField("puzzle"),
                ReplaceRootField("puzzle"),
                Sample(poolSize),
                Sort(Ascending("rating"))
              )
            }.map { docs =>
              docs.flatMap(StormPuzzleBSONReader.readOpt)
            }
          }
          .mon(_.storm.selector.time)
          .logTime("selector")
          .addEffect { puzzles =>
            monitor(puzzles.toVector)
          }
      }
  }

  private def monitor(puzzles: Vector[StormPuzzle]): Unit = {
    val nb = puzzles.size
    lila.mon.storm.selector.count.record(nb)
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
