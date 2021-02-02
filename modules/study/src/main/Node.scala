package lila.study

import chess.format.pgn.{ Glyph, Glyphs }
import chess.format.{ FEN, Uci, UciCharPair }
import chess.variant.Crazyhouse

import chess.Centis
import lila.tree.Eval.Score
import lila.tree.Node.{ Comment, Comments, Gamebook, Shapes }

sealed trait RootOrNode {
  val ply: Int
  val fen: FEN
  val check: Boolean
  val shapes: Shapes
  val clock: Option[Centis]
  val crazyData: Option[Crazyhouse.Data]
  val children: Node.Children
  val comments: Comments
  val gamebook: Option[Gamebook]
  val glyphs: Glyphs
  val score: Option[Score]
  def addChild(node: Node): RootOrNode
  def fullMoveNumber = 1 + ply / 2
  def mainline: Vector[Node]
  def color = chess.Color.fromPly(ply)
  def moveOption: Option[Uci.WithSan]
}

case class Node(
    id: UciCharPair,
    ply: Int,
    move: Uci.WithSan,
    fen: FEN,
    check: Boolean,
    shapes: Shapes = Shapes(Nil),
    comments: Comments = Comments(Nil),
    gamebook: Option[Gamebook] = None,
    glyphs: Glyphs = Glyphs.empty,
    score: Option[Score] = None,
    clock: Option[Centis],
    crazyData: Option[Crazyhouse.Data],
    children: Node.Children,
    forceVariation: Boolean
) extends RootOrNode {

  import Node.Children

  def withChildren(f: Children => Option[Children]) =
    f(children) map { newChildren =>
      copy(children = newChildren)
    }

  def withoutChildren = copy(children = Node.emptyChildren)

  def addChild(child: Node) = copy(children = children addNode child)

  def withClock(centis: Option[Centis])  = copy(clock = centis)
  def withForceVariation(force: Boolean) = copy(forceVariation = force)

  def isCommented = comments.value.nonEmpty

  def setComment(comment: Comment)         = copy(comments = comments set comment)
  def deleteComment(commentId: Comment.Id) = copy(comments = comments delete commentId)
  def deleteComments                       = copy(comments = Comments.empty)

  def setGamebook(gamebook: Gamebook) = copy(gamebook = gamebook.some)

  def setShapes(s: Shapes) = copy(shapes = s)

  def toggleGlyph(glyph: Glyph) = copy(glyphs = glyphs toggle glyph)

  def mainline: Vector[Node] = this +: children.first.??(_.mainline)

  def updateMainlineLast(f: Node => Node): Node =
    children.first.fold(f(this)) { main =>
      copy(children = children.update(main updateMainlineLast f))
    }

  def clearAnnotations =
    copy(
      comments = Comments(Nil),
      shapes = Shapes(Nil),
      glyphs = Glyphs.empty,
      score = none
    )

  def merge(n: Node): Node =
    copy(
      shapes = shapes ++ n.shapes,
      comments = comments ++ n.comments,
      gamebook = n.gamebook orElse gamebook,
      glyphs = glyphs merge n.glyphs,
      score = n.score orElse score,
      clock = n.clock orElse clock,
      crazyData = n.crazyData orElse crazyData,
      children = n.children.nodes.foldLeft(children) { case (cs, c) =>
        cs addNode c
      },
      forceVariation = n.forceVariation || forceVariation
    )

  def moveOption = move.some

  override def toString = s"$ply.${move.san}"
}

object Node {

  val MAX_PLIES = 400

