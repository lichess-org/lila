package lila.puzzle

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.CacheApi

private object PuzzlePath {
  object tier {
    val top = "top"
    val all = "all"
  }
}

final private class PuzzlePathApi(
    colls: PuzzleColls,
    cacheApi: CacheApi
)(implicit ec: ExecutionContext) {

  def countPuzzlesByTheme(theme: PuzzleTheme.Key): Fu[Int] =
    countByThemeCache get {} dmap { _.getOrElse(theme, 0) }

  private val countByThemeCache =
    cacheApi.unit[Map[PuzzleTheme.Key, Int]] {
      _.refreshAfterWrite(10 minutes)
        .buildAsyncFuture { _ =>
          colls.path {
            _.aggregateList(Int.MaxValue) { framework =>
              import framework._
              Match($doc("tier" -> "all")) -> List(
                GroupField("tag")(
                  "count" -> SumField("length")
                )
              )
            }.map {
              _.flatMap { obj =>
                for {
                  key   <- obj string "_id"
                  count <- obj int "count"
                } yield PuzzleTheme.Key(key) -> count
              }.toMap
            }
          }
        }
    }
}
