package lila.puzzle

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.memo.CacheApi.buildAsyncTimeout

case class PuzzleStreak(ids: String, first: Puzzle)

final class PuzzleStreakApi(colls: PuzzleColls, cacheApi: CacheApi)(using Executor, Scheduler):

  import BsonHandlers.given
  import lila.puzzle.PuzzlePath.sep

  def apply: Fu[Option[PuzzleStreak]] = current.get {}

  /* for path boundaries:
   * 800,  900,  1000, 1100, 1200, 1270, 1340, 1410, 1480, 1550, 1620,
   * 1690, 1760, 1830, 1900, 2000, 2100, 2200, 2350, 2500, 2650, 2800
   */
  private val buckets = List(
    1050 -> 3,
    1150 -> 4,
    1300 -> 5,
    1450 -> 6,
    1600 -> 7,
    1750 -> 8,
    1900 -> 10,
    2050 -> 13,
    2199 -> 15,
    2349 -> 17,
    2499 -> 19,
    2649 -> 21,
    2799 -> 21
  )
  private val poolSize = buckets._2F.sum
  private val theme = lila.puzzle.PuzzleTheme.mix.key

  private val current = cacheApi.unit[Option[PuzzleStreak]]:
    _.refreshAfterWrite(30.seconds).buildAsyncTimeout(20.seconds): _ =>
      colls
        .path:
          _.aggregateList(poolSize, _.sec): framework =>
            import framework.*
            Facet(
              buckets.map: (rating, nbPuzzles) =>
                val (tier, samples, deviation) =
                  if rating > 2300 then (PuzzleTier.good, 5, 110) else (PuzzleTier.top, 1, 85)
                val target = f"${theme}${sep}${tier}${sep}${rating}%04d"
                rating.toString -> List(
                  Match($doc("min".$lte(target), "max".$gte(target))),
                  Sample(samples),
                  Project($doc("_id" -> false, "ids" -> true)),
                  UnwindField("ids"),
                  // ensure we have enough after filtering deviation
                  Sample(nbPuzzles * 4),
                  PipelineOperator(
                    $lookup.simple(
                      from = colls.puzzle.name,
                      as = "puzzle",
                      local = "ids",
                      foreign = "_id",
                      pipe = List($doc("$match" -> $doc("glicko.d".$lte(deviation))))
                    )
                  ),
                  UnwindField("puzzle"),
                  Sample(nbPuzzles),
                  ReplaceRootField("puzzle")
                )
            ) -> List(
              Project($doc("all" -> $doc("$setUnion" -> buckets.map(r => s"$$${r._1}")))),
              UnwindField("all"),
              ReplaceRootField("all"),
              Sort(Ascending("glicko.r")),
              Limit(poolSize)
            )
          .map:
            _.flatMap(puzzleReader.readOpt)
        .mon(_.streak.selector.time)
        .addEffect(monitor)
        .map: puzzles =>
          puzzles.headOption.map:
            PuzzleStreak(puzzles.map(_.id).mkString(" "), _)

  private def monitor(puzzles: List[Puzzle]): Unit =
    val nb = puzzles.size
    lila.mon.streak.selector.count.record(nb)
    if nb < poolSize * 0.9 then logger.warn(s"Streak selector wanted $poolSize puzzles, only got $nb")
    if nb > 1 then
      val rest = puzzles.toVector.drop(1)
      scalalib.Maths.mean(rest.map(_.glicko.intRating.value)).foreach { r =>
        lila.mon.streak.selector.rating.record(r.toInt)
      }
      (0 to poolSize by 10).foreach { i =>
        val slice = rest.drop(i).take(10)
        scalalib.Maths.mean(slice.map(_.glicko.intRating.value)).foreach { r =>
          lila.mon.streak.selector.ratingSlice(i).record(r.toInt)
        }
      }
