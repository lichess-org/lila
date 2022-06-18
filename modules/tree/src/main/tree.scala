package lila.tree

import lila.common.Json._

import play.api.libs.json._

import shogi.format.{ Glyph, Glyphs }
import shogi.format.forsyth.Sfen
import shogi.format.usi.{ Usi, UsiCharPair }
import shogi.opening.FullOpening
import shogi.{ Pos, Piece => ShogiPiece }

import shogi.Centis

sealed trait Node {
  def ply: Int
  def sfen: Sfen
  def check: Boolean
  def eval: Option[Eval]
  def shapes: Node.Shapes
  def comments: Node.Comments
  def gamebook: Option[Node.Gamebook]
  def glyphs: Glyphs
  def children: List[Branch]
  def opening: Option[FullOpening]
  def comp: Boolean // generated by a computer analysis
  def addChild(branch: Branch): Node
  def dropFirstChild: Node
  def clock: Option[Centis]
  def forceVariation: Boolean

  // implementation dependent
  def idOption: Option[UsiCharPair]
  def usiOption: Option[Usi]

  // who's color plays next
  def color = shogi.Color.fromPly(ply)

  def mainlineNodeList: List[Node] =
    dropFirstChild :: children.headOption.fold(List.empty[Node])(_.mainlineNodeList)
}

case class Root(
    ply: Int,
    sfen: Sfen,
    check: Boolean,
    eval: Option[Eval] = None,
    shapes: Node.Shapes = Node.Shapes(Nil),
    comments: Node.Comments = Node.Comments(Nil),
    gamebook: Option[Node.Gamebook] = None,
    glyphs: Glyphs = Glyphs.empty,
    children: List[Branch] = Nil,
    opening: Option[FullOpening] = None,
    clock: Option[Centis] = None // clock state at game start, assumed same for both players
) extends Node {

  def idOption       = None
  def usiOption      = None
  def comp           = false
  def forceVariation = false

  def addChild(branch: Branch)     = copy(children = children :+ branch)
  def prependChild(branch: Branch) = copy(children = branch :: children)
  def dropFirstChild               = copy(children = if (children.isEmpty) children else children.tail)
}

case class Branch(
    id: UsiCharPair,
    ply: Int,
    usi: Usi,
    sfen: Sfen,
    check: Boolean,
    eval: Option[Eval] = None,
    shapes: Node.Shapes = Node.Shapes(Nil),
    comments: Node.Comments = Node.Comments(Nil),
    gamebook: Option[Node.Gamebook] = None,
    glyphs: Glyphs = Glyphs.empty,
    children: List[Branch] = Nil,
    opening: Option[FullOpening] = None,
    comp: Boolean = false,
    clock: Option[Centis] = None, // clock state after the move is played, and the increment applied
    forceVariation: Boolean = false // cannot be mainline
) extends Node {

  def idOption   = Some(id)
  def usiOption  = Some(usi)

  def addChild(branch: Branch)     = copy(children = children :+ branch)
  def prependChild(branch: Branch) = copy(children = branch :: children)
  def dropFirstChild               = copy(children = if (children.isEmpty) children else children.tail)

  def setComp = copy(comp = true)
}

object Node {

  sealed trait Shape
  object Shape {
    type ID    = String
    type Brush = String
    case class Circle(brush: Brush, orig: Pos)                    extends Shape
    case class Arrow(brush: Brush, orig: Pos, dest: Pos)          extends Shape
    case class Piece(brush: Brush, orig: Pos, piece: ShogiPiece)  extends Shape
  }
  case class Shapes(value: List[Shape]) extends AnyVal {
    def list = value
    def ++(shapes: Shapes) =
      Shapes {
        (value ::: shapes.value).distinct
      }
  }
  object Shapes {
    val empty = Shapes(Nil)
  }

  case class Comment(id: Comment.Id, text: Comment.Text, by: Comment.Author) {
    def removeMeta =
      text.removeMeta map { t =>
        copy(text = t)
      }
  }
  object Comment {
    case class Id(value: String) extends AnyVal
    object Id {
      def make = Id(lila.common.ThreadLocalRandom nextString 4)
    }
    private val metaReg = """\[%[^\]]+\]""".r
    case class Text(value: String) extends AnyVal {
      def removeMeta: Option[Text] = {
        val v = metaReg.replaceAllIn(value, "").trim
        if (v.nonEmpty) Some(Text(v)) else None
      }
    }
    sealed trait Author
    object Author {
      case class User(id: String, titleName: String) extends Author
      case class External(name: String)              extends Author
      case object Lishogi                            extends Author
      case object Unknown                            extends Author
    }
    def sanitize(text: String) =
      Text {
        text.trim
          .take(4000)
          .replaceAll("""\r\n""", "\n") // these 3 lines dedup white spaces and new lines
          .replaceAll("""(?m)(^ *| +(?= |$))""", "")
          .replaceAll("""(?m)^$([\n]+?)(^$[\n]+?^)+""", "$1")
      }
  }
  case class Comments(value: List[Comment]) extends AnyVal {
    def list                           = value
    def findBy(author: Comment.Author) = list.find(_.by == author)
    def set(comment: Comment) =
      Comments {
        if (list.exists(_.by == comment.by)) list.map {
          case c if c.by == comment.by => c.copy(text = comment.text)
          case c                       => c
        }
        else list :+ comment
      }
    def delete(commentId: Comment.Id) =
      Comments {
        value.filterNot(_.id == commentId)
      }
    def +(comment: Comment)    = Comments(comment :: value)
    def ++(comments: Comments) = Comments(value ::: comments.value)

    def filterEmpty = Comments(value.filter(_.text.value.nonEmpty))

    def hasLishogiComment = value.exists(_.by == Comment.Author.Lishogi)
    def authors = value.filterNot(_.by == Comment.Author.Lishogi).map(_.by)
  }
  object Comments {
    val empty = Comments(Nil)
  }

