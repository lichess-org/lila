package lila.study

import chess.format.FEN
import chess.variant.Variant
import chess.{ Pos, Color }
import org.joda.time.DateTime

case class Chapter(
    setup: Chapter.Setup,
    root: Node.Root,
    shapes: List[Chapter.Shape],
    ownerPath: Path,
    order: Int,
    createdAt: DateTime) {

  def updateRoot(f: Node.Root => Node.Root) = copy(root = f(root))
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

  def make(setup: Setup, root: Node.Root, order: Int) = Chapter(
    setup = setup,
    root = root,
    shapes = Nil,
    ownerPath = Path.root,
    order = order,
    createdAt = DateTime.now)
}
