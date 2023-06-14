package lila.tree

import alleycats.Zero
import chess.Centis
import chess.format.pgn.{ Glyph, Glyphs }
import chess.format.{ Fen, Uci, UciCharPair, UciPath }
import chess.opening.Opening
import chess.{ Ply, Square, Check }
import chess.variant.{ Variant, Crazyhouse }
import play.api.libs.json.*
import ornicar.scalalib.ThreadLocalRandom

import lila.common.Json.given
import Node.{ Comments, Comment, Gamebook, Shapes }

//opaque type not working due to cyclic ref try again later
// either we decide that branches strictly represent all the children from a node
// with the first being the mainline, OR we just use it as an List with extra functionalities
case class Branches(nodes: List[Branch]) extends AnyVal:
  def first      = nodes.headOption
  def variations = nodes.drop(1)
  def isEmpty    = nodes.isEmpty
  def nonEmpty   = !isEmpty

  def ::(b: Branch) = Branches(b :: nodes)
  def :+(b: Branch) = Branches(nodes :+ b)

  def get(id: UciCharPair): Option[Branch] = nodes.find(_.id == id)
  def hasNode(id: UciCharPair): Boolean    = nodes.exists(_.id == id)

  def getNodeAndIndex(id: UciCharPair): Option[(Branch, Int)] =
    nodes.zipWithIndex.collectFirst {
      case pair if pair._1.id == id => pair
    }

  def nodeAt(path: UciPath): Option[Branch] =
    path.split.flatMap { (head, rest) =>
      rest.computeIds.foldLeft(get(head)) { (cur, id) =>
        cur.flatMap(_.children.get(id))
      }
    }

  // select all nodes on that path
  def nodesOn(path: UciPath): Vector[(Branch, UciPath)] =
    path.split
      .so { (head, tail) =>
        get(head).so { first =>
          (first, UciPath.fromId(head)) +: first.children.nodesOn(tail).map { (n, p) =>
            (n, p prepend head)
          }
        }
      }

  def addNodeAt(node: Branch, path: UciPath): Option[Branches] =
    path.split match
      case None               => addNode(node).some
      case Some((head, tail)) => updateChildren(head, _.addNodeAt(node, tail))

  // suboptimal due to using List instead of Vector
  def addNode(node: Branch): Branches =
    Branches(get(node.id).fold(nodes :+ node) { prev =>
      nodes.filterNot(_.id == node.id) :+ prev.merge(node)
    })

  def deleteNodeAt(path: UciPath): Option[Branches] =
    path.split flatMap {
      case (head, p) if p.isEmpty && hasNode(head) => Branches(nodes.filterNot(_.id == head)).some
      case (_, p) if p.isEmpty                     => none
      case (head, tail)                            => updateChildren(head, _.deleteNodeAt(tail))
    }

  def promoteToMainlineAt(path: UciPath): Option[Branches] =
    path.split match
      case None => this.some
      case Some((head, tail)) =>
        get(head).flatMap { node =>
          node.withChildren(_.promoteToMainlineAt(tail)).map { promoted =>
            Branches(promoted :: nodes.filterNot(node ==))
          }
        }

  def promoteUpAt(path: UciPath): Option[(Branches, Boolean)] =
    path.split match
      case None => Some(this -> false)
      case Some((head, tail)) =>
        for {
          node                  <- get(head)
          mainlineNode          <- this.first
          (newChildren, isDone) <- node.children promoteUpAt tail
          newNode = node.copy(children = newChildren)
        } yield
          if (isDone) update(newNode) -> true
          else if (newNode.id == mainlineNode.id) update(newNode) -> false
          else Branches(newNode :: nodes.filterNot(newNode ==))   -> true

  def updateAt(path: UciPath, f: Branch => Branch): Option[Branches] =
    path.split flatMap {
      case (head, p) if p.isEmpty => updateWith(head, n => Some(f(n)))
      case (head, tail)           => updateChildren(head, _.updateAt(tail, f))
    }

  def updateAllWith(op: Branch => Branch): Branches =
    Branches(nodes.map { n =>
      op(n.copy(children = n.children.updateAllWith(op)))
    })

  def update(child: Branch): Branches =
    Branches(nodes.map {
      case n if child.id == n.id => child
      case n                     => n
    })

  def updateWith(id: UciCharPair, op: Branch => Option[Branch]): Option[Branches] =
    get(id) flatMap op map update

  def updateChildren(id: UciCharPair, f: Branches => Option[Branches]): Option[Branches] =
    updateWith(id, _ withChildren f)

  def updateMainline(f: Branch => Branch): Branches =
    Branches(nodes match {
      case main :: others =>
        val newNode = f(main)
        newNode.copy(children = newNode.children.updateMainline(f)) :: others
      case x => x
    })

  def takeMainlineWhile(f: Branch => Boolean): Branches =
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
    this.first map { first =>
      first.children.lastMainlineNode | first
    }

  override def toString = nodes.mkString(", ")

