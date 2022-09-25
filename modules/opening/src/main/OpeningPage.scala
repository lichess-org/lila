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
  val name = opening.fold(query.fen.value)(_.name.value)
  val key  = opening.fold(query.fen.value.replace(" ", "_"))(_.key.value)
}

case class ResultCounts(
    white: Int,
    draws: Int,
    black: Int
)

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
