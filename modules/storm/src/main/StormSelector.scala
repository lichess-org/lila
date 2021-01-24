package lila.storm

import reactivemongo.api.bson.BSONNull
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.AsyncColl
import lila.db.dsl._
import lila.memo.CacheApi
import lila.puzzle.PuzzleColls

final class StormSelector(colls: PuzzleColls, cacheApi: CacheApi)(implicit ec: ExecutionContext) {

  import BsonHandlers._

  val poolSize = 100
  val theme    = lila.puzzle.PuzzleTheme.mix.key.value
  val tier     = lila.puzzle.PuzzleTier.Good.key

  val ratings = (1000 to 2200 by 100).toList

  private val current = cacheApi.unit[List[StormPuzzle]] {
    _.refreshAfterWrite(10 seconds)
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
                        "min" $lte s"${theme}_${tier}_${rating}%04d",
                        "max" $gt s"${theme}_${tier}_${rating}%04d"
                      )
                    ),
                    Project($doc("_id" -> false, "ids" -> true)),
                    Sample(1),
                    UnwindField("ids"),
                    Sample(20),
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
                                $doc("$lt" -> $arr("$glicko.d", 100))
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
                Sample(100),
                Sort(Ascending("rating"))
              )
            }.map { docs =>
              docs.flatMap(StormPuzzleBSONReader.readOpt)
            }
          }
          .mon(_.storm.selector.time)
      }
  }

  def apply: Fu[List[StormPuzzle]] = current.get {}
}