object Branches:
  // already included by `TotalWrapper`?
  // def apply(branches: List[Branch]): Branches = branches
  val empty: Branches  = Branches(Nil)
  given Zero[Branches] = Zero(empty)

sealed trait Node:
  def ply: Ply
  def fen: Fen.Epd
  def check: Check
  // None when not computed yet
  def dests: Option[Map[Square, List[Square]]]
  def drops: Option[List[Square]]
  def eval: Option[Eval]
  def shapes: Node.Shapes
  def comments: Node.Comments
  def gamebook: Option[Node.Gamebook]
  def glyphs: Glyphs
  def children: Branches
  def opening: Option[Opening]
  def comp: Boolean // generated by a computer analysis
  def crazyData: Option[Crazyhouse.Data]
  def addChild(branch: Branch): Node
  def dropFirstChild: Node
  def clock: Option[Centis]
  def forceVariation: Boolean

  // implementation dependent
  def idOption: Option[UciCharPair]
  def moveOption: Option[Uci.WithSan]

  // who's color plays next
  def color = ply.turn

  def mainlineNodeList: List[Node] =
    dropFirstChild :: children.first.fold(List.empty[Node])(_.mainlineNodeList)

case class Root(
    ply: Ply,
    fen: Fen.Epd,
    check: Check,
    // None when not computed yet
    dests: Option[Map[Square, List[Square]]] = None,
    drops: Option[List[Square]] = None,
    eval: Option[Eval] = None,
    shapes: Node.Shapes = Node.Shapes(Nil),
    comments: Node.Comments = Node.Comments(Nil),
    gamebook: Option[Node.Gamebook] = None,
    glyphs: Glyphs = Glyphs.empty,
    children: Branches = Branches.empty,
    opening: Option[Opening] = None,
    clock: Option[Centis] = None, // clock state at game start, assumed same for both players
    crazyData: Option[Crazyhouse.Data]
) extends Node:

  def idOption       = None
  def moveOption     = None
  def comp           = false
  def forceVariation = false

  // def addChild(branch: Branch)     = copy(children = children :+ branch)
  def addChild(child: Branch)      = copy(children = children addNode child)
  def prependChild(branch: Branch) = copy(children = branch :: children)
  def dropFirstChild = copy(children = if (children.isEmpty) children else Branches(children.variations))

  def withChildren(f: Branches => Option[Branches]) =
    f(children) map { newChildren =>
      copy(children = newChildren)
    }

  def withoutChildren = copy(children = Branches.empty)

  def nodeAt(path: UciPath): Option[Node] =
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

  private def updateChildrenAt(path: UciPath, f: Branch => Branch): Option[Root] =
    withChildren(_.updateAt(path, f))

  def updateMainlineLast(f: Branch => Branch): Root =
    children.first.fold(this) { main =>
      copy(children = children.update(main updateMainlineLast f))
    }

  def clearVariations =
    copy(
      children = children.first.fold(Branches.empty) { child =>
        Branches(List(child.clearVariations))
      }
    )

  // NOT `Branches` as it does not represent one mainline move and variations
  // but only mainline moves
  lazy val mainline: List[Branch] = children.first.so(_.mainline)

  def lastMainlinePly = mainline.lastOption.fold(Ply.initial)(_.ply)

  def lastMainlinePlyOf(path: UciPath) =
    mainline
      .zip(path.computeIds)
      .takeWhile { (node, id) => node.id == id }
      .lastOption
      .fold(Ply.initial) { (node, _) => node.ply }

  def mainlinePath = UciPath.fromIds(mainline.map(_.id))

  def lastMainlineNode: Node = children.lastMainlineNode getOrElse this

  def takeMainlineWhile(f: Branch => Boolean) =
    if children.first.isDefined
    then copy(children = children.takeMainlineWhile(f))
    else this

  def merge(n: Root): Root = copy(
    shapes = shapes ++ n.shapes,
    comments = comments ++ n.comments,
    gamebook = n.gamebook orElse gamebook,
    glyphs = glyphs merge n.glyphs,
    eval = n.eval orElse eval,
    clock = n.clock orElse clock,
    crazyData = n.crazyData orElse crazyData,
    children = n.children.nodes.foldLeft(children)(_ addNode _)
  )

  override def toString = s"$ply $children"

