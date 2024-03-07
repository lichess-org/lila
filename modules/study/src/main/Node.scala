package lila.study

import shogi.format.{ Glyph, Glyphs }
import shogi.format.forsyth.Sfen
import shogi.format.usi.{ Usi, UsiCharPair }
import shogi.variant.Variant
import shogi.Centis

import lila.tree.Eval.Score
import lila.tree.Node.{ Comment, Comments, Gamebook, Shapes }

sealed trait RootOrNode {
  val ply: Int
  val sfen: Sfen
  val check: Boolean
  val shapes: Shapes
  val clock: Option[Centis]
  val children: Node.Children
  val comments: Comments
  val gamebook: Option[Gamebook]
  val glyphs: Glyphs
  val score: Option[Score]
  def addChild(node: Node): RootOrNode
  def mainline: Vector[Node]
  def color = shogi.Color.fromPly(ply)
  def usiOption: Option[Usi]
  def idOption: Option[UsiCharPair]
  def forceVariation: Boolean
  def dropFirstChild: RootOrNode
}

case class Node(
    id: UsiCharPair,
    ply: Int,
    usi: Usi,
    sfen: Sfen,
    check: Boolean,
    shapes: Shapes = Shapes(Nil),
    comments: Comments = Comments(Nil),
    gamebook: Option[Gamebook] = None,
    glyphs: Glyphs = Glyphs.empty,
    score: Option[Score] = None,
    clock: Option[Centis],
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

  def dropFirstChild = copy(children = Children(children.variations))

  def isCommented = comments.value.nonEmpty

  def setComment(comment: Comment)         = copy(comments = comments set comment)
  def deleteComment(commentId: Comment.Id) = copy(comments = comments delete commentId)
  def deleteComments                       = copy(comments = Comments.empty)

  def setGamebook(gamebook: Gamebook) = copy(gamebook = gamebook.some)

  def setShapes(s: Shapes) = copy(shapes = s)

  def toggleGlyph(glyph: Glyph) = copy(glyphs = glyphs toggle glyph)

  def mainline: Vector[Node] = this +: children.first.??(_.mainline)

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
      children = n.children.nodes.foldLeft(children) { case (cs, c) =>
        cs addNode c
      },
      forceVariation = n.forceVariation || forceVariation
    )

  def usiOption = usi.some
  def idOption  = id.some

  override def toString = s"$ply.${usi}"
}

object Node {

  val MAX_PLIES = 400

  case class Children(nodes: Vector[Node]) extends AnyVal {

    def first      = nodes.headOption
    def variations = nodes drop 1

    def nodeAt(path: Path): Option[Node] =
      path.split flatMap { case (head, rest) =>
        rest.ids.foldLeft(get(head)) { case (cur, id) =>
          cur.flatMap(_.children.get(id))
        }
      }

    // select all nodes on that path
    def nodesOn(path: Path): Vector[(Node, Path)] =
      path.split ?? { case (head, tail) =>
        get(head) ?? { first =>
          (first, Path(Vector(head))) +: first.children.nodesOn(tail).map { case (n, p) =>
            (n, p prepend head)
          }
        }
      }

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

    def get(id: UsiCharPair): Option[Node] = nodes.find(_.id == id)

    def getNodeAndIndex(id: UsiCharPair): Option[(Node, Int)] =
      nodes.zipWithIndex.collectFirst {
        case pair if pair._1.id == id => pair
      }

    def has(id: UsiCharPair): Boolean = nodes.exists(_.id == id)

    def updateAllWith(op: Node => Node): Children =
      Children {
        nodes.map { n =>
          op(n.copy(children = n.children.updateAllWith(op)))
        }
      }

    def updateWith(id: UsiCharPair, op: Node => Option[Node]): Option[Children] =
      get(id) flatMap op map update

    def updateChildren(id: UsiCharPair, f: Children => Option[Children]): Option[Children] =
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

    def countRecursive: Int =
      nodes.foldLeft(nodes.size) { case (count, n) =>
        count + n.children.countRecursive
      }

    def commentAuthors: List[Comment.Author] = {
      nodes.foldLeft(nodes.toList.map(_.comments.authors).flatten) { case (acc, n) =>
        acc ::: n.children.commentAuthors
      }
    }

    def lastMainlineNode: Option[Node] =
      nodes.headOption map { first =>
        first.children.lastMainlineNode | first
      }

    override def toString = nodes.mkString(", ")
  }
  val emptyChildren = Children(Vector.empty)

  case class GameMainline(
      id: lila.game.Game.ID,
      part: Int, // if game was partitioned into multiple parts
      variant: Variant,
      usis: Vector[Usi],
      initialSfen: Option[Sfen],
      clocks: Option[Vector[Centis]]
  )

  case class GameMainlineExtension(
      shapes: Shapes = Shapes(Nil),
      comments: Comments = Comments(Nil),
      glyphs: Glyphs = Glyphs.empty,
      score: Option[Score] = None
  ) {

    def merge(n: Node): Node =
      n.copy(
        shapes = n.shapes ++ shapes,
        comments = n.comments ++ comments,
        glyphs = n.glyphs merge glyphs,
        score = score
      )

    def merge(r: Node.Root): Node.Root =
      r.copy(
        shapes = r.shapes ++ shapes,
        comments = r.comments ++ comments,
        glyphs = r.glyphs merge glyphs,
        score = score
      )

  }

  case class GameRootHelper(
      gameMainlineExtensions: Option[Map[Int, GameMainlineExtension]],
      variations: Option[Map[Int, Children]]
  )

  case class Root(
      ply: Int,
      sfen: Sfen,
      check: Boolean,
      shapes: Shapes = Shapes(Nil),
      comments: Comments = Comments(Nil),
      gamebook: Option[Gamebook] = None,
      glyphs: Glyphs = Glyphs.empty,
      score: Option[Score] = None,
      clock: Option[Centis] = None,
      gameMainline: Option[GameMainline],
      children: Children
  ) extends RootOrNode {

    def withChildren(f: Children => Option[Children]) =
      f(children) map { newChildren =>
        copy(children = newChildren)
      }

    def withoutChildren = copy(children = Node.emptyChildren)
    def dropFirstChild  = copy(children = Children(children.variations))

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

    lazy val mainlinePath = Path(mainline.map(_.id))

    def lastMainlineNode: RootOrNode = children.lastMainlineNode getOrElse this

    def isGameRoot = gameMainline.isDefined

    def gameMainlinePath: Option[Path] =
      gameMainline.map(gm => mainlinePath.take(gm.usis.size))

    def hasMultipleCommentAuthors: Boolean = (comments.authors ::: children.commentAuthors).toSet.sizeIs > 1

    def usiOption      = none
    def idOption       = none
    def forceVariation = false

    override def toString = "ROOT"
  }

  object Root {

    def default(variant: shogi.variant.Variant) =
      Root(
        ply = 0,
        sfen = variant.initialSfen,
        check = false,
        gameMainline = none,
        children = emptyChildren
      )

  }

  object BsonFields {
    val ply            = "p"
    val usi            = "u"
    val sfen           = "f"
    val check          = "c"
    val shapes         = "h"
    val comments       = "co"
    val gamebook       = "ga"
    val glyphs         = "g"
    val score          = "e"
    val clock          = "l"
    val forceVariation = "fv"
    val order          = "o"

    val gameId      = "id"
    val part        = "pt"
    val variant     = "v"
    val usis        = "um"
    val initialSfen = "is"
    val clocks      = "cl"
  }
}