  case class Children(nodes: Vector[Node]) extends AnyVal {

    def first      = nodes.headOption
    def variations = nodes drop 1

    def nodeAt(path: Path): Option[Node] =
      path.split flatMap {
        case (head, tail) if tail.isEmpty => get(head)
        case (head, tail)                 => get(head) flatMap (_.children nodeAt tail)
      }

    // def nodesOn(path: Path): List[(Node, Path)] =
    //   path.split ?? { case (head, tail) =>
    //     get(head) ?? { first =>
    //       (first, Path(List(head))) :: first.children.nodesOn(tail).map { case (n, p) =>
    //         (n, p prepend head)
    //       }
    //     }
    //   }

    def addNodeAt(node: Node, path: Path): Option[Children] =
      path.split match {
        case None               => addNode(node).some
        case Some((head, tail)) => updateChildren(head, _.addNodeAt(node, tail))
      }

    def addNode(node: Node): Children =
      get(node.id).fold(Children(nodes :+ node)) { prev =>
        Children(nodes.filterNot(_.id == node.id) :+ prev.merge(node))
      }

    def deleteNodeAt(path: Path): Option[Children] =
      path.split flatMap {
        case (head, Path(Nil)) if has(head) => Children(nodes.filterNot(_.id == head)).some
        case (_, Path(Nil))                 => none
        case (head, tail)                   => updateChildren(head, _.deleteNodeAt(tail))
      }

    def promoteToMainlineAt(path: Path): Option[Children] =
      path.split match {
        case None => this.some
        case Some((head, tail)) =>
          get(head).flatMap { node =>
            node.withChildren(_.promoteToMainlineAt(tail)).map { promoted =>
              Children(promoted +: nodes.filterNot(node ==))
            }
          }
      }

    def promoteUpAt(path: Path): Option[(Children, Boolean)] =
      path.split match {
        case None => Some(this -> false)
        case Some((head, tail)) =>
          for {
            node                  <- get(head)
            mainlineNode          <- nodes.headOption
            (newChildren, isDone) <- node.children promoteUpAt tail
            newNode = node.copy(children = newChildren)
          } yield {
            if (isDone) update(newNode) -> true
            else if (newNode.id == mainlineNode.id) update(newNode) -> false
            else Children(newNode +: nodes.filterNot(newNode ==))   -> true
          }
      }

    def updateAt(path: Path, f: Node => Node): Option[Children] =
      path.split flatMap {
        case (head, Path(Nil)) => updateWith(head, n => Some(f(n)))
        case (head, tail)      => updateChildren(head, _.updateAt(tail, f))
      }

    def get(id: UciCharPair): Option[Node] = nodes.find(_.id == id)

    def getNodeAndIndex(id: UciCharPair): Option[(Node, Int)] =
      nodes.zipWithIndex.collectFirst {
        case pair if pair._1.id == id => pair
      }

    def has(id: UciCharPair): Boolean = nodes.exists(_.id == id)

    def updateAllWith(op: Node => Node): Children =
      Children {
        nodes.map { n =>
          op(n.copy(children = n.children.updateAllWith(op)))
        }
      }

    def updateWith(id: UciCharPair, op: Node => Option[Node]): Option[Children] =
      get(id) flatMap op map update

    def updateChildren(id: UciCharPair, f: Children => Option[Children]): Option[Children] =
      updateWith(id, _ withChildren f)

    def update(child: Node): Children =
      Children(nodes.map {
        case n if child.id == n.id => child
        case n                     => n
      })

    def updateMainline(f: Node => Node): Children =
      Children(nodes match {
        case main +: others =>
          val newNode = f(main)
          newNode.copy(children = newNode.children.updateMainline(f)) +: others
        case x => x
      })

    // List(0, 0, 1, 0, 2)
    def pathToIndexes(path: Path): Option[List[Int]] =
      path.split.fold(List.empty[Int].some) { case (head, tail) =>
        getNodeAndIndex(head) flatMap { case (node, index) =>
          node.children.pathToIndexes(tail).map(rest => index :: rest)
        }
      }

    def countRecursive: Int =
      nodes.foldLeft(nodes.size) { case (count, n) =>
        count + n.children.countRecursive
      }

    def lastMainlineNode: Option[Node] =
      nodes.headOption map { first =>
        first.children.lastMainlineNode | first
      }

    override def toString = nodes.mkString(", ")
  }
  val emptyChildren = Children(Vector.empty)

