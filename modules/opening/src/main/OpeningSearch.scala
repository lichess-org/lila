package lila.opening

import chess.format.pgn.PgnMovesStr
import chess.opening.{ Opening, OpeningDb }
import scalalib.HeapSort.topN

import java.text.Normalizer

import lila.common.Chronometer
import lila.memo.CacheApi

case class OpeningSearchResult(opening: Opening):
  def pgn   = OpeningSearch.removePgnMoveNumbers(opening.pgn)
  def query = OpeningQuery.Query(opening.key.value, pgn.some)

final class OpeningSearch(using Executor):

  val max = 32

  private val cache = CacheApi.scaffeine
    .maximumSize(1024)
    .build[String, List[OpeningSearchResult]](doSearch)

  def apply(q: String): List[OpeningSearchResult] = cache.get(q)

  def doSearch(q: String): List[OpeningSearchResult] =
    OpeningSearch(q, max).map(OpeningSearchResult.apply)

// linear performance but it's fine for 3,067 unique openings
private object OpeningSearch:

  object removePgnMoveNumbers:
    private val numbersRegex    = """\d{1,2}\.{1,3}\s?""".r
    def apply(pgn: PgnMovesStr) = pgn.map(_.replaceAllIn(numbersRegex, "").trim)

  private val openings: Vector[Opening] = OpeningDb.shortestLines.values.toVector

  private type Token = String
  private type Score = Int

  private[opening] object tokenize:
    private val nonLetterRegex = """[^a-zA-Z0-9]+""".r
    private val exclude        = Set("opening", "variation")
    private val replace        = Map("defence" -> "defense")
    def apply(str: String): Set[Token] =
      str
        .take(200)
        .replace("_", " ")
        .replace("-", " ")
        .split(' ')
        .view
        .map(_.trim)
        .filter(_.nonEmpty)
        .take(6)
        .map: token =>
          Normalizer
            .normalize(token, Normalizer.Form.NFD)
            .toLowerCase
            .replaceAllIn(nonLetterRegex, "")
        .map(t => replace.getOrElse(t, t))
        .toSet
        .diff(exclude)
    def apply(opening: Opening): Set[Token] =
      opening.key.value.toLowerCase.replace("-", "_").split('_').view.filterNot(exclude.contains).toSet +
        opening.eco.value.toLowerCase ++
        opening.pgn.value.split(' ').take(6).toSet

  private case class Query(raw: String, numberedPgn: String, tokens: Set[Token])
  private def makeQuery(userInput: String) =
    val clean = userInput.trim.toLowerCase
    val numberedPgn = // try to produce numbered PGN "1. e4 e5 2. f4" from a query like "e4 e5 f4"
      clean
        .split(' ')
        .toList
        .map(_.trim)
        .filter(_.nonEmpty)
        .grouped(2)
        .toList
        .mapWithIndex { (moves, index) =>
          s"${index + 1}. ${moves.mkString(" ")}"
        }
        .mkString(" ")
    Query(clean, numberedPgn, tokenize(clean))
  private case class Entry(opening: Opening, tokens: Set[Token])
  private case class Match(opening: Opening, score: Score)

  private val index: List[Entry] =
    openings.view.map { op =>
      Entry(op, tokenize(op))
    }.toList

  private def scoreOf(query: Query, entry: Entry): Option[Score] = {
    def exactMatch(token: Token) =
      entry.tokens(token) ||
        entry.tokens(s"${token}s") // King's and Queen's can be matched by king and queen
    if entry.opening.pgn.value.startsWith(query.raw) ||
      entry.opening.pgn.value.startsWith(query.numberedPgn) ||
      entry.opening.uci.value.startsWith(query.raw)
    then (query.raw.size * 1000 - entry.opening.nbMoves)
    else
      query.tokens
        .foldLeft((query.tokens, 0)) { case ((remaining, score), token) =>
          if exactMatch(token) then (remaining - token, score + token.size * 100)
          else (remaining, score)
        } match
        case (remaining, score) =>
          score + remaining.map { t =>
            entry.tokens.map { e =>
              if e.startsWith(t) then t.size * 50
              else if e.contains(t) then t.size * 20
              else 0
            }.sum
          }.sum
  }.some.filter(_ > 0).map(_ - entry.opening.key.value.size)

  private given Ordering[Match] = Ordering.by { case Match(_, score) => score }

  def apply(str: String, max: Int): List[Opening] = Chronometer.syncMon(_.opening.searchTime):
    val query = makeQuery(str)
    index
      .flatMap: entry =>
        scoreOf(query, entry).map:
          Match(entry.opening, _)
      .topN(max)
      .map(_.opening)
