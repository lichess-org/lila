package lila.opening

import scala.util.{ Try, Success, Failure }

import org.joda.time.DateTime
import play.api.libs.json._

private[opening] case class Generated(
    fen: String,
    moves: Map[String, Generated.Move]) {

  def toOpening: Try[Opening.ID => Opening] =
    (chess.format.Forsyth <<< fen) match {
      case None => Failure(new Exception(s"Can't parse fen $fen"))
      case Some(parsed) =>
        val color = parsed.situation.color
        moves.map {
          case (first, move) => for {
            pgn <- Generated.toPgn(parsed.situation, first :: move.line.split(' ').toList)
            cp <- parseIntOption(move.cp) match {
              case None     => Failure(new Exception(s"Invalid cp ${move.cp}"))
              case Some(cp) => Success(cp)
            }
          } yield Move(first = first, cp = cp, line = pgn)
        }.foldLeft(Try(List[Move]())) {
          case (Success(acc), Success(l)) => Success(l :: acc)
          case (err: Failure[_], _)       => err
          case (_, Failure(err))          => Failure(err)
        }.map { realMoves =>
          Opening.make(
            fen = fen,
            color = color,
            moves = realMoves)
        }
    }
}

private[opening] object Generated {

  case class Move(cp: String, line: String)

  implicit val generatedMoveJSONRead = Json.reads[Move]
  implicit val generatedJSONRead = Json.reads[Generated]

  import chess.format.UciMove

  private[opening] def toPgn(
    situation: chess.Situation,
    uciMoves: List[String]): Try[List[String]] = {
    val game = chess.Game(
      board = situation.board,
      player = situation.color)
    (uciMoves.foldLeft(Try(game)) {
      case (game, moveStr) => game flatMap { g =>
        (UciMove(moveStr) toValid s"Invalid UCI move $moveStr" flatMap {
          case UciMove(orig, dest, prom) => g(orig, dest, prom) map (_._1)
        }).fold(errs => Failure(new Exception(errs.shows)), Success.apply)
      }
    }) map (_.pgnMoves)
  }
}
