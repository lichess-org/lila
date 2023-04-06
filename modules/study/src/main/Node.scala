package lila.study

import chess.format.pgn.{ Glyph, Glyphs }
import chess.format.{ Fen, Uci, UciCharPair, UciPath }
import chess.variant.Crazyhouse

import chess.{ Ply, Centis, Check }
import lila.tree.Eval.Score
import lila.tree.Node.{ Comment, Comments, Gamebook, Shapes }

sealed trait RootOrNode:
  val ply: Ply
  val fen: Fen.Epd
  val check: Check
  val shapes: Shapes
  val clock: Option[Centis]
  val crazyData: Option[Crazyhouse.Data]
  val children: Node.Children
  val comments: Comments
  val gamebook: Option[Gamebook]
  val glyphs: Glyphs
  val score: Option[Score]
  def addChild(node: Node): RootOrNode
  def mainline: Vector[Node]
  def moveOption: Option[Uci.WithSan]
  def color          = ply.color
  def fullMoveNumber = ply.fullMoveNumber

case class Node(
    id: UciCharPair,
    ply: Ply,
    move: Uci.WithSan,
    fen: Fen.Epd,
    check: Check,
    shapes: Shapes = Shapes(Nil),
    comments: Comments = Comments(Nil),
    gamebook: Option[Gamebook] = None,
    glyphs: Glyphs = Glyphs.empty,
    score: Option[Score] = None,
    clock: Option[Centis],
    crazyData: Option[Crazyhouse.Data],
    children: Node.Children,
    forceVariation: Boolean
) extends RootOrNode:

  import Node.Children

  def withChildren(f: Children => Option[Children]) =
    f(children) map { newChildren =>
      copy(children = newChildren)
    }

  def withoutChildren = copy(children = Node.emptyChildren)

  def addChild(child: Node): Node = copy(children = children addNode child)

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
      glyphs = Glyphs.empty
    )

  def clearVariations: Node =
    copy(
      children = children.first.fold(Node.emptyChildren) { child => Children(Vector(child.clearVariations)) }
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
      children = n.children.nodes.foldLeft(children) { (cs, c) =>
        cs addNode c
      },
      forceVariation = n.forceVariation || forceVariation
    )

  def moveOption = move.some

  override def toString = s"$ply.${move.san}"