  case class Root(
      ply: Int,
      fen: FEN,
      check: Boolean,
      shapes: Shapes = Shapes(Nil),
      comments: Comments = Comments(Nil),
      gamebook: Option[Gamebook] = None,
      glyphs: Glyphs = Glyphs.empty,
      score: Option[Score] = None,
      clock: Option[Centis],
      crazyData: Option[Crazyhouse.Data],
      children: Children
  ) extends RootOrNode {

    def withChildren(f: Children => Option[Children]) =
      f(children) map { newChildren =>
        copy(children = newChildren)
      }

    def withoutChildren = copy(children = Node.emptyChildren)

    def addChild(child: Node) = copy(children = children addNode child)

    def nodeAt(path: Path): Option[RootOrNode] =
      if (path.isEmpty) this.some else children nodeAt path

    def pathExists(path: Path): Boolean = nodeAt(path).isDefined

    def setShapesAt(shapes: Shapes, path: Path): Option[Root] =
      if (path.isEmpty) copy(shapes = shapes).some
      else updateChildrenAt(path, _ setShapes shapes)

    def setCommentAt(comment: Comment, path: Path): Option[Root] =
      if (path.isEmpty) copy(comments = comments set comment).some
      else updateChildrenAt(path, _ setComment comment)

    def deleteCommentAt(commentId: Comment.Id, path: Path): Option[Root] =
      if (path.isEmpty) copy(comments = comments delete commentId).some
      else updateChildrenAt(path, _ deleteComment commentId)

    def setGamebookAt(gamebook: Gamebook, path: Path): Option[Root] =
      if (path.isEmpty) copy(gamebook = gamebook.some).some
      else updateChildrenAt(path, _ setGamebook gamebook)

    def toggleGlyphAt(glyph: Glyph, path: Path): Option[Root] =
      if (path.isEmpty) copy(glyphs = glyphs toggle glyph).some
      else updateChildrenAt(path, _ toggleGlyph glyph)

    def setClockAt(clock: Option[Centis], path: Path): Option[Root] =
      if (path.isEmpty) copy(clock = clock).some
      else updateChildrenAt(path, _ withClock clock)

    def forceVariationAt(force: Boolean, path: Path): Option[Root] =
      if (path.isEmpty) copy(clock = clock).some
      else updateChildrenAt(path, _ withForceVariation force)

    private def updateChildrenAt(path: Path, f: Node => Node): Option[Root] =
      withChildren(_.updateAt(path, f))

    def updateMainlineLast(f: Node => Node): Root =
      children.first.fold(this) { main =>
        copy(children = children.update(main updateMainlineLast f))
      }

    lazy val mainline: Vector[Node] = children.first.??(_.mainline)

    def lastMainlinePly = Chapter.Ply(mainline.lastOption.??(_.ply))

    def lastMainlinePlyOf(path: Path) =
      Chapter.Ply {
        mainline
          .zip(path.ids)
          .takeWhile { case (node, id) =>
            node.id == id
          }
          .lastOption
          .?? { case (node, _) =>
            node.ply
          }
      }

    def mainlinePath = Path(mainline.map(_.id))

    def lastMainlineNode: RootOrNode = children.lastMainlineNode getOrElse this

    def moveOption = none

    override def toString = "ROOT"
  }

  object Root {

    def default(variant: chess.variant.Variant) =
      Root(
        ply = 0,
        fen = variant.initialFen,
        check = false,
        clock = none,
        crazyData = variant.crazyhouse option Crazyhouse.Data.init,
        children = emptyChildren
      )

    def fromRoot(b: lila.tree.Root): Root =
      Root(
        ply = b.ply,
        fen = b.fen,
        check = b.check,
        clock = b.clock,
        crazyData = b.crazyData,
        children = Children(b.children.view.map(fromBranch).toVector)
      )
  }

  def fromBranch(b: lila.tree.Branch): Node =
    Node(
      id = b.id,
      ply = b.ply,
      move = b.move,
      fen = b.fen,
      check = b.check,
      crazyData = b.crazyData,
      clock = b.clock,
      children = Children(b.children.view.map(fromBranch).toVector),
      forceVariation = false
    )
}
