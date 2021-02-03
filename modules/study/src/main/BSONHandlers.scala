package lila.study

import chess.format.pgn.{ Glyph, Glyphs, Tag, Tags }
import chess.format.{ FEN, Uci, UciCharPair }
import chess.variant.{ Crazyhouse, Variant }
import chess.{ Centis, Pos, PromotableRole, Role }
import org.joda.time.DateTime
import reactivemongo.api.bson._
import scala.util.Success
import scala.util.Try

import lila.common.Iso
import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl._
import lila.tree.Eval
import lila.tree.Eval.Score
import lila.tree.Node.{ Comment, Comments, Gamebook, Shape, Shapes }

object BSONHandlers {

  import Chapter._

  implicit val StudyIdBSONHandler     = stringIsoHandler(Study.idIso)
  implicit val StudyNameBSONHandler   = stringIsoHandler(Study.nameIso)
  implicit val ChapterIdBSONHandler   = stringIsoHandler(Chapter.idIso)
  implicit val ChapterNameBSONHandler = stringIsoHandler(Chapter.nameIso)
  implicit val CentisBSONHandler      = intIsoHandler(Iso.centisIso)
  implicit val StudyTopicBSONHandler  = stringIsoHandler(StudyTopic.topicIso)
  implicit val StudyTopicsBSONHandler =
    implicitly[BSONHandler[List[StudyTopic]]].as[StudyTopics](StudyTopics.apply, _.value)

  implicit private val PosBSONHandler = tryHandler[Pos](
    { case BSONString(v) => Pos.fromKey(v) toTry s"No such pos: $v" },
    x => BSONString(x.key)
  )

  implicit val ShapeBSONHandler = new BSON[Shape] {
    def reads(r: Reader) = {
      val brush = r str "b"
      r.getO[Pos]("p") map { pos =>
        Shape.Circle(brush, pos)
      } getOrElse Shape.Arrow(brush, r.get[Pos]("o"), r.get[Pos]("d"))
    }
    def writes(w: Writer, t: Shape) =
      t match {
        case Shape.Circle(brush, pos)       => $doc("b" -> brush, "p" -> pos.key)
        case Shape.Arrow(brush, orig, dest) => $doc("b" -> brush, "o" -> orig.key, "d" -> dest.key)
      }
  }

  implicit val PromotableRoleHandler = tryHandler[PromotableRole](
    { case BSONString(v) => v.headOption flatMap Role.allPromotableByForsyth.get toTry s"No such role: $v" },
    x => BSONString(x.forsyth.toString)
  )

  implicit val RoleHandler = tryHandler[Role](
    { case BSONString(v) => v.headOption flatMap Role.allByForsyth.get toTry s"No such role: $v" },
    x => BSONString(x.forsyth.toString)
  )

  implicit val UciHandler = tryHandler[Uci](
    { case BSONString(v) => Uci(v) toTry s"Bad UCI: $v" },
    x => BSONString(x.uci)
  )

  implicit val UciCharPairHandler = tryHandler[UciCharPair](
    { case BSONString(v) =>
      v.toArray match {
        case Array(a, b) => Success(UciCharPair(a, b))
        case _           => handlerBadValue(s"Invalid UciCharPair $v")
      }
    },
    x => BSONString(x.toString)
  )

  import Study.IdName
  implicit val StudyIdNameBSONHandler = Macros.handler[IdName]

  import Uci.WithSan

  implicit val ShapesBSONHandler: BSONHandler[Shapes] =
    isoHandler[Shapes, List[Shape]]((s: Shapes) => s.value, Shapes(_))

