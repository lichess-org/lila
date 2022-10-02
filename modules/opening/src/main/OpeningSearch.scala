package lila.opening

import chess.opening.{ FullOpening, FullOpeningDB }
import java.text.Normalizer

import lila.common.base.StringUtils.levenshtein
import lila.common.Heapsort.implicits._
import lila.memo.CacheApi

case class OpeningSearchResult(opening: FullOpening)

final class OpeningSearch(cacheApi: CacheApi, explorer: OpeningExplorer) {

  val max = 32

  def apply(q: String): Fu[List[OpeningSearchResult]] = fuccess {
    OpeningSearch(q, max).map(OpeningSearchResult)
  }
}

object OpeningSearch {

  private val openings: Vector[FullOpening] = Opening.shortestLines.values.toVector

  private type Token    = String
  private type Position = Int

  private[opening] object tokenize {
    private val nonLetterRegex = """[^a-zA-Z]+""".r
    private val exclude        = Set("opening", "variation")
    def apply(str: String): List[Token] = {
      str
        .take(200)
        .split(' ')
        .view
        .map(_.trim)
        .filter(_.nonEmpty)
        .take(6)
        .toList
        .map { token =>
          Normalizer
            .normalize(token, Normalizer.Form.NFD)
            .toLowerCase
            .replaceAllIn(nonLetterRegex, "")
        }
        .filterNot(exclude.contains)
    }
    def apply(opening: FullOpening): List[Token] =
      opening.key.toLowerCase.split('_').view.filterNot(exclude.contains).toList
  }

  private val index: Map[Token, Set[Position]] =
    openings.zipWithIndex.foldLeft(Map.empty[Token, Set[Position]]) { case (index, (opening, position)) =>
      tokenize(opening).foldLeft(index) { case (index, token) =>
        index.updatedWith(token) {
          case None            => Set(position).some
          case Some(positions) => (positions + position).some
        }
      }
    }

  private case class Match[R](result: R, exact: Boolean, freq: Int, nameSize: Int)

  private def matchOrdering[R] =
    Ordering.by[Match[R], (Boolean, Int, Int)] { case Match(_, exactMatch, freq, size) =>
      (exactMatch, freq, -size)
    }

  def apply(q: String, max: Int): List[FullOpening] = {
    val tokens            = tokenize(q)
    val positions         = tokens.flatMap(index.get)
    val merged            = positions.flatMap(_.toList)
    val positionsWithFreq = merged.groupBy(identity).view.mapValues(_.size).toList
    val matches: List[Match[FullOpening]] = positionsWithFreq.flatMap { case (position, freq) =>
      openings.lift(position).map(op => Match(op, true, freq, op.name.size))
    }
    val sorted = matches.topN(max)(matchOrdering)
    sorted.map(_.result)
  }
}
