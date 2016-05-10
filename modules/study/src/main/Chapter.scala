package lila.study

import chess.Color
import chess.variant.Variant
import org.joda.time.DateTime

import chess.opening.{ FullOpening, FullOpeningDB }
import lila.socket.tree.Node.{ Shape, Comment }

case class Chapter(
    _id: Chapter.ID,
    studyId: Study.ID,
    name: String,
    setup: Chapter.Setup,
    root: Node.Root,
    order: Int,
    createdAt: DateTime) extends Chapter.Like {

  def updateRoot(f: Node.Root => Option[Node.Root]) =
    f(root) map { newRoot =>
      copy(root = newRoot)
    }

  def addNode(node: Node, path: Path): Option[Chapter] =
    updateRoot { root =>
      root.withChildren(_.addNodeAt(node, path))
    }

  def setShapes(shapes: List[Shape], path: Path): Option[Chapter] =
    updateRoot(_.setShapesAt(shapes, path))

  def setComment(comment: Comment, path: Path): Option[Chapter] =
    updateRoot(_.setCommentAt(comment, path))

  def opening: Option[FullOpening] =
    if (!Variant.openingSensibleVariants(setup.variant)) none
    else FullOpeningDB searchInFens root.mainLine.map(_.fen)
}

object Chapter {

  type ID = String

  sealed trait Like {
    val _id: Chapter.ID
    val name: String
    val setup: Chapter.Setup
    def id = _id
  }

  case class Setup(
    gameId: Option[String],
    variant: Variant,
    orientation: Color)

  case class Metadata(
    _id: Chapter.ID,
    name: String,
    setup: Chapter.Setup) extends Like

  def toName(str: String) = str.trim take 80

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
