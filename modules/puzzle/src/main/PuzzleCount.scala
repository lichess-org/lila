package lila.puzzle

import lila.db.dsl.*
import lila.memo.CacheApi.buildAsyncTimeout

final private class PuzzleCountApi(
    colls: PuzzleColls,
    mongoCache: lila.memo.MongoCache.Api,
    openingApi: PuzzleOpeningApi
)(using Executor, Scheduler):

  private type ThemeCount = Map[PuzzleTheme.Key, Int]

  def countsByTheme: Fu[ThemeCount] =
    byThemeCache.get({})

  def byTheme(theme: PuzzleTheme.Key): Fu[Int] =
    countsByTheme.dmap { _.getOrElse(theme, 0) }

  def byAngle(angle: PuzzleAngle): Fu[Int] = angle match
    case PuzzleAngle.Theme(theme) => byTheme(theme)
    case PuzzleAngle.Opening(either) => openingApi.count(either)

  private val byThemeCache =
    given reactivemongo.api.bson.BSONHandler[ThemeCount] = typedMapHandler
    mongoCache.unit[ThemeCount]("puzzle:themeCount", 25.hours): loader =>
      _.refreshAfterWrite(1.hour).buildAsyncTimeout(30.seconds):
        loader: _ =>
          import Puzzle.BSONFields.*
          colls.puzzle:
            _.aggregateList(Int.MaxValue, _.sec): framework =>
              import framework.*
              Project($doc(themes -> true)) -> List(
                Unwind(themes),
                GroupField(themes)("nb" -> SumAll)
              )
            .map: objs =>
              for
                obj <- objs
                key <- obj.string("_id")
                count <- obj.int("nb")
              yield PuzzleTheme.Key(key) -> count
            .flatMap: themed =>
              colls
                .puzzle(_.countAll)
                .map: all =>
                  themed.toMap + (PuzzleTheme.mix.key -> all.toInt)