  implicit private val CommentIdBSONHandler   = stringAnyValHandler[Comment.Id](_.value, Comment.Id.apply)
  implicit private val CommentTextBSONHandler = stringAnyValHandler[Comment.Text](_.value, Comment.Text.apply)
  implicit val CommentAuthorBSONHandler = quickHandler[Comment.Author](
    {
      case BSONString(lila.user.User.lichessId | "l") => Comment.Author.Lichess
      case BSONString(name)                           => Comment.Author.External(name)
      case doc: Bdoc =>
        {
          for {
            id   <- doc.getAsOpt[String]("id")
            name <- doc.getAsOpt[String]("name")
          } yield Comment.Author.User(id, name)
        } err s"Invalid comment author $doc"
      case _ => Comment.Author.Unknown
    },
    {
      case Comment.Author.User(id, name) => $doc("id" -> id, "name" -> name)
      case Comment.Author.External(name) => BSONString(s"${name.trim}")
      case Comment.Author.Lichess        => BSONString("l")
      case Comment.Author.Unknown        => BSONString("")
    }
  )
  implicit private val CommentBSONHandler = Macros.handler[Comment]

  implicit val CommentsBSONHandler: BSONHandler[Comments] =
    isoHandler[Comments, List[Comment]]((s: Comments) => s.value, Comments(_))

  implicit val GamebookBSONHandler = Macros.handler[Gamebook]

  implicit private def CrazyDataBSONHandler: BSON[Crazyhouse.Data] =
    new BSON[Crazyhouse.Data] {
      private def writePocket(p: Crazyhouse.Pocket) = p.roles.map(_.forsyth).mkString
      private def readPocket(p: String)             = Crazyhouse.Pocket(p.view.flatMap(chess.Role.forsyth).toList)
      def reads(r: Reader) =
        Crazyhouse.Data(
          promoted = r.getsD[Pos]("o").toSet,
          pockets = Crazyhouse.Pockets(
            white = readPocket(r.strD("w")),
            black = readPocket(r.strD("b"))
          )
        )
      def writes(w: Writer, s: Crazyhouse.Data) =
        $doc(
          "o" -> w.listO(s.promoted.toList),
          "w" -> w.strO(writePocket(s.pockets.white)),
          "b" -> w.strO(writePocket(s.pockets.black))
        )
    }

  implicit val GlyphsBSONHandler = {
    val intReader = collectionReader[List, Int]
    tryHandler[Glyphs](
      { case arr: Barr =>
        intReader.readTry(arr) map { ints =>
          Glyphs.fromList(ints flatMap Glyph.find)
        }
      },
      x => BSONArray(x.toList.map(_.id).map(BSONInteger.apply))
    )
  }

  implicit val EvalScoreBSONHandler = {
    val mateFactor = 1000000
    BSONIntegerHandler.as[Score](
      v =>
        Score {
          if (v >= mateFactor || v <= -mateFactor) Right(Eval.Mate(v / mateFactor))
          else Left(Eval.Cp(v))
        },
      _.value.fold(
        cp => cp.value atLeast (-mateFactor + 1) atMost (mateFactor - 1),
        mate => mate.value * mateFactor
      )
    )
  }

  def readNode(doc: Bdoc, id: UciCharPair): Option[Node] = {
    import Node.{ BsonFields => F }
    for {
      ply <- doc.getAsOpt[Int](F.ply)
      uci <- doc.getAsOpt[Uci](F.uci)
      san <- doc.getAsOpt[String](F.san)
      fen <- doc.getAsOpt[FEN](F.fen)
      check          = ~doc.getAsOpt[Boolean](F.check)
      shapes         = doc.getAsOpt[Shapes](F.shapes) getOrElse Shapes.empty
      comments       = doc.getAsOpt[Comments](F.comments) getOrElse Comments.empty
      gamebook       = doc.getAsOpt[Gamebook](F.gamebook)
      glyphs         = doc.getAsOpt[Glyphs](F.glyphs) getOrElse Glyphs.empty
      score          = doc.getAsOpt[Score](F.score)
      clock          = doc.getAsOpt[Centis](F.clock)
      crazy          = doc.getAsOpt[Crazyhouse.Data](F.crazy)
      forceVariation = ~doc.getAsOpt[Boolean](F.forceVariation)
    } yield Node(
      id,
      ply,
      WithSan(uci, san),
      fen,
      check,
      shapes,
      comments,
      gamebook,
      glyphs,
      score,
      clock,
      crazy,
      Node.emptyChildren,
      forceVariation
    )
  }

