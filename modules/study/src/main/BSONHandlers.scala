package lila.study

import chess.format.pgn.{ Glyph, Glyphs, Tag, Tags }
import chess.format.{ Uci, UciCharPair, FEN }
import chess.variant.{ Variant, Crazyhouse }
import chess.{ Centis, Pos, Role, PromotableRole }
import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl._
import lila.game.BSONHandlers.FENBSONHandler
import lila.tree.Eval
import lila.tree.Eval.Score
import lila.tree.Node.{ Shape, Shapes, Comment, Comments, Gamebook }

import lila.common.Iso
import lila.common.Iso._

object BSONHandlers {

  import Chapter._

  implicit val StudyIdBSONHandler = stringIsoHandler(Study.idIso)
  implicit val StudyNameBSONHandler = stringIsoHandler(Study.nameIso)
  implicit val ChapterIdBSONHandler = stringIsoHandler(Chapter.idIso)
  implicit val ChapterNameBSONHandler = stringIsoHandler(Chapter.nameIso)
  implicit val CentisBSONHandler = intIsoHandler(Iso.centisIso)

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
      case Shape.Circle(brush, pos) => $doc("b" -> brush, "p" -> pos.key)
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
      case _ => sys error s"Invalid UciCharPair ${bsonStr.value}"
    }
    def write(x: UciCharPair) = BSONString(x.toString)
  }

  import Study.IdName
  implicit val StudyIdNameBSONHandler = Macros.handler[IdName]

  import Uci.WithSan
  private implicit val UciWithSanBSONHandler = Macros.handler[WithSan]

  implicit val ShapesBSONHandler: BSONHandler[BSONArray, Shapes] =
    isoHandler[Shapes, List[Shape], BSONArray](
      (s: Shapes) => s.value,
      Shapes(_)
    )

  private implicit val CommentIdBSONHandler = stringAnyValHandler[Comment.Id](_.value, Comment.Id.apply)
  private implicit val CommentTextBSONHandler = stringAnyValHandler[Comment.Text](_.value, Comment.Text.apply)
  implicit val CommentAuthorBSONHandler = new BSONHandler[BSONValue, Comment.Author] {
    def read(bsonValue: BSONValue): Comment.Author = bsonValue match {
      case BSONString(lila.user.User.lichessId) => Comment.Author.Lichess
      case BSONString(name) => Comment.Author.External(name)
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
      case Comment.Author.Lichess => BSONString("l")
      case Comment.Author.Unknown => BSONString("")
    }
  }
  private implicit val CommentBSONHandler = Macros.handler[Comment]

  implicit val CommentsBSONHandler: BSONHandler[BSONArray, Comments] =
    isoHandler[Comments, List[Comment], BSONArray](
      (s: Comments) => s.value,
      Comments(_)
    )

  implicit val GamebookBSONHandler = Macros.handler[Gamebook]

  private implicit def CrazyDataBSONHandler: BSON[Crazyhouse.Data] = new BSON[Crazyhouse.Data] {
    private def writePocket(p: Crazyhouse.Pocket) = p.roles.map(_.forsyth).mkString
    private def readPocket(p: String) = Crazyhouse.Pocket(p.flatMap(chess.Role.forsyth)(scala.collection.breakOut))
    def reads(r: Reader) = Crazyhouse.Data(
      promoted = r.getsD[Pos]("o").toSet,
      pockets = Crazyhouse.Pockets(
        white = readPocket(r.strD("w")),
        black = readPocket(r.strD("b"))
      )
    )
    def writes(w: Writer, s: Crazyhouse.Data) = $doc(
      "o" -> w.listO(s.promoted.toList),
      "w" -> w.strO(writePocket(s.pockets.white)),
      "b" -> w.strO(writePocket(s.pockets.black))
    )
  }

  implicit val GlyphsBSONHandler = new BSONHandler[Barr, Glyphs] {
    private val idsHandler = bsonArrayToListHandler[Int]
    def read(b: Barr) = Glyphs.fromList(idsHandler read b flatMap Glyph.find)
    // must be BSONArray and not $arr!
    def write(x: Glyphs) = BSONArray(x.toList.map(_.id).map(BSONInteger.apply))
  }

  implicit val EvalScoreBSONHandler = new BSONHandler[BSONInteger, Score] {
    private val mateFactor = 1000000
    def read(i: BSONInteger) = Score {
      val v = i.value
      if (v >= mateFactor || v <= -mateFactor) Right(Eval.Mate(v / mateFactor))
      else Left(Eval.Cp(v))
    }
    def write(e: Score) = BSONInteger {
      e.value.fold(
        cp => cp.value atLeast (-mateFactor + 1) atMost (mateFactor - 1),
        mate => mate.value * mateFactor
      )
    }
  }

  implicit def NodeBSONHandler: BSON[Node] = new BSON[Node] {
    def reads(r: Reader) = Node(
      id = r.get[UciCharPair]("i"),
      ply = r int "p",
      move = WithSan(r.get[Uci]("u"), r.str("s")),
      fen = r.get[FEN]("f"),
      check = r boolD "c",
      shapes = r.getO[Shapes]("h") | Shapes.empty,
      comments = r.getO[Comments]("co") | Comments.empty,
      gamebook = r.getO[Gamebook]("ga"),
      glyphs = r.getO[Glyphs]("g") | Glyphs.empty,
      score = r.getO[Score]("e"),
      crazyData = r.getO[Crazyhouse.Data]("z"),
      clock = r.getO[Centis]("l"),
      children = r.get[Node.Children]("n"),
      forceVariation = r boolD "fv"
    )
    def writes(w: Writer, s: Node) = $doc(
      "i" -> s.id,
      "p" -> s.ply,
      "u" -> s.move.uci,
      "s" -> s.move.san,
      "f" -> s.fen,
      "c" -> w.boolO(s.check),
      "h" -> s.shapes.value.nonEmpty.option(s.shapes),
      "co" -> s.comments.value.nonEmpty.option(s.comments),
      "ga" -> s.gamebook,
      "g" -> s.glyphs.nonEmpty,
      "e" -> s.score,
      "l" -> s.clock,
      "z" -> s.crazyData,
      "n" -> (if (s.ply < Node.MAX_PLIES) s.children else Node.emptyChildren),
      "fv" -> w.boolO(s.forceVariation)
    )
  }
  import Node.Root
  private[study] implicit def NodeRootBSONHandler: BSON[Root] = new BSON[Root] {
    def reads(r: Reader) = Root(
      ply = r int "p",
      fen = r.get[FEN]("f"),
      check = r boolD "c",
      shapes = r.getO[Shapes]("h") | Shapes.empty,
      comments = r.getO[Comments]("co") | Comments.empty,
      gamebook = r.getO[Gamebook]("ga"),
      glyphs = r.getO[Glyphs]("g") | Glyphs.empty,
      score = r.getO[Score]("e"),
      clock = r.getO[Centis]("l"),
      crazyData = r.getO[Crazyhouse.Data]("z"),
      children = r.get[Node.Children]("n")
    )
    def writes(w: Writer, s: Root) = $doc(
      "p" -> s.ply,
      "f" -> s.fen,
      "c" -> w.boolO(s.check),
      "h" -> s.shapes.value.nonEmpty.option(s.shapes),
      "co" -> s.comments.value.nonEmpty.option(s.comments),
      "ga" -> s.gamebook,
      "g" -> s.glyphs.nonEmpty,
      "e" -> s.score,
      "l" -> s.clock,
      "z" -> s.crazyData,
      "n" -> s.children
    )
  }
  implicit val ChildrenBSONHandler = new BSONHandler[Barr, Node.Children] {
    private val nodesHandler = bsonArrayToVectorHandler[Node]
    def read(b: Barr) = try {
      Node.Children(nodesHandler read b)
    } catch {
      case e: StackOverflowError =>
        println(s"study handler ${e.toString}")
        Node.emptyChildren
    }
    def write(x: Node.Children) = try {
      nodesHandler write x.nodes
    } catch {
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

  implicit val PgnTagBSONHandler = new BSONHandler[BSONString, Tag] {
    def read(b: BSONString): Tag = b.value.split(":", 2) match {
      case Array(name, value) => Tag(name, value)
      case _ => sys error s"Invalid pgn tag ${b.value}"
    }
    def write(t: Tag) = BSONString(s"${t.name}:${t.value}")
  }
  implicit val PgnTagsBSONHandler: BSONHandler[BSONArray, Tags] =
    isoHandler[Tags, List[Tag], BSONArray](
      (s: Tags) => s.value,
      Tags(_)
    )
  private implicit val ChapterSetupBSONHandler = Macros.handler[Chapter.Setup]
  implicit val ChapterRelayBSONHandler = Macros.handler[Chapter.Relay]
  implicit val ChapterServerEvalBSONHandler = Macros.handler[Chapter.ServerEval]
  import Chapter.Ply
  implicit val PlyBSONHandler = intAnyValHandler[Ply](_.value, Ply.apply)
  implicit val ChapterBSONHandler = Macros.handler[Chapter]
  implicit val ChapterMetadataBSONHandler = Macros.handler[Chapter.Metadata]

  private implicit val ChaptersMap = BSON.MapDocument.MapHandler[Chapter.Id, Chapter]

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
    private val mapHandler = BSON.MapDocument.MapHandler[String, DbMember]
    def read(b: Bdoc) = StudyMembers(mapHandler read b map {
      case (id, dbMember) => id -> StudyMember(id, dbMember.role, dbMember.addedAt)
    })
    def write(x: StudyMembers) = BSONDocument(x.members.mapValues(StudyMemberBSONWriter.write))
  }
  import Study.Visibility
  private[study] implicit val VisibilityHandler: BSONHandler[BSONString, Visibility] = new BSONHandler[BSONString, Visibility] {
    def read(bs: BSONString) = Visibility.byKey get bs.value err s"Invalid visibility ${bs.value}"
    def write(x: Visibility) = BSONString(x.key)
  }
  import Study.From
  private[study] implicit val FromHandler: BSONHandler[BSONString, From] = new BSONHandler[BSONString, From] {
    def read(bs: BSONString) = bs.value.split(' ') match {
      case Array("scratch") => From.Scratch
      case Array("game", id) => From.Game(id)
      case Array("study", id) => From.Study(Study.Id(id))
      case Array("relay") => From.Relay(none)
      case Array("relay", id) => From.Relay(Study.Id(id).some)
      case _ => sys error s"Invalid from ${bs.value}"
    }
    def write(x: From) = BSONString(x match {
      case From.Scratch => "scratch"
      case From.Game(id) => s"game $id"
      case From.Study(id) => s"study $id"
      case From.Relay(id) => s"relay${id.fold("")(" " + _)}"
    })
  }
  import Settings.UserSelection
  private[study] implicit val UserSelectionHandler: BSONHandler[BSONString, UserSelection] = new BSONHandler[BSONString, UserSelection] {
    def read(bs: BSONString) = UserSelection.byKey get bs.value err s"Invalid user selection ${bs.value}"
    def write(x: UserSelection) = BSONString(x.key)
  }
  implicit val SettingsBSONHandler = new BSON[Settings] {
    def reads(r: Reader) = Settings(
      computer = r.get[UserSelection]("computer"),
      explorer = r.get[UserSelection]("explorer"),
      cloneable = r.getO[UserSelection]("cloneable") | Settings.init.cloneable,
      chat = r.getO[UserSelection]("chat") | Settings.init.chat,
      sticky = r.getO[Boolean]("sticky") | Settings.init.sticky,
      description = r.getO[Boolean]("description") | Settings.init.description
    )
    private val writer = Macros.writer[Settings]
    def writes(w: Writer, s: Settings) = writer write s
  }

  import Study.Likes
  implicit val LikesBSONHandler = intAnyValHandler[Likes](_.value, Likes.apply)
  import Study.Rank
  private[study] implicit val RankBSONHandler = dateIsoHandler[Rank](Iso[DateTime, Rank](Rank.apply, _.value))

  // implicit val StudyBSONHandler = BSON.LoggingHandler(logger)(Macros.handler[Study])
  implicit val StudyBSONHandler = Macros.handler[Study]

  implicit val lightStudyBSONReader = new BSONDocumentReader[Study.LightStudy] {
    def read(doc: BSONDocument) = Study.LightStudy(
      isPublic = doc.getAs[String]("visibility") has "public",
      contributors = doc.getAs[StudyMembers]("members").??(_.contributorIds)
    )
  }
}
