package lila.socket
package tree

import chess.format.pgn.{ Glyph, Glyphs }
import chess.format.{ Uci, UciCharPair }
import chess.opening.FullOpening
import chess.Pos
import chess.variant.Crazyhouse

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Reads._

sealed trait Node {
  def ply: Int
  def fen: String
  def check: Boolean
  // None when not computed yet
  def dests: Option[Map[Pos, List[Pos]]]
  def drops: Option[List[Pos]]
  def eval: Option[Node.Eval]
  def shapes: Node.Shapes
  def comments: Node.Comments
  def glyphs: Glyphs
  def children: List[Branch]
  def opening: Option[FullOpening]
  def crazyData: Option[Crazyhouse.Data]
  def addChild(branch: Branch): Node
  def dropFirstChild: Node

  // implementation dependant
  def idOption: Option[UciCharPair]
  def moveOption: Option[Uci.WithSan]

  // who's color plays next
  def color = chess.Color(ply % 2 == 0)

  def mainlineNodeList: List[Node] =
    dropFirstChild :: children.headOption.??(_.mainlineNodeList)
}

case class Root(
    ply: Int,
    fen: String,
    check: Boolean,
    // None when not computed yet
    dests: Option[Map[Pos, List[Pos]]] = None,
    drops: Option[List[Pos]] = None,
    eval: Option[Node.Eval] = None,
    shapes: Node.Shapes = Node.Shapes(Nil),
    comments: Node.Comments = Node.Comments(Nil),
    glyphs: Glyphs = Glyphs.empty,
    children: List[Branch] = Nil,
    opening: Option[FullOpening] = None,
    crazyData: Option[Crazyhouse.Data]) extends Node {

  def idOption = None
  def moveOption = None

  def addChild(branch: Branch) = copy(children = children :+ branch)
  def prependChild(branch: Branch) = copy(children = branch :: children)
  def dropFirstChild = copy(children = if (children.isEmpty) children else children.tail)
}

case class Branch(
    id: UciCharPair,
    ply: Int,
    move: Uci.WithSan,
    fen: String,
    check: Boolean,
    // None when not computed yet
    dests: Option[Map[Pos, List[Pos]]] = None,
    drops: Option[List[Pos]] = None,
    eval: Option[Node.Eval] = None,
    shapes: Node.Shapes = Node.Shapes(Nil),
    comments: Node.Comments = Node.Comments(Nil),
    glyphs: Glyphs = Glyphs.empty,
    children: List[Branch] = Nil,
    opening: Option[FullOpening] = None,
    crazyData: Option[Crazyhouse.Data]) extends Node {

  def idOption = Some(id)
  def moveOption = Some(move)

  def addChild(branch: Branch) = copy(children = children :+ branch)
  def prependChild(branch: Branch) = copy(children = branch :: children)
  def dropFirstChild = copy(children = if (children.isEmpty) children else children.tail)
}

object Node {

  sealed trait Shape
  object Shape {
    type ID = String
    type Brush = String
    case class Circle(brush: Brush, orig: Pos) extends Shape
    case class Arrow(brush: Brush, orig: Pos, dest: Pos) extends Shape
  }
  case class Shapes(value: List[Shape]) extends AnyVal {
    def list = value
    def ++(shapes: Shapes) = Shapes(value ::: shapes.value)
  }

  case class Comment(id: Comment.Id, text: Comment.Text, by: Comment.Author)
  object Comment {
    case class Id(value: String) extends AnyVal
    object Id {
      def make = Id(scala.util.Random.alphanumeric take 4 mkString)
    }
    case class Text(value: String) extends AnyVal
    sealed trait Author
    object Author {
      case class User(id: String, titleName: String) extends Author
      case class External(name: String) extends Author
      case object Lichess extends Author
      case object Unknown extends Author
    }
    def sanitize(text: String) = Text {
      text.trim.take(4000)
        .replaceAll("""\r\n""", "\n") // these 3 lines dedup white spaces and new lines
        .replaceAll("""(?m)(^ *| +(?= |$))""", "")
        .replaceAll("""(?m)^$([\n]+?)(^$[\n]+?^)+""", "$1")
        .replaceAll("\\{|\\}", "") // {} are reserved in PGN comments
    }
  }
  case class Comments(value: List[Comment]) extends AnyVal {
    def list = value
    def findBy(author: Comment.Author) = list.find(_.by == author)
    def set(comment: Comment) = Comments {
      if (list.exists(_.by == comment.by)) list.map {
        case c if c.by == comment.by => c.copy(text = comment.text)
        case c                       => c
      }
      else list :+ comment
    }
    def delete(commentId: Comment.Id) = Comments {
      value.filterNot(_.id == commentId)
    }
    def +(comment: Comment) = Comments(comment :: value)
  }

