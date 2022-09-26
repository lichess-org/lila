package lila.opening

import chess.format.{ FEN, Forsyth, Uci }
import chess.opening.FullOpeningDB
import chess.Speed

import lila.common.LilaOpening

case class OpeningPage(
    query: OpeningQuery,
    opening: Option[LilaOpening],
    explored: OpeningExplored
) {
  val name = opening.fold(query.fen.value)(_.ref.name)
  val key  = opening.fold(query.fen.value.replace(" ", "_"))(_.key.value)
}

case class ResultCounts(
    white: Int,
    draws: Int,
    black: Int
) {
  lazy val sum: Int = black + draws + white

  def whitePercent              = percentOf(white)
  def blackPercent              = percentOf(black)
  def drawPercent               = percentOf(draws)
  private def percentOf(v: Int) = (v.toDouble * 100d / sum.toDouble).toFloat
}

case class OpeningNext(move: OpeningExplorer.Move, fen: FEN)

case class OpeningExplored(result: ResultCounts, next: List[OpeningNext])

object OpeningPage {
  def apply(query: OpeningQuery, exp: OpeningExplorer.Position): OpeningPage = OpeningPage(
    query = query,
    opening = FullOpeningDB.findByFen(query.fen).flatMap(LilaOpening.apply),
    OpeningExplored(
      result = ResultCounts(exp.white, exp.draws, exp.black),
      next = exp.moves.flatMap { m =>
        for {
          uci  <- Uci.Move(m.uci)
          move <- query.position.move(uci).toOption
        } yield OpeningNext(m, Forsyth >> move.situationAfter)
      }
    )
  )
}