  def writeNode(n: Node) = {
    import Node.BsonFields._
    val w = new Writer
    $doc(
      ply            -> n.ply,
      uci            -> n.move.uci,
      san            -> n.move.san,
      fen            -> n.fen,
      check          -> w.boolO(n.check),
      shapes         -> n.shapes.value.nonEmpty.option(n.shapes),
      comments       -> n.comments.value.nonEmpty.option(n.comments),
      gamebook       -> n.gamebook,
      glyphs         -> n.glyphs.nonEmpty,
      score          -> n.score,
      clock          -> n.clock,
      crazy          -> n.crazyData,
      forceVariation -> w.boolO(n.forceVariation),
      order -> {
        (n.children.nodes.sizeIs > 1) option n.children.nodes.map(_.id)
      }
    )
  }

  import Node.Root
  implicit private[study] lazy val NodeRootBSONHandler: BSON[Root] = new BSON[Root] {
    import Node.BsonFields._
    def reads(fullReader: Reader) = {
      val rootNode = fullReader.doc.getAsOpt[Bdoc](Path.rootDbKey) err "Missing root"
      val r        = new Reader(rootNode)
      Root(
        ply = r int ply,
        fen = r.get[FEN](fen),
        check = r boolD check,
        shapes = r.getO[Shapes](shapes) | Shapes.empty,
        comments = r.getO[Comments](comments) | Comments.empty,
        gamebook = r.getO[Gamebook](gamebook),
        glyphs = r.getO[Glyphs](glyphs) | Glyphs.empty,
        score = r.getO[Score](score),
        clock = r.getO[Centis](clock),
        crazyData = r.getO[Crazyhouse.Data](crazy),
        children = StudyFlatTree.reader.rootChildren(fullReader.doc)
      )
    }
    def writes(w: Writer, r: Root) = $doc(
      StudyFlatTree.writer.rootChildren(r) appended {
        Path.rootDbKey -> $doc(
          ply      -> r.ply,
          fen      -> r.fen,
          check    -> r.check.some.filter(identity),
          shapes   -> r.shapes.value.nonEmpty.option(r.shapes),
          comments -> r.comments.value.nonEmpty.option(r.comments),
          gamebook -> r.gamebook,
          glyphs   -> r.glyphs.nonEmpty,
          score    -> r.score,
          clock    -> r.clock,
          crazy    -> r.crazyData
        )
      }
    )
  }

  implicit val PathBSONHandler = BSONStringHandler.as[Path](Path.apply, _.toString)
  implicit val VariantBSONHandler = tryHandler[Variant](
    { case BSONInteger(v) => Variant(v) toTry s"No such variant: $v" },
    x => BSONInteger(x.id)
  )

  implicit val PgnTagBSONHandler = tryHandler[Tag](
    { case BSONString(v) =>
      v.split(":", 2) match {
        case Array(name, value) => Success(Tag(name, value))
        case _                  => handlerBadValue(s"Invalid pgn tag $v")
      }
    },
    t => BSONString(s"${t.name}:${t.value}")
  )
  implicit val tagsHandler                     = implicitly[BSONHandler[List[Tag]]].as[Tags](Tags.apply, _.value)
  implicit private val ChapterSetupBSONHandler = Macros.handler[Chapter.Setup]
  implicit val ChapterRelayBSONHandler         = Macros.handler[Chapter.Relay]
  implicit val ChapterServerEvalBSONHandler    = Macros.handler[Chapter.ServerEval]
  import Chapter.Ply
  implicit val PlyBSONHandler             = intAnyValHandler[Ply](_.value, Ply.apply)
  implicit val ChapterBSONHandler         = Macros.handler[Chapter]
  implicit val ChapterMetadataBSONHandler = Macros.handler[Chapter.Metadata]