object Node:

  val MAX_PLIES = 400

  case class Children(nodes: Vector[Node]) extends AnyVal:

    def first      = nodes.headOption
    def variations = nodes drop 1

    def nodeAt(path: UciPath): Option[Node] =
      path.split.flatMap { (head, rest) =>
        rest.computeIds.foldLeft(get(head)) { (cur, id) =>
          cur.flatMap(_.children.get(id))
        }
      }

    // select all nodes on that path
    def nodesOn(path: UciPath): Vector[(Node, UciPath)] =
      path.split
        .?? { (head, tail) =>
          get(head).?? { first =>
            (first, UciPath.fromId(head)) +: first.children.nodesOn(tail).map { (n, p) =>
              (n, p prepend head)
            }
          }
        }

    def addNodeAt(node: Node, path: UciPath): Option[Children] =
      path.split match
        case None               => addNode(node).some
        case Some((head, tail)) => updateChildren(head, _.addNodeAt(node, tail))

    def addNode(node: Node): Children =
      get(node.id).fold(Children(nodes :+ node)) { prev =>
        Children(nodes.filterNot(_.id == node.id) :+ prev.merge(node))
      }

    def deleteNodeAt(path: UciPath): Option[Children] =
      path.split flatMap {
        case (head, p) if p.isEmpty && has(head) => Children(nodes.filterNot(_.id == head)).some
        case (_, p) if p.isEmpty                 => none
        case (head, tail)                        => updateChildren(head, _.deleteNodeAt(tail))
      }

    def promoteToMainlineAt(path: UciPath): Option[Children] =
      path.split match
        case None => this.some
        case Some((head, tail)) =>
          get(head).flatMap { node =>
            node.withChildren(_.promoteToMainlineAt(tail)).map { promoted =>
              Children(promoted +: nodes.filterNot(node ==))
            }
          }

    def promoteUpAt(path: UciPath): Option[(Children, Boolean)] =
      path.split match
        case None => Some(this -> false)
        case Some((head, tail)) =>
          for {
            node                  <- get(head)
            mainlineNode          <- nodes.headOption
            (newChildren, isDone) <- node.children promoteUpAt tail
            newNode = node.copy(children = newChildren)
          } yield
            if (isDone) update(newNode) -> true
            else if (newNode.id == mainlineNode.id) update(newNode) -> false
            else Children(newNode +: nodes.filterNot(newNode ==))   -> true

    def updateAt(path: UciPath, f: Node => Node): Option[Children] =
      path.split flatMap {
        case (head, p) if p.isEmpty => updateWith(head, n => Some(f(n)))
        case (head, tail)           => updateChildren(head, _.updateAt(tail, f))
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

    def takeMainlineWhile(f: Node => Boolean): Children =
      updateMainline { node =>
        node.children.first.fold(node) { mainline =>
          if (f(mainline)) node
          else node.withoutChildren
        }
      }

    def countRecursive: Int =
      nodes.foldLeft(nodes.size) { (count, n) =>
        count + n.children.countRecursive
      }

    def lastMainlineNode: Option[Node] =
      nodes.headOption map { first =>
        first.children.lastMainlineNode | first
      }

    override def toString = nodes.mkString(", ")
  val emptyChildren = Children(Vector.empty)

  case class Root(
      ply: Ply,
      fen: Fen.Epd,
      check: Check,
      shapes: Shapes = Shapes(Nil),
      comments: Comments = Comments(Nil),
      gamebook: Option[Gamebook] = None,
      glyphs: Glyphs = Glyphs.empty,
      score: Option[Score] = None,
      clock: Option[Centis],
      crazyData: Option[Crazyhouse.Data],
      children: Children
  ) extends RootOrNode:

    def withChildren(f: Children => Option[Children]) =
      f(children) map { newChildren =>
        copy(children = newChildren)
      }

    def withoutChildren = copy(children = Node.emptyChildren)

    def addChild(child: Node) = copy(children = children addNode child)

    def nodeAt(path: UciPath): Option[RootOrNode] =
      if (path.isEmpty) this.some else children nodeAt path

    def pathExists(path: UciPath): Boolean = nodeAt(path).isDefined

    def setShapesAt(shapes: Shapes, path: UciPath): Option[Root] =
      if (path.isEmpty) copy(shapes = shapes).some
      else updateChildrenAt(path, _ setShapes shapes)

    def setCommentAt(comment: Comment, path: UciPath): Option[Root] =
      if (path.isEmpty) copy(comments = comments set comment).some
      else updateChildrenAt(path, _ setComment comment)

    def deleteCommentAt(commentId: Comment.Id, path: UciPath): Option[Root] =
      if (path.isEmpty) copy(comments = comments delete commentId).some
      else updateChildrenAt(path, _ deleteComment commentId)

    def setGamebookAt(gamebook: Gamebook, path: UciPath): Option[Root] =
      if (path.isEmpty) copy(gamebook = gamebook.some).some
      else updateChildrenAt(path, _ setGamebook gamebook)

    def toggleGlyphAt(glyph: Glyph, path: UciPath): Option[Root] =
      if (path.isEmpty) copy(glyphs = glyphs toggle glyph).some
      else updateChildrenAt(path, _ toggleGlyph glyph)

    def setClockAt(clock: Option[Centis], path: UciPath): Option[Root] =
      if (path.isEmpty) copy(clock = clock).some
      else updateChildrenAt(path, _ withClock clock)

    def forceVariationAt(force: Boolean, path: UciPath): Option[Root] =
      if (path.isEmpty) copy(clock = clock).some
      else updateChildrenAt(path, _ withForceVariation force)

    private def updateChildrenAt(path: UciPath, f: Node => Node): Option[Root] =
      withChildren(_.updateAt(path, f))

    def updateMainlineLast(f: Node => Node): Root =
      children.first.fold(this) { main =>
        copy(children = children.update(main updateMainlineLast f))
      }

    def clearVariations =
      copy(
        children = children.first.fold(Node.emptyChildren) { child =>
          Children(Vector(child.clearVariations))
        }
      )

    lazy val mainline: Vector[Node] = children.first.??(_.mainline)

    def lastMainlinePly = mainline.lastOption.fold(Ply(0))(_.ply)

    def lastMainlinePlyOf(path: UciPath) =
      mainline
        .zip(path.computeIds)
        .takeWhile { (node, id) => node.id == id }
        .lastOption
        .fold(Ply(0)) { (node, _) => node.ply }

    def mainlinePath = UciPath.fromIds(mainline.map(_.id))

    def lastMainlineNode: RootOrNode = children.lastMainlineNode getOrElse this

    def takeMainlineWhile(f: Node => Boolean) =
      if children.first.isDefined
      then copy(children = children.takeMainlineWhile(f))
      else this

    def moveOption = none

    override def toString = "ROOT"

  object Root:

    def default(variant: chess.variant.Variant) =
      Root(
        ply = Ply(0),
        fen = variant.initialFen,
        check = Check.No,
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

  object BsonFields:
    val ply            = "p"
    val uci            = "u"
    val san            = "s"
    val fen            = "f"
    val check          = "c"
    val shapes         = "h"
    val comments       = "co"
    val gamebook       = "ga"
    val glyphs         = "g"
    val score          = "e"
    val clock          = "l"
    val crazy          = "z"
    val forceVariation = "fv"
    val order          = "o"
