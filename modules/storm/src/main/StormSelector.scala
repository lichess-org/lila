package lila.storm

import chess.IntRating
import scalalib.ThreadLocalRandom

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.puzzle.PuzzleColls
import lila.common.BatchProvider

/* The difficulty of storm should remain constant!
 * Be very careful when adjusting the selector.
 * Use the grafana average rating per slice chart.
 */
final class StormSelector(colls: PuzzleColls, cacheApi: CacheApi)(using Executor, Scheduler):

  import StormBsonHandlers.given
  import lila.puzzle.PuzzlePath.sep

  private type PuzzleSet = List[StormPuzzle]

  def apply: Fu[PuzzleSet] = current.get {}

  private val theme = lila.puzzle.PuzzleTheme.mix.key
  private val tier = lila.puzzle.PuzzleTier.good.key
  private val maxDeviation = 85
  private val setsPerAggregation = 20

  /* for path boundaries:
   * 800,  900,  1000, 1100, 1200, 1270, 1340, 1410, 1480, 1550, 1620,
   * 1690, 1760, 1830, 1900, 2000, 2100, 2200, 2350, 2500, 2650, 2800
   */
  private val ratingBuckets = List(
    1050 -> 7,
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
  private val setSize = ratingBuckets.map(_._2).sum

  private val batchProvider =
    BatchProvider[PuzzleSet]("stormSelector", timeout = 15.seconds)(() => aggregateMultipleSets)

  private val current = cacheApi.unit[PuzzleSet]:
    _.refreshAfterWrite(7.seconds).buildAsyncFuture(_ => batchProvider.one)

  private var aggregationColor = chess.Color.White

  private def aggregateMultipleSets: Fu[List[PuzzleSet]] =
    aggregationColor = !aggregationColor
    val nbSets = if lila.common.Uptime.startedSinceMinutes(2) then setsPerAggregation else 1
    colls
      .path:
        _.aggregateList(setSize * nbSets, _.sec): framework =>
          import framework.*
          Facet(
            ratingBuckets.map: (rating, nbPuzzles) =>
              val target = f"${theme}${sep}${tier}${sep}${rating}%04d"
              rating.toString -> List(
                Match($doc("min".$lte(target), "max".$gte(target))),
                Sample(nbSets),
                Project($doc("_id" -> false, "ids" -> true)),
                UnwindField("ids"),
                // ensure we have enough after filtering deviation & color
                Sample(nbPuzzles * nbSets * 7),
                PipelineOperator(withPuzzlePipeline(aggregationColor)),
                UnwindField("puzzle"),
                Sample(nbPuzzles * nbSets),
                ReplaceRootField("puzzle")
              )
          ) -> List(
            Project($doc("all" -> $doc("$setUnion" -> ratingBuckets.map(r => s"$$${r._1}")))),
            UnwindField("all"),
            ReplaceRootField("all"),
            Sort(Ascending("rating")),
            Limit(setSize * nbSets)
          )
        .map:
          _.flatMap(puzzleReader.readOpt)
        .map:
          _.grouped(nbSets).toList.map(ThreadLocalRandom.shuffle).transpose
      .logTimeIfGt(s"storm selector x$nbSets", 8.seconds)
      .mon(_.storm.selector.time)
      .addEffect:
        _.foreach: puzzles =>
          monitor(puzzles.toVector)

  private def withPuzzlePipeline(color: chess.Color) =
    $lookup.pipelineFull(
      from = colls.puzzle.name.value,
      as = "puzzle",
      let = $doc("id" -> "$ids"),
      pipe = List(
        $doc:
          "$match" -> $expr:
            $and(
              $doc("$eq" -> $arr("$_id", "$$id")),
              $doc("$lte" -> $arr("$glicko.d", maxDeviation)),
              $doc(
                "$regexMatch" -> $doc(
                  "input" -> "$fen",
                  "regex" -> { if color.white then " w " else " b " }
                )
              )
            )
        ,
        $doc:
          "$project" -> $doc(
            "fen" -> true,
            "line" -> true,
            "rating" -> $doc("$toInt" -> "$glicko.r")
          )
      )
    )

  private def monitor(puzzles: Vector[StormPuzzle]): Unit =
    val nb = puzzles.size
    lila.mon.storm.selector.count.record(nb)
    if nb < setSize * 0.9 then logger.warn(s"Selector wanted $setSize puzzles, only got $nb")
    if nb > 1 then
      val rest = puzzles.toVector.drop(1)
      scalalib.Maths.mean(IntRating.raw(rest.map(_.rating))).foreach { r =>
        lila.mon.storm.selector.rating.record(r.toInt)
      }
      (0 to setSize by 10).foreach { i =>
        val slice = rest.drop(i).take(10)
        scalalib.Maths.mean(IntRating.raw(slice.map(_.rating))).foreach { r =>
          lila.mon.storm.selector.ratingSlice(i).record(r.toInt)
        }
      }