object Root:
  def default(variant: Variant) =
    Root(
      ply = Ply.initial,
      fen = variant.initialFen,
      check = Check.No,
      crazyData = variant.crazyhouse option Crazyhouse.Data.init
    )

case class Branch(
    id: UciCharPair,
    ply: Ply,
    move: Uci.WithSan,
    fen: Fen.Epd,
    check: Check,
    // None when not computed yet
    dests: Option[Map[Square, List[Square]]] = None,
    drops: Option[List[Square]] = None,
    eval: Option[Eval] = None,
    shapes: Node.Shapes = Node.Shapes(Nil),
    comments: Node.Comments = Node.Comments(Nil),
    gamebook: Option[Node.Gamebook] = None,
    glyphs: Glyphs = Glyphs.empty,
    children: Branches = Branches.empty, // Vector used in `Study.Node`, switch?
    opening: Option[Opening] = None,
    comp: Boolean = false,
    clock: Option[Centis] = None, // clock state after the move is played, and the increment applied
    crazyData: Option[Crazyhouse.Data],
    forceVariation: Boolean = false // cannot be mainline
) extends Node:

  def idOption   = Some(id)
  def moveOption = Some(move)

  // NOT `Branches` as it does not represent one mainline move and variations
  // but only mainline moves
  def mainline: List[Branch] = this :: children.first.so(_.mainline)

  def withChildren(f: Branches => Option[Branches]) =
    f(children) map { newChildren =>
      copy(children = newChildren)
    }

  def withoutChildren = copy(children = Branches.empty)

  def addChild(branch: Branch): Branch = copy(children = children :+ branch)

  def withClock(centis: Option[Centis])  = copy(clock = centis)
  def withForceVariation(force: Boolean) = copy(forceVariation = force)

  def isCommented                          = comments.value.nonEmpty
  def setComment(comment: Comment)         = copy(comments = comments set comment)
  def deleteComment(commentId: Comment.Id) = copy(comments = comments delete commentId)
  def deleteComments                       = copy(comments = Comments.empty)
  def setGamebook(gamebook: Gamebook)      = copy(gamebook = gamebook.some)
  def setShapes(s: Shapes)                 = copy(shapes = s)
  def toggleGlyph(glyph: Glyph)            = copy(glyphs = glyphs toggle glyph)

  def updateMainlineLast(f: Branch => Branch): Branch =
    children.first.fold(f(this)) { main =>
      copy(children = children.update(main updateMainlineLast f))
    }

  def clearAnnotations =
    copy(
      comments = Comments(Nil),
      shapes = Shapes(Nil),
      glyphs = Glyphs.empty
    )

  def clearVariations: Branch =
    copy(
      children = children.first.fold(Branches.empty) { child => Branches(List(child.clearVariations)) }
    )

  def prependChild(branch: Branch) = copy(children = branch :: children)
  def dropFirstChild = copy(children = if (children.isEmpty) children else Branches(children.variations))

  def setComp = copy(comp = true)

  def merge(n: Branch): Branch =
    copy(
      shapes = shapes ++ n.shapes,
      comments = comments ++ n.comments,
      gamebook = n.gamebook orElse gamebook,
      glyphs = glyphs merge n.glyphs,
      eval = n.eval orElse eval,
      clock = n.clock orElse clock,
      crazyData = n.crazyData orElse crazyData,
      children = n.children.nodes.foldLeft(children) { (cs, c) =>
        cs addNode c
      },
      forceVariation = n.forceVariation || forceVariation
    )

  override def toString = s"$ply.${move.san} (Branches: $children)"

