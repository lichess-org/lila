package lila.puzzle

import scala.util.{ Try, Success, Failure }
import scalaz.Validation.FlatMap._

import chess.format.{ Forsyth, Uci }
import chess.Color
import org.joda.time.DateTime
import play.api.libs.json._

private case class Generated(
    category: String,
    last_pos: String,
    last_move: String,
    move_list: JsArray,
    game_id: String) {

  def isMate = category == "Mate"

  def colorFromFen = (Forsyth << last_pos).fold(Color.white)(!_.color)

  def toPuzzle: PuzzleId => Puzzle = Puzzle.make(
    gameId = game_id.some,
    history = List(last_move),
    fen = last_pos,
    color = colorFromFen,
    lines = Generated readLines move_list.as[List[String]])
}

private object Generated {

  def readLines(moves: List[String]): Lines = moves match {
    case Nil          => Nil
    case move :: Nil  => List(Win(move))
    case move :: more => List(Node(move, readLines(more)))
  }

  implicit val generatedJSONRead = Json.reads[Generated]
}
