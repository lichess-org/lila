package lila.puzzle

import org.apache.http.protocol
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.memo.CacheApi
import reactivemongo.api.ReadPreference

case class PuzzleStreak(ids: List[Puzzle.Id])

final class PuzzleStreakApi(colls: PuzzleColls, cacheApi: CacheApi)(implicit ec: ExecutionContext) {

  import BsonHandlers._

  def apply: Fu[Option[(PuzzleStreak, Puzzle)]] = current.get {} flatMap {
    case id :: ids =>
      colls.puzzle(_.byId[Puzzle](id.value)) map {
        _ map { p => PuzzleStreak(id :: ids) -> p }
      }
    case _ => fuccess(none)
  }

  /* for path boundaries:
   * 800,  900,  1000, 1100, 1200, 1270, 1340, 1410, 1480, 1550, 1620,
   * 1690, 1760, 1830, 1900, 2000, 2100, 2200, 2350, 2500, 2650, 2800
   */
  private val highestBoundary = 2800
  private val buckets = List(
    1000            -> 3,
    1150            -> 4,
    1300            -> 5,
    1450            -> 6,
    1600            -> 7,
    1750            -> 8,
    1900            -> 10,
    2050            -> 13,
    2199            -> 15,
    2349            -> 17,
    2499            -> 19,
    2649            -> 21,
    2799            -> 23,
    highestBoundary -> 25
  )
  private val poolSize     = buckets.map(_._2).sum
  private val theme        = lila.puzzle.PuzzleTheme.mix.key.value
  private val tier         = lila.puzzle.PuzzleTier.Good.key
  private val maxDeviation = 110

  private val current = cacheApi.unit[List[Puzzle.Id]] {
    _.refreshAfterWrite(30 seconds)
      .buildAsyncFuture { _ =>
        colls
          .path {
            _.aggregateList(poolSize) { framework =>
              import framework._
              Facet(
                buckets.map { case (rating, nbPuzzles) =>
                  rating.toString -> List(
                    Match(
                      $doc(
                        "min" $lte f"${theme}_${tier}_${rating}%04d",
                        "max" $gte f"${theme}_${tier}_${if (rating == highestBoundary) 9999 else rating}%04d"
                      )
                    ),
                    Sample(if (rating > 2300) 5 else 1),
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
                                    $doc("$lte" -> $arr("$glicko.d", maxDeviation))
                                  )
                                )
                              )
                            ),
                            $doc(
                              "$project" -> $doc(
                                "glicko.r" -> true
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
              _.flatMap(_.getAsOpt[Puzzle.Id]("_id"))
            }
          }
          .mon(_.streak.selector.time)
          .addEffect(monitor)
      }
  }

  private def monitor(ids: List[Puzzle.Id]): Unit =
    colls
      .puzzle(_.byIds[Puzzle](ids.map(_.value), ReadPreference.secondaryPreferred))
      .map(_.sortBy(_.glicko.rating)) foreach { puzzles =>
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
