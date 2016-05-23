package lila.study

import chess.format.pgn.{ Glyph, Glyphs }
import chess.format.{ Uci, UciCharPair, Forsyth, FEN }
import chess.opening.FullOpening
import chess.variant.Crazyhouse

import lila.socket.tree.Node.{ Shape, Shapes, Comment, Comments }
import lila.user.User

sealed trait RootOrNode {
  val ply: Int
  val fen: FEN
  val check: Boolean
  val shapes: Shapes
  val crazyData: Option[Crazyhouse.Data]
  val children: Node.Children
  val comments: Comments
  val glyphs: Glyphs
  def fullMoveNumber = 1 + ply / 2
}

case class Node(
    id: UciCharPair,
    ply: Int,
    move: Uci.WithSan,
    fen: FEN,
    check: Boolean,
    shapes: Shapes = Shapes(Nil),
    comments: Comments = Comments(Nil),
    glyphs: Glyphs = Glyphs.empty,
    crazyData: Option[Crazyhouse.Data],
    children: Node.Children) extends RootOrNode {

  import Node.Children

  def withChildren(f: Children => Option[Children]) =
    f(children) map { newChildren =>
      copy(children = newChildren)
    }

  def setComment(comment: Comment) = copy(comments = comments set comment)
  def deleteComment(commentId: Comment.Id) = copy(comments = comments delete commentId)

  def toggleGlyph(glyph: Glyph) = copy(glyphs = glyphs toggle glyph)

  def mainline: List[Node] = this :: children.first.??(_.mainline)
}

object Node {

  val MAX_PLIES = 400

  case class Children(nodes: Vector[Node]) {

    def first = nodes.headOption
    def variations = nodes drop 1

    def nodeAt(path: Path): Option[Node] = path.split match {
      case None                               => none
      case Some((head, tail)) if tail.isEmpty => get(head)
      case Some((head, tail))                 => get(head) flatMap (_.children nodeAt tail)
    }

    def addNodeAt(node: Node, path: Path): Option[Children] = path.split match {
      case None if has(node.id) => this.some
      case None                 => copy(nodes = nodes :+ node).some
      case Some((head, tail))   => updateChildren(head, _.addNodeAt(node, tail))
    }

    def deleteNodeAt(path: Path): Option[Children] = path.split match {
      case None                                 => none
      case Some((head, Path(Nil))) if has(head) => copy(nodes = nodes.filterNot(_.id == head)).some
      case Some((_, Path(Nil)))                 => none
      case Some((head, tail))                   => updateChildren(head, _.deleteNodeAt(tail))
    }

    def promoteNodeAt(path: Path): Option[Children] = path.split match {
      case None => this.some
      case Some((head, tail)) =>
        get(head).flatMap { node =>
          node.withChildren(_.promoteNodeAt(tail)).map { promoted =>
            copy(nodes = promoted +: nodes.filterNot(node ==))
          }
        }
    }

    def setShapesAt(shapes: Shapes, path: Path): Option[Children] = path.split match {
      case None                    => none
      case Some((head, Path(Nil))) => updateWith(head, _.copy(shapes = shapes).some)
      case Some((head, tail))      => updateChildren(head, _.setShapesAt(shapes, tail))
    }

    def setCommentAt(comment: Comment, path: Path): Option[Children] = path.split match {
      case None                    => none
      case Some((head, Path(Nil))) => updateWith(head, _.setComment(comment).some)
      case Some((head, tail))      => updateChildren(head, _.setCommentAt(comment, tail))
    }
    def deleteCommentAt(commentId: Comment.Id, path: Path): Option[Children] = path.split match {
      case None                    => none
      case Some((head, Path(Nil))) => updateWith(head, _.deleteComment(commentId).some)
      case Some((head, tail))      => updateChildren(head, _.deleteCommentAt(commentId, tail))
    }

    def toggleGlyphAt(glyph: Glyph, path: Path): Option[Children] = path.split match {
      case None                    => none
      case Some((head, Path(Nil))) => updateWith(head, _.toggleGlyph(glyph).some)
      case Some((head, tail))      => updateChildren(head, _.toggleGlyphAt(glyph, tail))
    }

    def get(id: UciCharPair): Option[Node] = nodes.find(_.id == id)

    def has(id: UciCharPair): Boolean = nodes.exists(_.id == id)

    def updateWith(id: UciCharPair, op: Node => Option[Node]): Option[Children] =
      get(id) flatMap op map update

    def updateChildren(id: UciCharPair, f: Children => Option[Children]): Option[Children] =
      updateWith(id, _ withChildren f)

    def update(child: Node): Children = copy(
      nodes = nodes.map {
        case n if child.id == n.id => child
        case n                     => n
      })
  }
  val emptyChildren = Children(Vector.empty)

  case class Root(
      ply: Int,
      fen: FEN,
      check: Boolean,
      shapes: Shapes = Shapes(Nil),
      comments: Comments = Comments(Nil),
      glyphs: Glyphs = Glyphs.empty,
      crazyData: Option[Crazyhouse.Data],
      children: Children) extends RootOrNode {

    def withChildren(f: Children => Option[Children]) =
      f(children) map { newChildren =>
        copy(children = newChildren)
      }

    def nodeAt(path: Path): Option[RootOrNode] =
      if (path.isEmpty) this.some else children nodeAt path

    def pathExists(path: Path): Boolean = nodeAt(path).isDefined

    def setShapesAt(shapes: Shapes, path: Path): Option[Root] =
      if (path.isEmpty) copy(shapes = shapes).some
      else withChildren(_.setShapesAt(shapes, path))

    def setCommentAt(comment: Comment, path: Path): Option[Root] =
      if (path.isEmpty) copy(comments = comments set comment).some
      else withChildren(_.setCommentAt(comment, path))
    def deleteCommentAt(commentId: Comment.Id, path: Path): Option[Root] =
      if (path.isEmpty) copy(comments = comments delete commentId).some
      else withChildren(_.deleteCommentAt(commentId, path))

    def toggleGlyphAt(glyph: Glyph, path: Path): Option[Root] =
      if (path.isEmpty) copy(glyphs = glyphs toggle glyph).some
      else withChildren(_.toggleGlyphAt(glyph, path))

    lazy val mainline: List[Node] = children.first.??(_.mainline)

    def lastMainlinePly = Chapter.Ply(mainline.lastOption.??(_.ply))

    def lastMainlinePlyOf(path: Path) = Chapter.Ply {
      mainline.zip(path.ids).takeWhile {
        case (node, id) => node.id == id
      }.lastOption.?? {
        case (node, _) => node.ply
      }
    }
  }

  object Root {

    def default(variant: chess.variant.Variant) = Root(
      ply = 0,
      fen = FEN(variant.initialFen),
      check = false,
      crazyData = variant.crazyhouse option Crazyhouse.Data.init,
      children = emptyChildren)

    def fromRoot(b: lila.socket.tree.Root): Root = Root(
      ply = b.ply,
      fen = FEN(b.fen),
      check = b.check,
      crazyData = b.crazyData,
      children = Children(b.children.toVector map fromBranch))
  }

  def fromBranch(b: lila.socket.tree.Branch): Node = Node(
    id = b.id,
    ply = b.ply,
    move = b.move,
    fen = FEN(b.fen),
    check = b.check,
    crazyData = b.crazyData,
    children = Children(b.children.toVector map fromBranch))
}