  case class Eval(
    cp: Option[Int] = None,
    mate: Option[Int] = None,
    best: Option[Uci.Move])

  private implicit val uciJsonWriter: Writes[Uci.Move] = Writes { uci =>
    JsString(uci.uci)
  }
  private implicit val evalJsonWriter = Json.writes[Eval]

  // TODO copied from lila.game
  // put all that shit somewhere else
  private implicit val crazyhousePocketWriter: OWrites[Crazyhouse.Pocket] = OWrites { v =>
    JsObject(
      Crazyhouse.storableRoles.flatMap { role =>
        Some(v.roles.count(role ==)).filter(0 <).map { count =>
          role.name -> JsNumber(count)
        }
      })
  }
  private implicit val crazyhouseDataWriter: OWrites[chess.variant.Crazyhouse.Data] = OWrites { v =>
    Json.obj("pockets" -> List(v.pockets.white, v.pockets.black))
  }

  implicit val openingWriter: OWrites[chess.opening.FullOpening] = OWrites { o =>
    Json.obj(
      "eco" -> o.eco,
      "name" -> o.name)
  }

  private implicit val posWrites: Writes[Pos] = Writes[Pos] { p =>
    JsString(p.key)
  }
  private implicit val shapeCircleWrites = Json.writes[Shape.Circle]
  private implicit val shapeArrowWrites = Json.writes[Shape.Arrow]
  implicit val shapeWrites: Writes[Shape] = Writes[Shape] {
    case s: Shape.Circle => shapeCircleWrites writes s
    case s: Shape.Arrow  => shapeArrowWrites writes s
  }
  implicit val shapesWrites: Writes[Node.Shapes] = Writes[Node.Shapes] { s =>
    JsArray(s.list.map(shapeWrites.writes))
  }
  implicit val glyphWriter: Writes[Glyph] = Json.writes[Glyph]
  implicit val glyphsWriter: Writes[Glyphs] = Writes[Glyphs] { gs =>
    Json.toJson(gs.toList)
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
    case Comment.Author.Lichess        => JsString("lichess")
    case Comment.Author.Unknown        => JsNull
  }
  implicit val commentWriter = Json.writes[Node.Comment]
  private implicit val commentsWriter: Writes[Node.Comments] = Writes[Node.Comments] { s =>
    JsArray(s.list.map(commentWriter.writes))
  }

  def makeNodeJsonWriter(alwaysChildren: Boolean): Writes[Node] = Writes { node =>
    import node._
    (
      add("id", idOption.map(_.toString)) _ compose
      add("uci", moveOption.map(_.uci.uci)) _ compose
      add("san", moveOption.map(_.san)) _ compose
      add("check", true, check) _ compose
      add("eval", eval) _ compose
      add("comments", comments.list, comments.list.nonEmpty) _ compose
      add("glyphs", glyphs.nonEmpty) _ compose
      add("shapes", shapes.list, shapes.list.nonEmpty) _ compose
      add("opening", opening) _ compose
      add("dests", dests.map {
        _.map {
          case (orig, dests) => s"${orig.piotr}${dests.map(_.piotr).mkString}"
        }.mkString(" ")
      }) _ compose
      add("drops", drops.map { drops =>
        JsString(drops.map(_.key).mkString)
      }) _ compose
      add("crazy", crazyData) _ compose
      add("children", children, alwaysChildren || children.nonEmpty)
    )(Json.obj(
        "ply" -> ply,
        "fen" -> fen))
  }

  implicit val defaultNodeJsonWriter: Writes[Node] =
    makeNodeJsonWriter(alwaysChildren = true)

  val minimalNodeJsonWriter: Writes[Node] =
    makeNodeJsonWriter(alwaysChildren = false)

  val partitionTreeJsonWriter: Writes[Node] = Writes { node =>
    JsArray {
      node.mainlineNodeList.map(minimalNodeJsonWriter.writes)
    }
  }

  private def add[A](k: String, v: A, cond: Boolean)(o: JsObject)(implicit writes: Writes[A]): JsObject =
    if (cond) o + (k -> writes.writes(v)) else o

  private def add[A: Writes](k: String, v: Option[A]): JsObject => JsObject =
    v.fold(identity[JsObject] _) { add(k, _, true) _ }
}