  case class Gamebook(deviation: Option[String], hint: Option[String]) {
    private def trimOrNone(txt: Option[String]) = txt.map(_.trim).filter(_.nonEmpty)
    def cleanUp =
      copy(
        deviation = trimOrNone(deviation),
        hint = trimOrNone(hint)
      )
    def nonEmpty = deviation.nonEmpty || hint.nonEmpty
  }

  implicit val openingWriter: OWrites[shogi.opening.FullOpening] = OWrites { o =>
    Json.obj(
      "japanese" -> o.japanese,
      "english"  -> o.english
    )
  }

  implicit private val posWrites: Writes[Pos] = Writes[Pos] { p =>
    JsString(p.usiKey)
  }
  implicit private val pieceWrites: Writes[ShogiPiece] = Writes[ShogiPiece] { p =>
    Json.obj(
      "role"  -> p.role.name,
      "color" -> p.color.name
    )
  }
  implicit private val shapeCircleWrites = Json.writes[Shape.Circle]
  implicit private val shapeArrowWrites  = Json.writes[Shape.Arrow]
  implicit private val shapePieceWrites  = Json.writes[Shape.Piece]
  implicit val shapeWrites: Writes[Shape] = Writes[Shape] {
    case s: Shape.Circle => shapeCircleWrites writes s
    case s: Shape.Arrow  => shapeArrowWrites writes s
    case s: Shape.Piece  => shapePieceWrites writes s
  }
  implicit val shapesWrites: Writes[Node.Shapes] = Writes[Node.Shapes] { s =>
    JsArray(s.list.map(shapeWrites.writes))
  }
  implicit val glyphWriter: Writes[Glyph] = Json.writes[Glyph]
  implicit val glyphsWriter: Writes[Glyphs] = Writes[Glyphs] { gs =>
    Json.toJson(gs.toList)
  }

  implicit val clockWrites: Writes[Centis] = Writes { clock =>
    JsNumber(clock.centis)
  }
  implicit val commentIdWrites: Writes[Comment.Id] = Writes { id =>
    JsString(id.value)
  }
  implicit val commentTextWrites: Writes[Comment.Text] = Writes { text =>
    JsString(text.value)
  }
  implicit val commentAuthorWrites: Writes[Comment.Author] = Writes[Comment.Author] {
    case Comment.Author.User(id, name) => Json.obj("id" -> id, "name" -> name)
    case Comment.Author.External(name) => JsString(s"${name.trim}")
    case Comment.Author.Lishogi        => JsString("lishogi")
    case Comment.Author.Unknown        => JsNull
  }
  implicit val commentWriter  = Json.writes[Node.Comment]
  implicit val gamebookWriter = Json.writes[Node.Gamebook]
  import Eval.JsonHandlers.evalWrites

  @inline implicit private def toPimpedJsObject(jo: JsObject) = new lila.base.PimpedJsObject(jo)

  implicit val defaultNodeJsonWriter: Writes[Node] =
    makeNodeJsonWriter(alwaysChildren = true)

  val minimalNodeJsonWriter: Writes[Node] =
    makeNodeJsonWriter(alwaysChildren = false)

  implicit def nodeListJsonWriter(alwaysChildren: Boolean): Writes[List[Node]] =
    Writes[List[Node]] { list =>
      val writer = if (alwaysChildren) defaultNodeJsonWriter else minimalNodeJsonWriter
      JsArray(list map writer.writes)
    }

  def makeNodeJsonWriter(alwaysChildren: Boolean): Writes[Node] =
    Writes { node =>
      import node._
      try {
        val comments = node.comments.list.flatMap(_.removeMeta)
        Json
          .obj(
            "ply"  -> ply,
            "sfen" -> sfen
          )
          .add("id", idOption.map(_.toString))
          .add("usi", usiOption.map(_.usi))
          .add("check", check)
          .add("eval", eval.filterNot(_.isEmpty))
          .add("comments", if (comments.nonEmpty) Some(comments) else None)
          .add("gamebook", gamebook)
          .add("glyphs", glyphs.nonEmpty)
          .add("shapes", if (shapes.list.nonEmpty) Some(shapes.list) else None)
          .add("opening", opening)
          .add("clock", clock)
          .add("comp", comp)
          .add(
            "children",
            if (alwaysChildren || children.nonEmpty) Some {
              nodeListJsonWriter(true) writes children
            }
            else None
          )
          .add("forceVariation", forceVariation)
      } catch {
        case e: StackOverflowError =>
          e.printStackTrace
          sys error s"### StackOverflowError ### in tree.makeNodeJsonWriter($alwaysChildren)"
      }
    }

  val partitionTreeJsonWriter: Writes[Node] = Writes { node =>
    JsArray {
      node.mainlineNodeList.map(minimalNodeJsonWriter.writes)
    }
  }
}