object Node:

  enum Shape:
    case Circle(brush: Shape.Brush, orig: Square)
    case Arrow(brush: Shape.Brush, orig: Square, dest: Square)
  object Shape:
    type Brush = String

  opaque type Shapes = List[Shape]
  object Shapes extends TotalWrapper[Shapes, List[Shape]]:
    extension (a: Shapes) def ++(shapes: Shapes): Shapes = (a.value ::: shapes.value).distinct
    val empty: Shapes                                    = Nil

  case class Comment(id: Comment.Id, text: Comment.Text, by: Comment.Author):
    def removeMeta = text.removeMeta map { t => copy(text = t) }
  object Comment:
    opaque type Id = String
    object Id extends OpaqueString[Id]:
      def make = Id(ThreadLocalRandom nextString 4)
    private val metaReg = """\[%[^\]]++\]""".r
    opaque type Text = String
    object Text extends OpaqueString[Text]:
      extension (a: Text)
        def removeMeta: Option[Text] =
          val v = metaReg.replaceAllIn(a.value, "").trim
          v.nonEmpty option Text(v)
    enum Author:
      case User(id: UserId, titleName: String)
      case External(name: String)
      case Lichess
      case Unknown

      def is(other: Author) = (this, other) match
        case (User(a, _), User(b, _)) => a == b
        case _                        => this == other

    def sanitize(text: String) = Text:
      lila.common.String
        .softCleanUp(text)
        .take(4000)
        .replaceAll("""\r\n""", "\n") // these 3 lines dedup white spaces and new lines
        .replaceAll("""(?m)(^ *| +(?= |$))""", "")
        .replaceAll("""(?m)^$([\n]+?)(^$[\n]+?^)+""", "$1")
        .replaceAll("[{}]", "") // {} are reserved in PGN comments

  opaque type Comments = List[Comment]
  object Comments extends TotalWrapper[Comments, List[Comment]]:
    extension (a: Comments)
      def findBy(author: Comment.Author) = a.value.find(_.by is author)
      def set(comment: Comment): Comments = {
        if (a.value.exists(_.by.is(comment.by))) a.value.map {
          case c if c.by.is(comment.by) => c.copy(text = comment.text, by = comment.by)
          case c                        => c
        }
        else a.value :+ comment
      }
      def delete(commentId: Comment.Id): Comments = a.value.filterNot(_.id == commentId)
      def +(comment: Comment): Comments           = comment :: a.value
      def ++(comments: Comments): Comments        = a.value ::: comments.value
      def filterEmpty: Comments                   = a.value.filter(_.text.value.nonEmpty)
      def hasLichessComment                       = a.value.exists(_.by == Comment.Author.Lichess)
    val empty = Comments(Nil)

  case class Gamebook(deviation: Option[String], hint: Option[String]):
    private def trimOrNone(txt: Option[String]) = txt.map(_.trim).filter(_.nonEmpty)
    def cleanUp =
      copy(
        deviation = trimOrNone(deviation),
        hint = trimOrNone(hint)
      )
    def nonEmpty = deviation.nonEmpty || hint.nonEmpty

  given OWrites[chess.opening.Opening] = OWrites { o =>
    Json.obj(
      "eco"  -> o.eco,
      "name" -> o.name
    )
  }

  private given Writes[Square] = Writes[Square] { p =>
    JsString(p.key)
  }
  private val shapeCircleWrites = Json.writes[Shape.Circle]
  private val shapeArrowWrites  = Json.writes[Shape.Arrow]
  given shapeWrites: Writes[Shape] = Writes[Shape] {
    case s: Shape.Circle => shapeCircleWrites writes s
    case s: Shape.Arrow  => shapeArrowWrites writes s
  }
  given Writes[Node.Shapes] = Writes[Node.Shapes] { s =>
    JsArray(s.value.map(shapeWrites.writes))
  }
  given Writes[Glyph] = Json.writes[Glyph]
  given Writes[Glyphs] = Writes[Glyphs] { gs =>
    Json.toJson(gs.toList)
  }

  given Writes[Centis] = Writes { clock =>
    JsNumber(clock.centis)
  }
  given Writes[Comment.Id] = Writes { id =>
    JsString(id.value)
  }
  given Writes[Comment.Text] = Writes { text =>
    JsString(text.value)
  }
  given Writes[Comment.Author] = Writes[Comment.Author] {
    case Comment.Author.User(id, name) => Json.obj("id" -> id.value, "name" -> name)
    case Comment.Author.External(name) => JsString(s"${name.trim}")
    case Comment.Author.Lichess        => JsString("lichess")
    case Comment.Author.Unknown        => JsNull
  }
  given Writes[Node.Comment]  = Json.writes[Node.Comment]
  given Writes[Node.Gamebook] = Json.writes[Node.Gamebook]

  import lila.common.Json.given
  import JsonHandlers.given

  given defaultNodeJsonWriter: Writes[Node] = makeNodeJsonWriter(alwaysChildren = true)

  val minimalNodeJsonWriter = makeNodeJsonWriter(alwaysChildren = false)

  def nodeListJsonWriter(alwaysChildren: Boolean): Writes[List[Node]] =
    Writes: list =>
      val writer = if alwaysChildren then defaultNodeJsonWriter else minimalNodeJsonWriter
      JsArray(list map writer.writes)

  def makeNodeJsonWriter(alwaysChildren: Boolean): Writes[Node] =
    Writes: node =>
      import node.*
      try
        val comments = node.comments.value.flatMap(_.removeMeta)
        Json
          .obj(
            "ply" -> ply,
            "fen" -> fen
          )
          .add("id", idOption.map(_.toString))
          .add("uci", moveOption.map(_.uci.uci))
          .add("san", moveOption.map(_.san))
          .add("check", check)
          .add("eval", eval.filterNot(_.isEmpty))
          .add("comments", if (comments.nonEmpty) Some(comments) else None)
          .add("gamebook", gamebook)
          .add("glyphs", glyphs.nonEmpty)
          .add("shapes", if (shapes.value.nonEmpty) Some(shapes.value) else None)
          .add("opening", opening)
          .add("dests", dests)
          .add("drops", drops.map(drops => JsString(drops.map(_.key).mkString)))
          .add("clock", clock)
          .add("crazy", crazyData)
          .add("comp", comp)
          .add(
            "children",
            if (alwaysChildren || children.nonEmpty) Some:
              nodeListJsonWriter(true) writes children.nodes
            else None
          )
          .add("forceVariation", forceVariation)
      catch
        case e: StackOverflowError =>
          e.printStackTrace()
          sys error s"### StackOverflowError ### in tree.makeNodeJsonWriter($alwaysChildren)"

  def destString(dests: Map[Square, List[Square]]): String =
    val sb    = java.lang.StringBuilder(80)
    var first = true
    dests.foreach: (orig, dests) =>
      if first then first = false
      else sb append " "
      sb append orig.asChar
      dests foreach { sb append _.asChar }
    sb.toString

  given Writes[Map[Square, List[Square]]] = Writes: dests =>
    JsString(destString(dests))

  val partitionTreeJsonWriter: Writes[Node] = Writes: node =>
    JsArray:
      node.mainlineNodeList.map(minimalNodeJsonWriter.writes)
