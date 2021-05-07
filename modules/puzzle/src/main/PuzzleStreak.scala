package lila.puzzle

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.memo.CacheApi

case class PuzzleStreak(ids: String, first: Puzzle)

final class PuzzleStreakApi(colls: PuzzleColls, cacheApi: CacheApi)(implicit ec: ExecutionContext) {

  import BsonHandlers._

  def apply: Fu[Option[PuzzleStreak]] = current.get {}

  /* for path boundaries:
   * 800,  900,  1000, 1100, 1200, 1270, 1340, 1410, 1480, 1550, 1620,
   * 1690, 1760, 1830, 1900, 2000, 2100, 2200, 2350, 2500, 2650, 2800
   */
  private val buckets = List(
    1000 -> 3,
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
  private val poolSize = buckets.map(_._2).sum
  private val theme    = lila.puzzle.PuzzleTheme.mix.key.value

  private val current = cacheApi.unit[Option[PuzzleStreak]] {
    _.refreshAfterWrite(30 seconds)
      .buildAsyncFuture { _ =>
        colls
          .path {
            _.aggregateList(poolSize) { framework =>
              import framework._
              Facet(
                buckets.map { case (rating, nbPuzzles) =>
                  val (tier, samples, deviation) =
                    if (rating > 2300) (PuzzleTier.Good, 5, 110) else (PuzzleTier.Top, 1, 85)
                  rating.toString -> List(
                    Match(
                      $doc(
                        "min" $lte f"${theme}_${tier}_${rating}%04d",
                        "max" $gte f"${theme}_${tier}_${rating}%04d"
                      )
                    ),
                    Sample(samples),
                    Project($doc("_id" -> false, "ids" -> true)),
                    UnwindField("ids"),
                    // ensure we have enough after filtering deviation
                    Sample(nbPuzzles * 4),
                    PipelineOperator(
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
                                    $doc("$lte" -> $arr("$glicko.d", deviation))
                                  )
                                )
                              )
                            )
                          )
                        )
                      )
                    ),
                    UnwindField("puzzle"),
                    Sample(nbPuzzles),
                    ReplaceRootField("puzzle")
                  )
                }
              ) -> List(
                Project($doc("all" -> $doc("$setUnion" -> buckets.map(r => s"$$${r._1}")))),
                UnwindField("all"),
                ReplaceRootField("all"),
                Sort(Ascending("glicko.r"))
              )
            }.map {
              _.flatMap(PuzzleBSONReader.readOpt)
            }
          }
          .mon(_.streak.selector.time)
          .addEffect(monitor)
          .map { puzzles =>
            puzzles.headOption map {
              PuzzleStreak(puzzles.map(_.id) mkString " ", _)
            }
          }
      }
  }

  private def monitor(puzzles: List[Puzzle]): Unit = {
    val nb = puzzles.size
    lila.mon.streak.selector.count.record(nb)
    if (nb < poolSize * 0.9)
      logger.warn(s"Streak selector wanted $poolSize puzzles, only got $nb")
    if (nb > 1) {
      val rest = puzzles.toVector drop 1
      lila.common.Maths.mean(rest.map(_.glicko.intRating)) foreach { r =>
        lila.mon.streak.selector.rating.record(r.toInt).unit
      }
      (0 to poolSize by 10) foreach { i =>
        val slice = rest drop i take 10
        lila.common.Maths.mean(slice.map(_.glicko.intRating)) foreach { r =>
          lila.mon.streak.selector.ratingSlice(i).record(r.toInt)
        }
      }
    }
  }
}