  implicit val PositionRefBSONHandler = tryHandler[Position.Ref](
    { case BSONString(v) => Position.Ref.decode(v) toTry s"Invalid position $v" },
    x => BSONString(x.encode)
  )
  implicit val StudyMemberRoleBSONHandler = tryHandler[StudyMember.Role](
    { case BSONString(v) => StudyMember.Role.byId get v toTry s"Invalid role $v" },
    x => BSONString(x.id)
  )
  private case class DbMember(role: StudyMember.Role) extends AnyVal
  implicit private val DbMemberBSONHandler = Macros.handler[DbMember]
  implicit private[study] val StudyMemberBSONWriter = new BSONWriter[StudyMember] {
    def writeTry(x: StudyMember) = DbMemberBSONHandler writeTry DbMember(x.role)
  }
  implicit private[study] val MembersBSONHandler: BSONHandler[StudyMembers] =
    implicitly[BSONHandler[Map[String, DbMember]]].as[StudyMembers](
      members =>
        StudyMembers(members map { case (id, dbMember) =>
          id -> StudyMember(id, dbMember.role)
        }),
      _.members.view.mapValues(m => DbMember(m.role)).toMap
    )
  import Study.Visibility
  implicit private[study] val VisibilityHandler = tryHandler[Visibility](
    { case BSONString(v) => Visibility.byKey get v toTry s"Invalid visibility $v" },
    v => BSONString(v.key)
  )
  import Study.From
  implicit private[study] val FromHandler = tryHandler[From](
    { case BSONString(v) =>
      v.split(' ') match {
        case Array("scratch")   => Success(From.Scratch)
        case Array("game", id)  => Success(From.Game(id))
        case Array("study", id) => Success(From.Study(Study.Id(id)))
        case Array("relay")     => Success(From.Relay(none))
        case Array("relay", id) => Success(From.Relay(Study.Id(id).some))
        case _                  => handlerBadValue(s"Invalid from $v")
      }
    },
    x =>
      BSONString(x match {
        case From.Scratch   => "scratch"
        case From.Game(id)  => s"game $id"
        case From.Study(id) => s"study $id"
        case From.Relay(id) => s"relay${id.fold("")(" " + _)}"
      })
  )
  import Settings.UserSelection
  implicit private[study] val UserSelectionHandler = tryHandler[UserSelection](
    { case BSONString(v) => UserSelection.byKey get v toTry s"Invalid user selection $v" },
    x => BSONString(x.key)
  )
  implicit val SettingsBSONHandler = new BSON[Settings] {
    def reads(r: Reader) =
      Settings(
        computer = r.get[UserSelection]("computer"),
        explorer = r.get[UserSelection]("explorer"),
        cloneable = r.getO[UserSelection]("cloneable") | Settings.init.cloneable,
        chat = r.getO[UserSelection]("chat") | Settings.init.chat,
        sticky = r.getO[Boolean]("sticky") | Settings.init.sticky,
        description = r.getO[Boolean]("description") | Settings.init.description
      )
    private val writer                 = Macros.writer[Settings]
    def writes(w: Writer, s: Settings) = writer.writeTry(s).get
  }

  import Study.Likes
  implicit val LikesBSONHandler = intAnyValHandler[Likes](_.value, Likes.apply)
  import Study.Rank
  implicit private[study] val RankBSONHandler = dateIsoHandler[Rank](Iso[DateTime, Rank](Rank.apply, _.value))

  // implicit val StudyBSONHandler = BSON.LoggingHandler(logger)(Macros.handler[Study])
  implicit val StudyBSONHandler = Macros.handler[Study]

  implicit val lightStudyBSONReader = new BSONDocumentReader[Study.LightStudy] {
    def readDocument(doc: BSONDocument) =
      Success(
        Study.LightStudy(
          isPublic = doc.string("visibility") has "public",
          contributors = doc.getAsOpt[StudyMembers]("members").??(_.contributorIds)
        )
      )
  }
}
