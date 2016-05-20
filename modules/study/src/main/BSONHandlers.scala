package lila.study

import chess.format.pgn.{ Glyph, Glyphs, Tag }
import chess.format.{ Uci, UciCharPair, FEN }
import chess.variant.{ Variant, Crazyhouse }
import chess.{ Pos, Color, Role, PromotableRole }
import org.joda.time.DateTime
import reactivemongo.bson._

import lila.common.LightUser
import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl._
import lila.socket.tree.Node.Shape

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
      case Shape.Circle(brush, pos)       => $doc("b" -> brush, "p" -> pos.key)
      case Shape.Arrow(brush, orig, dest) => $doc("b" -> brush, "o" -> orig.key, "d" -> dest.key)
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

  import lila.socket.tree.Node.{ Comment, Comments }
  private implicit val CommentIdBSONHandler = stringAnyValHandler[Comment.Id](_.value, Comment.Id.apply)
  private implicit val CommentTextBSONHandler = stringAnyValHandler[Comment.Text](_.value, Comment.Text.apply)
  implicit val CommentAuthorBSONHandler = new BSONHandler[BSONValue, Comment.Author] {
    def read(bsonValue: BSONValue): Comment.Author = bsonValue match {
      case BSONString("lichess") => Comment.Author.Lichess
      case BSONString(name)      => Comment.Author.External(name)
      case doc: Bdoc => {
        for {
          id <- doc.getAs[String]("id")
          name <- doc.getAs[String]("name")
        } yield Comment.Author.User(id, name)
      } err s"Invalid comment author $doc"
      case _ => Comment.Author.Unknown
    }
    def write(x: Comment.Author): BSONValue = x match {
      case Comment.Author.User(id, name) => $doc("id" -> id, "name" -> name)
      case Comment.Author.External(name) => BSONString(s"${name.trim}")
      case Comment.Author.Lichess        => BSONString("l")
      case Comment.Author.Unknown        => BSONString("")
    }
  }
  private implicit val CommentBSONHandler = Macros.handler[Comment]

  private def readComments(r: Reader) =
    Comments(r.getsD[Comment]("co").filter(_.text.value.nonEmpty))

  private implicit def CrazyDataBSONHandler: BSON[Crazyhouse.Data] = new BSON[Crazyhouse.Data] {
    private def writePocket(p: Crazyhouse.Pocket) = p.roles.map(_.forsyth).mkString
    private def readPocket(p: String) = Crazyhouse.Pocket(p.toList.flatMap(chess.Role.forsyth))
    def reads(r: Reader) = Crazyhouse.Data(
      promoted = r.getsD[Pos]("o").toSet,
      pockets = Crazyhouse.Pockets(
        white = readPocket(r.strD("w")),
        black = readPocket(r.strD("b"))))
    def writes(w: Writer, s: Crazyhouse.Data) = $doc(
      "o" -> w.listO(s.promoted.toList),
      "w" -> w.strO(writePocket(s.pockets.white)),
      "b" -> w.strO(writePocket(s.pockets.black)))
  }

  private implicit val GlyphsBSONHandler = new BSONHandler[BSONArray, Glyphs] {
    private val idsHandler = bsonArrayToListHandler[Int]
    def read(b: BSONArray) = Glyphs.fromList(idsHandler read b flatMap Glyph.find)
    def write(x: Glyphs) = BSONArray(x.toList.map(_.id).map(BSONInteger.apply))
  }

  private implicit def NodeBSONHandler: BSON[Node] = new BSON[Node] {
    def reads(r: Reader) = Node(
      id = r.get[UciCharPair]("i"),
      ply = r int "p",
      move = WithSan(r.get[Uci]("u"), r.str("s")),
      fen = r.get[FEN]("f"),
      check = r boolD "c",
      shapes = r.getsD[Shape]("h"),
      comments = readComments(r),
      glyphs = r.getO[Glyphs]("g") | Glyphs.empty,
      crazyData = r.getO[Crazyhouse.Data]("z"),
      children = r.get[Node.Children]("n"))
    def writes(w: Writer, s: Node) = $doc(
      "i" -> s.id,
      "p" -> s.ply,
      "u" -> s.move.uci,
      "s" -> s.move.san,
      "f" -> s.fen,
      "c" -> w.boolO(s.check),
      "h" -> w.listO(s.shapes),
      "co" -> w.listO(s.comments.list),
      "g" -> s.glyphs.nonEmpty,
      "z" -> s.crazyData,
      "n" -> (if (s.ply < Node.MAX_PLIES) s.children else Node.emptyChildren))
  }
  import Node.Root
  private implicit def NodeRootBSONHandler: BSON[Root] = new BSON[Root] {
    def reads(r: Reader) = Root(
      ply = r int "p",
      fen = r.get[FEN]("f"),
      check = r boolD "c",
      shapes = r.getsD[Shape]("h"),
      comments = readComments(r),
      glyphs = r.getO[Glyphs]("g") | Glyphs.empty,
      crazyData = r.getO[Crazyhouse.Data]("z"),
      children = r.get[Node.Children]("n"))
    def writes(w: Writer, s: Root) = $doc(
      "p" -> s.ply,
      "f" -> s.fen,
      "c" -> w.boolO(s.check),
      "h" -> w.listO(s.shapes),
      "co" -> w.listO(s.comments.list),
      "g" -> s.glyphs.nonEmpty,
      "z" -> s.crazyData,
      "n" -> s.children)
  }
  implicit val ChildrenBSONHandler = new BSONHandler[BSONArray, Node.Children] {
    private val nodesHandler = bsonArrayToVectorHandler[Node]
    def read(b: BSONArray) = try {
      Node.Children(nodesHandler read b)
    }
    catch {
      case e: StackOverflowError =>
        println(s"study handler ${e.toString}")
        Node.emptyChildren
    }
    def write(x: Node.Children) = try {
      nodesHandler write x.nodes
    }
    catch {
      case e: StackOverflowError =>
        println(s"study handler ${e.toString}")
        $arr()
    }
  }

  implicit val PathBSONHandler = new BSONHandler[BSONString, Path] {
    def read(b: BSONString): Path = Path(b.value)
    def write(x: Path) = BSONString(x.toString)
  }
  implicit val VariantBSONHandler = new BSONHandler[BSONInteger, Variant] {
    def read(b: BSONInteger): Variant = Variant(b.value) err s"No such variant: ${b.value}"
    def write(x: Variant) = BSONInteger(x.id)
  }
  private implicit val StudyViewsBSONHandler = intAnyValHandler[Study.Views](_.value, Study.Views.apply)

  private implicit val PgnTagBSONHandler = new BSONHandler[BSONString, Tag] {
    def read(b: BSONString): Tag = b.value.split(":", 2) match {
      case Array(name, value) => Tag(name, value)
      case _                  => sys error s"Invalid pgn tag ${b.value}"
    }
    def write(t: Tag) = BSONString(s"${t.name}:${t.value}")
  }
  private implicit val ChapterFromPgnBSONHandler = Macros.handler[Chapter.FromPgn]
  private implicit val ChapterSetupBSONHandler = Macros.handler[Chapter.Setup]
  import Chapter.Ply
  private implicit val PlyBSONHandler = intAnyValHandler[Ply](_.value, Ply.apply)
  implicit val ChapterBSONHandler = Macros.handler[Chapter]
  implicit val ChapterMetadataBSONHandler = Macros.handler[Chapter.Metadata]

  private implicit val ChaptersMap = BSON.MapDocument.MapHandler[Chapter]

  implicit val PositionRefBSONHandler = new BSONHandler[BSONString, Position.Ref] {
    def read(b: BSONString) = Position.Ref.decode(b.value) err s"Invalid position ${b.value}"
    def write(x: Position.Ref) = BSONString(x.encode)
  }
  implicit val StudyMemberRoleBSONHandler = new BSONHandler[BSONString, StudyMember.Role] {
    def read(b: BSONString) = StudyMember.Role.byId get b.value err s"Invalid role ${b.value}"
    def write(x: StudyMember.Role) = BSONString(x.id)
  }
  private case class DbMember(role: StudyMember.Role, addedAt: DateTime)
  private implicit val DbMemberBSONHandler = Macros.handler[DbMember]
  private[study] implicit val StudyMemberBSONWriter = new BSONWriter[StudyMember, Bdoc] {
    def write(x: StudyMember) = DbMemberBSONHandler write DbMember(x.role, x.addedAt)
  }
  private[study] implicit val MembersBSONHandler = new BSONHandler[Bdoc, StudyMembers] {
    private val mapHandler = BSON.MapDocument.MapHandler[DbMember]
    def read(b: Bdoc) = StudyMembers(mapHandler read b map {
      case (id, dbMember) => id -> StudyMember(id, dbMember.role, dbMember.addedAt)
    })
    def write(x: StudyMembers) = $doc(x.members.mapValues(StudyMemberBSONWriter.write))
  }
  import Study.Visibility
  private[study] implicit val VisibilityHandler: BSONHandler[BSONString, Visibility] = new BSONHandler[BSONString, Visibility] {
    def read(bs: BSONString) = Visibility.byKey get bs.value err s"Invalid visibility ${bs.value}"
    def write(x: Visibility) = BSONString(x.key)
  }
  import Settings.UserSelection
  private[study] implicit val UserSelectionHandler: BSONHandler[BSONString, UserSelection] = new BSONHandler[BSONString, UserSelection] {
    def read(bs: BSONString) = UserSelection.byKey get bs.value err s"Invalid user selection ${bs.value}"
    def write(x: UserSelection) = BSONString(x.key)
  }
  implicit val SettingsBSONHandler = Macros.handler[Settings]

  implicit val StudyBSONHandler = Macros.handler[Study]
}
