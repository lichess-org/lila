package lila.opening

import chess.format.{ FEN, Forsyth, Uci }
import chess.opening.FullOpeningDB
import chess.Speed

import lila.common.LilaOpening
import chess.format.pgn.San

case class OpeningPage(
    query: OpeningQuery,
    opening: Option[LilaOpening],
    explored: OpeningExplored
) {
  val name = opening.fold(query.fen.value)(_.ref.name)
  val key  = opening.fold(query.fen.value.replace(" ", "_"))(_.key.value)
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
    result: ResultCounts,
    percent: Double,
    opening: Option[LilaOpening]
) {
  val key = opening.fold(fen.value.replace(" ", "_"))(_.key.value)
}

case class OpeningExplored(result: ResultCounts, next: List[OpeningNext])

object OpeningPage {
  def apply(query: OpeningQuery, exp: OpeningExplorer.Position): OpeningPage = OpeningPage(
    query = query,
    opening = FullOpeningDB.findByFen(query.fen).flatMap(LilaOpening.apply),
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
            result,
            (result.sum * 100d / exp.movesSum),
            FullOpeningDB.findByFen(fen).flatMap(LilaOpening.apply)
          )
        }
        .sortBy(-_.result.sum)
    )
  )
}
