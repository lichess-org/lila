package lila.study

import chess.variant.Variant
import chess.Color
import org.joda.time.DateTime

case class Chapter(
    setup: Chapter.Setup,
    root: Node.Root,
    order: Int,
    createdAt: DateTime) {

  def updateRoot(f: Node.Root => Node.Root) = copy(root = f(root))
}

object Chapter {

  type ID = String

  case class Setup(
    gameId: Option[String],
    variant: Variant,
    orientation: Color)

  val idSize = 4

  def makeId = scala.util.Random.alphanumeric take idSize mkString

  def make(setup: Setup, root: Node.Root, order: Int) = Chapter(
    setup = setup,
    root = root,
    order = order,
    createdAt = DateTime.now)
}
