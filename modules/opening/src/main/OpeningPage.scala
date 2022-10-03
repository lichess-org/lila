package lila.opening

import chess.format.pgn.Pgn
import chess.format.pgn.San
import chess.format.{ FEN, Forsyth, Uci }
import chess.opening.FullOpening
import chess.opening.FullOpeningDB
import chess.Speed

import lila.game.Game

case class OpeningPage(
    query: OpeningQuery,
    explored: OpeningExplored
) {
  def opening = query.opening
  def name    = query.name

  def nameParts = query.openingAndExtraMoves match {
    case (op, moves) => (op ?? NamePart.from) ::: NamePart.from(moves)
  }
}

case class NamePart(name: String, path: Option[String])

case object NamePart {
  def from(op: FullOpening): List[NamePart] = {
    val sections = Opening.sectionsOf(op.name)
    sections.toList.zipWithIndex map { case (name, i) =>
      NamePart(
        name,
        Opening.shortestLines.get(FullOpening.nameToKey(sections.take(i + 1).mkString("_"))).map(_.key)
      )
    }
  }
  def from(moves: List[String]) = moves.map { m => NamePart(m, none) }
}

case class ResultCounts(
    white: Int,
    draws: Int,
    black: Int
) {
  lazy val sum: Int = white + draws + black

  def whitePercent                     = percentOf(white)
  def drawsPercent                     = percentOf(draws)
  def blackPercent                     = percentOf(black)
  private def percentOf(v: Int): Float = (v.toFloat * 100 / sum)
}

case class OpeningNext(
    san: String,
    uci: Uci.Move,
    fen: FEN,
    query: OpeningQuery,
    result: ResultCounts,
    percent: Double,
    opening: Option[FullOpening],
    shortName: Option[String]
) {
  val key = opening.fold(fen.value.replace(" ", "_"))(_.key)
}

case class GameWithPgn(game: Game, pgn: Pgn)

case class OpeningExplored(
    result: ResultCounts,
    games: List[GameWithPgn],
    next: List[OpeningNext],
    history: PopularityHistoryPercent
)

object OpeningPage {
  def apply(
      query: OpeningQuery,
      exp: OpeningExplorer.Position,
      games: List[GameWithPgn],
      history: PopularityHistoryPercent
  ): OpeningPage =
    OpeningPage(
      query = query,
      OpeningExplored(
        result = ResultCounts(exp.white, exp.draws, exp.black),
        games = games,
        next = exp.moves
          .flatMap { m =>
            for {
              uci  <- Uci.Move(m.uci)
              move <- query.position.move(uci).toOption
              result  = ResultCounts(m.white, m.draws, m.black)
              fen     = Forsyth >> move.situationAfter
              opening = FullOpeningDB findByFen fen
            } yield OpeningNext(
              m.san,
              uci,
              fen,
              query.copy(replay = query.replay addMove Left(move)),
              result,
              (result.sum * 100d / exp.movesSum),
              opening,
              shortName = Opening.variationName(query.opening, opening)
            )
          }
          .sortBy(-_.result.sum),
        history = history
      )
    )
}
