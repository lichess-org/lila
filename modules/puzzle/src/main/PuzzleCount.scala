package lila.puzzle

import reactivemongo.api._
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

  def countsByOpening: Fu[List[(PuzzleOpening, Int)]] =
    byOpeningCache get {}

  def byOpening(opening: PuzzleOpening.Key): Fu[Int] =
    countsByOpening dmap { ~_.find(_._1.key == opening).map(_._2) }

  def byAngle(angle: PuzzleAngle): Fu[Int] = angle match {
    case PuzzleAngle.Theme(theme)     => byTheme(theme)
    case PuzzleAngle.Opening(opening) => byOpening(opening)
  }

  private val byThemeCache =
    cacheApi.unit[Map[PuzzleTheme.Key, Int]] {
      _.refreshAfterWrite(1 day)
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

  private val byOpeningCache =
    cacheApi.unit[List[(PuzzleOpening, Int)]] {
      _.refreshAfterWrite(1 day)
        .buildAsyncFuture { _ =>
          import Puzzle.BSONFields._
          colls.puzzle {
            _.aggregateList(64) { framework =>
              import framework._
              PipelineOperator($doc("$sortByCount" -> s"$$$opening")) -> Nil
            }.map {
              _.flatMap { obj =>
                for {
                  key   <- obj string "_id" map PuzzleOpening.Key
                  count <- obj int "count"
                  name  <- PuzzleOpening.openings get key
                } yield PuzzleOpening(key, name) -> count
              }
            }
          }
        }
    }
}
