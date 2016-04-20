package lila.study

import chess.format.{ Uci, UciCharPair, FEN }
import chess.{ Pos, Color, Role, PromotableRole }
import chess.variant.Variant
import reactivemongo.bson._

import lila.db.BSON
import lila.db.BSON._
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.common.LightUser

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

  implicit val UciHandler = new BSONHandler[BSONString, Uci] {
    def read(bs: BSONString): Uci = Uci(bs.value) err s"Bad UCI: ${bs.value}"
    def write(x: Uci) = BSONString(x.uci)
  }

  implicit val UciCharPairHandler = new BSONHandler[BSONString, UciCharPair] {
    def read(bsonStr: BSONString): UciCharPair = bsonStr.value.toArray match {
      case Array(a, b) => UciCharPair(a, b)
      case _           => sys error s"Invalid UciCharPair ${bsonStr.value}"
    }
    def write(x: UciCharPair) = BSONString(x.toString)
  }

  import Uci.WithSan
  private implicit val UciWithSanBSONHandler = Macros.handler[WithSan]

  private implicit val FenBSONHandler = stringAnyValHandler[FEN](_.value, FEN.apply)

  private implicit def NodeBSONHandler: BSON[Node] = new BSON[Node] {
    def reads(r: Reader) = Node(
      id = r.get[UciCharPair]("i"),
      ply = r int "p",
      move = WithSan(r.get[Uci]("u"), r.str("s")),
      fen = r.get[FEN]("f"),
      check = r boolD "c",
      by = r str "b",
      children = r.get[Node.Children]("n"))
    def writes(w: Writer, s: Node) = BSONDocument(
      "i" -> s.id,
      "p" -> s.ply,
      "u" -> s.move.uci,
      "s" -> s.move.san,
      "f" -> s.fen,
      "c" -> w.boolO(s.check),
      "b" -> s.by,
      "n" -> s.children)
  }
  import Node.Root
  private implicit def NodeRootBSONHandler: BSON[Root] = new BSON[Root] {
    def reads(r: Reader) = Root(
      ply = r int "p",
      fen = r.get[FEN]("f"),
      check = r boolD "c",
      children = r.get[Node.Children]("n"))
    def writes(w: Writer, s: Root) = BSONDocument(
      "p" -> s.ply,
      "f" -> s.fen,
      "c" -> w.boolO(s.check),
      "n" -> s.children)
  }
  implicit val ChildrenBSONHandler = new BSONHandler[BSONArray, Node.Children] {
    private val nodesHandler = bsonArrayToVectorHandler[Node]
    def read(b: BSONArray) = Node.Children(nodesHandler read b)
    def write(x: Node.Children) = nodesHandler write x.nodes
  }

  implicit val PathBSONHandler = new BSONHandler[BSONString, Path] {
    def read(b: BSONString): Path = Path(b.value)
    def write(x: Path) = BSONString(x.toString)
  }
  implicit val VariantBSONHandler = new BSONHandler[BSONInteger, Variant] {
    def read(b: BSONInteger): Variant = Variant(b.value) err s"No such variant: ${b.value}"
    def write(x: Variant) = BSONInteger(x.id)
  }

  private implicit val ChapterSetupBSONHandler = Macros.handler[Chapter.Setup]
  implicit val ChapterBSONHandler = Macros.handler[Chapter]

  private implicit val ChaptersMap = MapDocument.MapHandler[Chapter]

  implicit val PositionRefBSONHandler = new BSONHandler[BSONString, Position.Ref] {
    def read(b: BSONString) = Position.Ref.decode(b.value) err s"Invalid position ${b.value}"
    def write(x: Position.Ref) = BSONString(x.encode)
  }
  implicit val StudyMemberRoleBSONHandler = new BSONHandler[BSONString, StudyMember.Role] {
    def read(b: BSONString) = StudyMember.Role.byId get b.value err s"Invalid role ${b.value}"
    def write(x: StudyMember.Role) = BSONString(x.id)
  }
  private implicit val LightUserBSONHandler = Macros.handler[LightUser]
  private implicit val MemberBSONHandler = Macros.handler[StudyMember]
  private[study] implicit val MemberMapBSONHandler = MapDocument.MapHandler[StudyMember]

  implicit val StudyBSONHandler = Macros.handler[Study]
}
