package lila.study

import chess.format.{ Uci, UciCharPair, FEN }
import chess.{ Pos, Color, Role, PromotableRole }
import chess.variant.Variant
import reactivemongo.bson._

import lila.db.BSON
import lila.db.BSON._
import lila.db.BSON.BSONJodaDateTimeHandler

private object BSONHandlers {

  import Chapter._

  private implicit val PosBSONHandler = new BSONHandler[BSONString, Pos] {
    def read(bsonStr: BSONString): Pos = Pos.posAt(bsonStr.value) err s"No such pos: ${bsonStr.value}"
    def write(x: Pos) = BSONString(x.key)
  }
  implicit val ColorBSONHandler = new BSONHandler[BSONBoolean, chess.Color] {
    def read(b: BSONBoolean) = chess.Color(b.value)
    def write(c: chess.Color) = BSONBoolean(c.white)
  }

  implicit val ShapeBSONHandler = new BSON[Shape] {
    def reads(r: Reader) = {
      val brush = r str "b"
      r.getO[Pos]("p") map { pos =>
        Shape.Circle(brush, pos)
      } getOrElse Shape.Arrow(brush, r.get[Pos]("o"), r.get[Pos]("d"))
    }
    def writes(w: Writer, t: Shape) = t match {
      case Shape.Circle(brush, pos)       => BSONDocument("b" -> brush, "p" -> pos.key)
      case Shape.Arrow(brush, orig, dest) => BSONDocument("b" -> brush, "o" -> orig.key, "d" -> dest.key)
    }
  }

  implicit val PromotableRoleHandler = new BSONHandler[BSONString, PromotableRole] {
    def read(bsonStr: BSONString): PromotableRole = bsonStr.value.headOption flatMap Role.allPromotableByForsyth.get err s"No such role: ${bsonStr.value}"
    def write(x: PromotableRole) = BSONString(x.forsyth.toString)
  }

  implicit val RoleHandler = new BSONHandler[BSONString, Role] {
    def read(bsonStr: BSONString): Role = bsonStr.value.headOption flatMap Role.allByForsyth.get err s"No such role: ${bsonStr.value}"
    def write(x: Role) = BSONString(x.forsyth.toString)
  }

  implicit val UciBSONHandler = new BSON[Uci] {
    def reads(r: Reader) = {
      r.getO[Pos]("o") map { orig =>
        Uci.Move(orig, r.get[Pos]("d"), r.getO[PromotableRole]("p"))
      } getOrElse Uci.Drop(r.get[Role]("r"), r.get[Pos]("p"))
    }
    def writes(w: Writer, u: Uci) = u match {
      case Uci.Move(orig, dest, prom) => BSONDocument("o" -> orig, "d" -> dest, "p" -> prom)
      case Uci.Drop(role, pos)        => BSONDocument("r" -> role, "p" -> pos)
    }
  }

  implicit val UciCharPairHandler = new BSONHandler[BSONString, UciCharPair] {
    def read(bsonStr: BSONString): UciCharPair = bsonStr.value.toArray match {
      case Array(a, b) => UciCharPair(a, b)
      case _           => sys error s"Invalid UciCharPair ${bsonStr.value}"
    }
    def write(x: UciCharPair) = BSONString(x.toString)
  }

  import Node.Move
  private implicit val MoveBSONHandler = Macros.handler[Move]

  private implicit def NodeBSONHandler: BSON[Node] = new BSON[Node] {
    def reads(r: Reader) = Node(
      id = r.get[UciCharPair]("i"),
      ply = r int "p",
      move = r.get[Move]("m"),
      fen = r str "f",
      check = r boolD "c",
      children = r.getsD[Node]("n"))
    def writes(w: Writer, s: Node) = BSONDocument(
      "i" -> s.id,
      "p" -> s.ply,
      "m" -> s.move,
      "f" -> s.fen,
      "c" -> w.boolO(s.check),
      "n" -> s.children)
  }
  import Node.Root
  private implicit def NodeRootBSONHandler: BSON[Root] = new BSON[Root] {
    def reads(r: Reader) = Root(
      ply = r int "p",
      fen = r str "f",
      check = r boolD "c",
      children = r.getsD[Node]("n"))
    def writes(w: Writer, s: Root) = BSONDocument(
      "p" -> s.ply,
      "f" -> s.fen,
      "c" -> w.boolO(s.check),
      "n" -> s.children)
  }

  implicit val PathBSONHandler = stringAnyValHandler[Path](_.value, Path.apply)
  implicit val FenBSONHandler = stringAnyValHandler[FEN](_.value, FEN.apply)
  implicit val VariantBSONHandler = new BSONHandler[BSONInteger, Variant] {
    def read(b: BSONInteger): Variant = Variant(b.value) err s"No such variant: ${b.value}"
    def write(x: Variant) = BSONInteger(x.id)
  }

  private implicit val ChapterSetupBSONHandler = Macros.handler[Chapter.Setup]
  private implicit val ChapterBSONHandler = Macros.handler[Chapter]

  private implicit val ChaptersMap = MapDocument.MapHandler[Chapter]

  implicit val StudyBSONHandler = Macros.handler[Study]
}
