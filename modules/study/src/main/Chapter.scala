package lila.study

import chess.Color
import chess.format.FEN
import chess.format.pgn.{ Glyph, Tag }
import chess.variant.Variant
import org.joda.time.DateTime

import chess.opening.{ FullOpening, FullOpeningDB }
import lila.socket.tree.Node.{ Shape, Comment }
import lila.user.User

case class Chapter(
    _id: Chapter.ID,
    studyId: Study.ID,
    name: String,
    setup: Chapter.Setup,
    root: Node.Root,
    order: Int,
    ownerId: User.ID,
    conceal: Option[Chapter.Ply] = None,
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

  def deleteComment(commentId: Comment.Id, path: Path): Option[Chapter] =
    updateRoot(_.deleteCommentAt(commentId, path))

  def toggleGlyph(glyph: Glyph, path: Path): Option[Chapter] =
    updateRoot(_.toggleGlyphAt(glyph, path))

  def opening: Option[FullOpening] =
    if (!Variant.openingSensibleVariants(setup.variant)) none
    else FullOpeningDB searchInFens root.mainline.map(_.fen)

  def isEmptyInitial = order == 1 && root.children.nodes.isEmpty
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
    orientation: Color,
    fromPgn: Option[FromPgn] = None,
    fromFen: Option[FEN] = None)

  case class FromPgn(tags: List[Tag])

  case class Metadata(
    _id: Chapter.ID,
    name: String,
    setup: Chapter.Setup,
    conceal: Option[Ply]) extends Like

  case class Ply(value: Int) extends AnyVal with Ordered[Ply] {
    def compare(that: Ply) = value - that.value
  }

  def toName(str: String) = str.trim take 80

  val idSize = 8

  def makeId = scala.util.Random.alphanumeric take idSize mkString

  def make(studyId: Study.ID, name: String, setup: Setup, root: Node.Root, order: Int, ownerId: User.ID, conceal: Option[Ply]) = Chapter(
    _id = scala.util.Random.alphanumeric take idSize mkString,
    studyId = studyId,
    name = toName(name),
    setup = setup,
    root = root,
    order = order,
    ownerId = ownerId,
    conceal = conceal,
    createdAt = DateTime.now)
}
