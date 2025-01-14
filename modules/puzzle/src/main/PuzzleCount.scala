package lila.puzzle

import lila.db.dsl.*
import lila.memo.CacheApi

final private class PuzzleCountApi(
    colls: PuzzleColls,
    cacheApi: CacheApi,
    openingApi: PuzzleOpeningApi
)(using Executor):

  def countsByTheme: Fu[Map[PuzzleTheme.Key, Int]] =
    byThemeCache.get {}

  def byTheme(theme: PuzzleTheme.Key): Fu[Int] =
    countsByTheme.dmap { _.getOrElse(theme, 0) }

  def byAngle(angle: PuzzleAngle): Fu[Int] = angle match
    case PuzzleAngle.Theme(theme)    => byTheme(theme)
    case PuzzleAngle.Opening(either) => openingApi.count(either)

  private val byThemeCache = cacheApi.unit[Map[PuzzleTheme.Key, Int]]:
    _.refreshAfterWrite(1.day).buildAsyncFuture: _ =>
      import Puzzle.BSONFields.*
      colls.puzzle:
        _.aggregateList(Int.MaxValue): framework =>
          import framework.*
          Project($doc(themes -> true)) -> List(
            Unwind(themes),
            GroupField(themes)("nb" -> SumAll)
          )
        .map: objs =>
          for
            obj   <- objs
            key   <- obj.string("_id")
            count <- obj.int("nb")
          yield PuzzleTheme.Key(key) -> count
        .flatMap: themed =>
          colls
            .puzzle(_.countAll)
            .map: all =>
              themed.toMap + (PuzzleTheme.mix.key -> all.toInt)
