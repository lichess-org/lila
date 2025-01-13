package lila.storm

import chess.IntRating

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.puzzle.PuzzleColls

/* The difficulty of storm should remain constant!
 * Be very careful when adjusting the selector.
 * Use the grafana average rating per slice chart.
 */
final class StormSelector(colls: PuzzleColls, cacheApi: CacheApi)(using Executor):

  import StormBsonHandlers.given
  import lila.puzzle.PuzzlePath.sep

  def apply: Fu[List[StormPuzzle]] = current.get {}

  private val theme        = lila.puzzle.PuzzleTheme.mix.key
  private val tier         = lila.puzzle.PuzzleTier.good.key
  private val maxDeviation = 85

  /* for path boundaries:
   * 800,  900,  1000, 1100, 1200, 1270, 1340, 1410, 1480, 1550, 1620,
   * 1690, 1760, 1830, 1900, 2000, 2100, 2200, 2350, 2500, 2650, 2800
   */
  private val ratingBuckets =
    List(
      1000 -> 7,
      1150 -> 7,
      1300 -> 8,
      1450 -> 9,
      1600 -> 10,
      1750 -> 11,
      1900 -> 13,
      2050 -> 15,
      2199 -> 17,
      2349 -> 19,
      2499 -> 21
    )
  private val poolSize = ratingBuckets.foldLeft(0) { case (acc, (_, nb)) =>
    acc + nb
  }

  private val current = cacheApi.unit[List[StormPuzzle]]:
    _.refreshAfterWrite(6.seconds).buildAsyncFuture: _ =>
      colls
        .path:
          _.aggregateList(poolSize): framework =>
            import framework.*
            val fenColorRegex = $doc:
              "$regexMatch" -> $doc(
                "input" -> "$fen",
                "regex" -> { if scala.util.Random.nextBoolean() then " w " else " b " }
              )
            Facet(
              ratingBuckets.map: (rating, nbPuzzles) =>
                rating.toString -> List(
                  Match:
                    $doc(
                      "min".$lte(f"${theme}${sep}${tier}${sep}${rating}%04d"),
                      "max".$gte(f"${theme}${sep}${tier}${sep}${rating}%04d")
                    )
                  ,
                  Sample(1),
                  Project($doc("_id" -> false, "ids" -> true)),
                  UnwindField("ids"),
                  // ensure we have enough after filtering deviation & color
                  Sample(nbPuzzles * 7),
                  PipelineOperator:
                    $lookup.pipelineFull(
                      from = colls.puzzle.name.value,
                      as = "puzzle",
                      let = $doc("id" -> "$ids"),
                      pipe = List(
                        $doc:
                          "$match" -> $expr:
                            $and(
                              $doc("$eq"  -> $arr("$_id", "$$id")),
                              $doc("$lte" -> $arr("$glicko.d", maxDeviation)),
                              fenColorRegex
                            )
                        ,
                        $doc:
                          "$project" -> $doc(
                            "fen"    -> true,
                            "line"   -> true,
                            "rating" -> $doc("$toInt" -> "$glicko.r")
                          )
                      )
                    )
                  ,
                  UnwindField("puzzle"),
                  Sample(nbPuzzles),
                  ReplaceRootField("puzzle")
                )
            ) -> List(
              Project($doc("all" -> $doc("$setUnion" -> ratingBuckets.map(r => s"$$${r._1}")))),
              UnwindField("all"),
              ReplaceRootField("all"),
              Sort(Ascending("rating")),
              Limit(poolSize)
            )
          .map:
            _.flatMap(puzzleReader.readOpt)
        .mon(_.storm.selector.time)
        .addEffect: puzzles =>
          monitor(puzzles.toVector, poolSize)

  private def monitor(puzzles: Vector[StormPuzzle], poolSize: Int): Unit =
    val nb = puzzles.size
    lila.mon.storm.selector.count.record(nb)
    if nb < poolSize * 0.9 then logger.warn(s"Selector wanted $poolSize puzzles, only got $nb")
    if nb > 1 then
      val rest = puzzles.toVector.drop(1)
      scalalib.Maths.mean(IntRating.raw(rest.map(_.rating))).foreach { r =>
        lila.mon.storm.selector.rating.record(r.toInt)
      }
      (0 to poolSize by 10).foreach { i =>
        val slice = rest.drop(i).take(10)
        scalalib.Maths.mean(IntRating.raw(slice.map(_.rating))).foreach { r =>
          lila.mon.storm.selector.ratingSlice(i).record(r.toInt)
        }
      }
