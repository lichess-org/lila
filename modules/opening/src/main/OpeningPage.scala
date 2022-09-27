package lila.opening

import chess.format.{ FEN, Forsyth, Uci }
import chess.opening.FullOpeningDB
import chess.Speed

import lila.common.SimpleOpening
import chess.format.pgn.San

case class OpeningPage(
    query: OpeningQuery,
    explored: OpeningExplored
) {
  def opening = query.opening
  def name    = query.name
}

case class ResultCounts(
    white: Long,
    draws: Long,
    black: Long
) {
  lazy val sum: Long = white + draws + black

  def whitePercent                       = percentOf(white)
  def drawsPercent                       = percentOf(draws)
  def blackPercent                       = percentOf(black)
  private def percentOf(v: Long): Double = (v * 100d / sum)
}

case class OpeningNext(
    san: String,
    uci: Uci.Move,
    fen: FEN,
    query: OpeningQuery,
    result: ResultCounts,
    percent: Double,
    opening: Option[SimpleOpening]
) {
  val key = opening.fold(fen.value.replace(" ", "_"))(_.key.value)
}

case class OpeningExplored(result: ResultCounts, next: List[OpeningNext])

object OpeningPage {
  def apply(query: OpeningQuery, exp: OpeningExplorer.Position): OpeningPage = OpeningPage(
    query = query,
    OpeningExplored(
      result = ResultCounts(exp.white, exp.draws, exp.black),
      next = exp.moves
        .flatMap { m =>
          for {
            uci  <- Uci.Move(m.uci)
            move <- query.position.move(uci).toOption
            result = ResultCounts(m.white, m.draws, m.black)
            fen    = Forsyth >> move.situationAfter
          } yield OpeningNext(
            m.san,
            uci,
            fen,
            query.copy(
              pgn = query.pgn :+ chess.format.pgn.Dumper(query.position, move, move.situationAfter),
              position = move.situationAfter
            ),
            result,
            (result.sum * 100d / exp.movesSum),
            FullOpeningDB.findByFen(fen).flatMap(SimpleOpening.apply)
          )
        }
        .sortBy(-_.result.sum)
    )
  )
}
