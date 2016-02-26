package lila.study

import chess.Pos
import chess.format.Uci
import org.joda.time.DateTime

import lila.user.User

case class Study(
    _id: Study.ID,
    owner: User.ID,
    gameId: Option[String],
    steps: List[Study.Step],
    shapes: List[Study.Shape],
    createdAt: DateTime) {

  import Study._

  def id = _id
}

object Study {

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

  def make(
    id: ID,
    owner: User.ID,
    gameId: Option[String]): Study = Study(
    _id = id,
    owner = owner,
    gameId = gameId,
    steps = Nil,
    shapes = Nil,
    createdAt = DateTime.now)
}
