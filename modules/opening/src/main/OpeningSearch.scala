package lila.opening

import chess.opening.{ FullOpening, FullOpeningDB }

import lila.common.base.StringUtils.levenshtein
import lila.memo.CacheApi
import lila.common.Heapsort

case class OpeningSearchResult(opening: FullOpening)

final class OpeningSearch(cacheApi: CacheApi, explorer: OpeningExplorer) {

  val max = 32

  def apply(q: String): Fu[List[OpeningSearchResult]] = fuccess {
    Heapsort
      .topN[(FullOpening, Int), Iterable[(FullOpening, Int)]](
        Opening.shortestLines.values map { op =>
          (op, levenshtein(q, op.name))
        },
        32,
        levenshteinOrdering
      )
      .view
      .map { case (op, _) => OpeningSearchResult(op) }
      .toList
  }

  private val levenshteinOrdering = Ordering.by[(FullOpening, Int), Int](-_._2)
}
