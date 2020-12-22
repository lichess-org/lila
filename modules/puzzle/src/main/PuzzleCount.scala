package lila.puzzle

import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.CacheApi

final private class PuzzleCountApi(
    colls: PuzzleColls,
    cacheApi: CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  def countsByTheme: Fu[Map[PuzzleTheme.Key, Int]] =
    byThemeCache get {}

  def byTheme(theme: PuzzleTheme.Key): Fu[Int] =
    countsByTheme dmap { _.getOrElse(theme, 0) }

  private val byThemeCache =
    cacheApi.unit[Map[PuzzleTheme.Key, Int]] {
      _.refreshAfterWrite(20 minutes)
        .buildAsyncFuture { _ =>
          import Puzzle.BSONFields._
          colls.puzzle {
            _.aggregateList(Int.MaxValue) { framework =>
              import framework._
              Project($doc(themes -> true)) -> List(
                Unwind(themes),
                GroupField(themes)("nb" -> SumAll)
              )
            }.map {
              _.flatMap { obj =>
                for {
                  key   <- obj string "_id"
                  count <- obj int "nb"
                } yield PuzzleTheme.Key(key) -> count
              }.toMap
            }.flatMap { themed =>
              colls.puzzle(_.countAll) map { all =>
                themed + (PuzzleTheme.mix.key -> all.toInt)
              }
            }
          }
        }
    }
}
