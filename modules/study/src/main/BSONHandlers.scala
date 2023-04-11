package lila.study

import chess.format.pgn.{ Glyph, Glyphs, Tag, Tags, SanStr }
import chess.format.{ Fen, Uci, UciCharPair }
import chess.variant.{ Crazyhouse, Variant }
import chess.{ Centis, ByColor, Square, PromotableRole, Role, Outcome, Ply, Check }
import reactivemongo.api.bson.*
import scala.util.Success

import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl.{ *, given }
import lila.tree.Eval
import lila.tree.Eval.Score
import lila.tree.Node.{ Comment, Comments, Gamebook, Shape, Shapes }

object BSONHandlers:

  private given BSONHandler[Square] = chessPosKeyHandler

  given BSON[Shape] with
    def reads(r: Reader) =
      val brush = r str "b"
      r.getO[Square]("p") map { pos =>
        Shape.Circle(brush, pos)
      } getOrElse Shape.Arrow(brush, r.get[Square]("o"), r.get[Square]("d"))
    def writes(@annotation.nowarn w: Writer, t: Shape) =
      t match
        case Shape.Circle(brush, pos)       => $doc("b" -> brush, "p" -> pos.key)
        case Shape.Arrow(brush, orig, dest) => $doc("b" -> brush, "o" -> orig.key, "d" -> dest.key)

  given chessRoleHandler: BSONHandler[PromotableRole] = tryHandler[PromotableRole](
    { case BSONString(v) => v.headOption flatMap Role.allPromotableByForsyth.get toTry s"No such role: $v" },
    x => BSONString(x.forsyth.toString)
  )

  given BSONHandler[Role] = tryHandler[Role](
    { case BSONString(v) => v.headOption flatMap Role.allByForsyth.get toTry s"No such role: $v" },
    x => BSONString(x.forsyth.toString)
  )

  given BSONHandler[Uci] = tryHandler[Uci](
    { case BSONString(v) => Uci(v) toTry s"Bad UCI: $v" },
    x => BSONString(x.uci)
  )

  given BSONHandler[UciCharPair] = tryHandler[UciCharPair](
    { case BSONString(v) =>
      v.toArray match {
        case Array(a, b) => Success(UciCharPair(a, b))
        case _           => handlerBadValue(s"Invalid UciCharPair $v")
      }
    },
    x => BSONString(x.toString)
  )

  import Study.IdName
  given BSONDocumentHandler[IdName] = Macros.handler

  given BSONHandler[Comment.Author] = quickHandler[Comment.Author](
    {
      case BSONString(n) if n == lila.user.User.lichessId.value || n == "l" => Comment.Author.Lichess
      case BSONString(name)                                                 => Comment.Author.External(name)
      case doc: Bdoc =>
        {
          for {
            id   <- doc.getAsOpt[UserId]("id")
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
  given BSONDocumentHandler[Comment] = Macros.handler

  given BSONDocumentHandler[Gamebook] = Macros.handler

  private given BSON[Crazyhouse.Data] with
    private def writePocket(p: Crazyhouse.Pocket) =
      p.values.flatMap { (role, nb) => List.fill(nb)(role.forsyth) }.mkString
    private def readPocket(p: String) = Crazyhouse.Pocket(p.view.flatMap(chess.Role.forsyth).toList)
    def reads(r: Reader) =
      Crazyhouse.Data(
        promoted = chess.bitboard.Bitboard(r.getsD[Square]("o")),
        pockets = ByColor(
          white = readPocket(r.strD("w")),
          black = readPocket(r.strD("b"))
        )
      )
    def writes(w: Writer, s: Crazyhouse.Data) =
      $doc(
        "o" -> w.listO(s.promoted.squares),
        "w" -> w.strO(writePocket(s.pockets.white)),
        "b" -> w.strO(writePocket(s.pockets.black))
      )

  given BSONHandler[Glyphs] =
    val intReader = collectionReader[List, Int]
    tryHandler[Glyphs](
      { case arr: Barr =>
        intReader.readTry(arr) map { ints =>
          Glyphs.fromList(ints flatMap Glyph.find)
        }
      },
      x => BSONArray(x.toList.map(_.id).map(BSONInteger.apply))
    )

  given BSONHandler[Score] =
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

  private[study] def readNode(doc: Bdoc, id: UciCharPair): Option[Node] =
    import Node.{ BsonFields as F }
    for {
      ply <- doc.getAsOpt[Ply](F.ply)
      uci <- doc.getAsOpt[Uci](F.uci)
      san <- doc.getAsOpt[SanStr](F.san)
      fen <- doc.getAsOpt[Fen.Epd](F.fen)
      check          = ~doc.getAsOpt[Check](F.check)
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
      Uci.WithSan(uci, san),
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

  private[study] def writeNode(n: Node) =
    import Node.BsonFields.*
    val w = new Writer
    $doc(
      ply            -> n.ply,
      uci            -> n.move.uci,
      san            -> n.move.san,
      fen            -> n.fen,
      check          -> w.yesnoO(n.check),
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

  import Node.Root
  private[study] given BSON[Root] with
    import Node.BsonFields.*
    def reads(fullReader: Reader) =
      val rootNode = fullReader.doc.getAsOpt[Bdoc](UciPathDb.rootDbKey) err "Missing root"
      val r        = new Reader(rootNode)
      Root(
        ply = r.get[Ply](ply),
        fen = r.get[Fen.Epd](fen),
        check = r yesnoD check,
        shapes = r.getO[Shapes](shapes) | Shapes.empty,
        comments = r.getO[Comments](comments) | Comments.empty,
        gamebook = r.getO[Gamebook](gamebook),
        glyphs = r.getO[Glyphs](glyphs) | Glyphs.empty,
        score = r.getO[Score](score),
        clock = r.getO[Centis](clock),
        crazyData = r.getO[Crazyhouse.Data](crazy),
        children = StudyFlatTree.reader.rootChildren(fullReader.doc)
      )
    def writes(w: Writer, r: Root) = $doc(
      StudyFlatTree.writer.rootChildren(r) appended {
        UciPathDb.rootDbKey -> $doc(
          ply      -> r.ply,
          fen      -> r.fen,
          check    -> w.yesnoO(r.check),
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

  given BSONHandler[Variant] = variantByIdHandler

  given BSONHandler[Tag] = tryHandler[Tag](
    { case BSONString(v) =>
      v.split(":", 2) match {
        case Array(name, value) => Success(Tag(name, value))
        case _                  => handlerBadValue(s"Invalid pgn tag $v")
      }
    },
    t => BSONString(s"${t.name}:${t.value}")
  )
  given (using handler: BSONHandler[List[Tag]]): BSONHandler[Tags] = handler.as[Tags](Tags.apply, _.value)
  private given BSONDocumentHandler[Chapter.Setup]                 = Macros.handler
  given BSONDocumentHandler[Chapter.Relay]                         = Macros.handler
  given BSONDocumentHandler[Chapter.ServerEval]                    = Macros.handler
  given BSONDocumentHandler[Chapter]                               = Macros.handler

  given BSONHandler[Position.Ref] = tryHandler(
    { case BSONString(v) => Position.Ref.decode(v) toTry s"Invalid position $v" },
    x => BSONString(x.encode)
  )
  given studyRoleHandler: BSONHandler[StudyMember.Role] = tryHandler(
    { case BSONString(v) => StudyMember.Role.byId get v toTry s"Invalid role $v" },
    x => BSONString(x.id)
  )
  private[study] case class DbMember(role: StudyMember.Role)
  private[study] given dbMemberHandler: BSONDocumentHandler[DbMember] = Macros.handler
  private[study] given BSONDocumentWriter[StudyMember] with
    def writeTry(x: StudyMember) = Success($doc("role" -> x.role))
  private[study] given (using handler: BSONHandler[Map[String, DbMember]]): BSONHandler[StudyMembers] =
    handler.as[StudyMembers](
      members =>
        StudyMembers(members map { case (id, dbMember) =>
          UserId(id) -> StudyMember(UserId(id), dbMember.role)
        }),
      _.members.view.map((id, m) => id.value -> DbMember(m.role)).toMap
    )
  import Study.Visibility
  private[study] given BSONHandler[Visibility] = tryHandler[Visibility](
    { case BSONString(v) => Visibility.byKey get v toTry s"Invalid visibility $v" },
    v => BSONString(v.key)
  )
  import Study.From
  private[study] given BSONHandler[From] = tryHandler[From](
    { case BSONString(v) =>
      v.split(' ') match {
        case Array("scratch")   => Success(From.Scratch)
        case Array("game", id)  => Success(From.Game(GameId(id)))
        case Array("study", id) => Success(From.Study(StudyId(id)))
        case Array("relay")     => Success(From.Relay(none))
        case Array("relay", id) => Success(From.Relay(StudyId(id).some))
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
  private[study] given BSONHandler[UserSelection] = tryHandler[UserSelection](
    { case BSONString(v) => UserSelection.byKey get v toTry s"Invalid user selection $v" },
    x => BSONString(x.key)
  )
  given BSON[Settings] with
    def reads(r: Reader) =
      Settings(
        computer = r.get[UserSelection]("computer"),
        explorer = r.get[UserSelection]("explorer"),
        cloneable = r.getO[UserSelection]("cloneable") | Settings.init.cloneable,
        shareable = r.getO[UserSelection]("shareable") | Settings.init.shareable,
        chat = r.getO[UserSelection]("chat") | Settings.init.chat,
        sticky = r.getO[Boolean]("sticky") | Settings.init.sticky,
        description = r.getO[Boolean]("description") | Settings.init.description
      )
    private val writer                 = Macros.writer[Settings]
    def writes(w: Writer, s: Settings) = writer.writeTry(s).get

  given studyHandler: BSONDocumentHandler[Study] = Macros.handler

  given BSONDocumentReader[Study.LightStudy] with
    def readDocument(doc: BSONDocument) =
      Success(
        Study.LightStudy(
          isPublic = doc.string("visibility") has "public",
          contributors = doc.getAsOpt[StudyMembers]("members").??(_.contributorIds)
        )
      )

  given BSONDocumentReader[Chapter.Metadata] with
    def readDocument(doc: Bdoc) = for {
      id    <- doc.getAsTry[StudyChapterId]("_id")
      name  <- doc.getAsTry[StudyChapterName]("name")
      setup <- doc.getAsTry[Chapter.Setup]("setup")
      outcome = doc
        .getAsOpt[List[String]]("tags")
        .flatMap {
          _.headOption // because only the Result: tag is fetched by metadataProjection
            .map(_ drop 7)
            .map(Outcome.fromResult)
        }
      hasRelayPath = doc.getAsOpt[Bdoc]("relay").flatMap(_ string "path").exists(_.nonEmpty)
    } yield Chapter.Metadata(id, name, setup, outcome, hasRelayPath)
