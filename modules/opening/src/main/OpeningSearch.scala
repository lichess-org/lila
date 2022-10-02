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
    OpeningSearch(q).map(OpeningSearchResult)
  }
}

object OpeningSearch {

  private val openings: Vector[FullOpening] = Opening.shortestLines.values.toVector

  private type Token    = String
  private type Position = Int
  private type Freq     = Int
  private type NameSize = Int

  private val tokenR              = """[^\w-]""".r
  private val tokenMultiDashRegex = """-{2,}""".r
  private val excludeTokens       = Set("opening", "variation")

  private def tokenize(str: String): List[Token] = {
    val singleDashes = tokenMultiDashRegex.replaceAllIn(str.trim.replace(' ', '-'), "-")
    val normalized   = Normalizer.normalize(singleDashes, Normalizer.Form.NFD)
    tokenR
      .replaceAllIn(normalized, "")
      .toLowerCase
      .split('-')
      .view
      .filterNot(excludeTokens.contains)
      .toList
  }
  private def tokenize(opening: FullOpening): List[Token] =
    opening.key.toLowerCase.split('_').view.filterNot(excludeTokens.contains).toList

  private val index: Map[Token, Set[Position]] =
    openings.zipWithIndex.foldLeft(Map.empty[Token, Set[Position]]) { case (index, (opening, position)) =>
      tokenize(opening).foldLeft(index) { case (index, token) =>
        index.updatedWith(token) {
          case None            => Set(position).some
          case Some(positions) => (positions + position).some
        }
      }
    }
  private val searchOrdering = Ordering.by[(FullOpening, Freq, NameSize), (Freq, NameSize)] {
    case (_, freq, size) => (freq, -size)
  }

  def apply(q: String): List[FullOpening] = {
    val tokens                         = tokenize(q)
    val positions: List[Set[Position]] = tokens.flatMap(index.get)
    val merged                         = positions.flatMap(_.toList)
    val positionsWithFreq              = merged.groupBy(identity).view.mapValues(_.size).toList
    val openingsWithFreqAndLen: List[(FullOpening, Freq, NameSize)] = positionsWithFreq.flatMap {
      case (position, freq) => openings.lift(position).map(op => (op, freq, op.name.size))
    }
    val sorted = openingsWithFreqAndLen.topN(10)(searchOrdering)
    sorted.map(_._1)
  }
}
