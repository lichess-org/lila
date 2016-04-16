package lila.study

import chess.format.FEN
import chess.{Pos,Color}
import chess.variant.Variant
import org.joda.time.DateTime

case class Chapter(
    setup: Chapter.Setup,
    root: Node.Root,
    shapes: List[Chapter.Shape],
    ownerPath: Path,
    createdAt: DateTime) {
}

object Chapter {

  case class Setup(
    gameId: Option[String],
    variant: Variant,
    orientation: Color,
    initialFen: FEN)

  type ID = String
  type Brush = String

  sealed trait Shape
  object Shape {
    case class Circle(brush: Brush, pos: Pos) extends Shape
    case class Arrow(brush: Brush, orig: Pos, dest: Pos) extends Shape
  }

  val idSize = 4

  def makeId = scala.util.Random.alphanumeric take idSize mkString

  def make(setup: Setup, root: Node.Root) = Chapter(
    setup = setup,
    root = root,
    shapes = Nil,
    ownerPath = Path.root,
    createdAt = DateTime.now)
}
