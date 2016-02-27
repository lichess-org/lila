package lila.study

import chess.format.Uci
import chess.Pos
import org.joda.time.DateTime

case class Chapter(
    gameId: Option[String],
    steps: List[Chapter.Step],
    shapes: List[Chapter.Shape],
    ownerPath: Path,
    createdAt: DateTime) {

}

object Chapter {

  type ID = String
  type Brush = String

  sealed trait Shape
  object Shape {
    case class Circle(brush: Brush, pos: Pos) extends Shape
    case class Arrow(brush: Brush, orig: Pos, dest: Pos) extends Shape
  }

  case class Step(
      ply: Int,
      move: Option[Step.Move],
      fen: String,
      check: Boolean,
      variations: List[List[Step]]) {
  }

  object Step {
    case class Move(uci: Uci, san: String)
  }

  val idSize = 4

  def makeId = scala.util.Random.alphanumeric take idSize mkString

  def make(gameId: Option[String]) = Chapter(
    gameId = gameId,
    steps = Nil,
    shapes = Nil,
    ownerPath = Path.init,
    createdAt = DateTime.now)
}
