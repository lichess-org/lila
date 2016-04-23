package lila.study

import chess.Color
import chess.variant.Variant
import org.joda.time.DateTime

case class Chapter(
    _id: Chapter.ID,
    studyId: Study.ID,
    name: String,
    setup: Chapter.Setup,
    root: Node.Root,
    order: Int,
    createdAt: DateTime) {

  def id = _id

  def updateRoot(f: Node.Root => Option[Node.Root]) =
    f(root) map { newRoot =>
      copy(root = newRoot)
    }

  def addNode(path: Path, node: Node): Option[Chapter] =
    updateRoot { root =>
      root.withChildren(_.addNodeAt(node, path))
    }
}

object Chapter {

  type ID = String

  case class Setup(
    gameId: Option[String],
    variant: Variant,
    orientation: Color)

  val idSize = 8

  def makeId = scala.util.Random.alphanumeric take idSize mkString

  def make(studyId: Study.ID, name: String, setup: Setup, root: Node.Root, order: Int) = Chapter(
    _id = scala.util.Random.alphanumeric take idSize mkString,
    studyId = studyId,
    name = name,
    setup = setup,
    root = root,
    order = order,
    createdAt = DateTime.now)
}
