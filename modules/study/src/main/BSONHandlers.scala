package lila.study

import chess.format.pgn.{ Glyph, Glyphs, SanStr, Tag, Tags }
import chess.format.{ Fen, Uci, UciCharPair, UciPath }
import chess.variant.{ Crazyhouse, Variant }
import chess.{ ByColor, Centis, Check, FideId, Ply, PromotableRole, Role, Square }
import chess.eval.*
import reactivemongo.api.bson.*

import scala.util.Success

import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl.{ *, given }
import lila.tree.Node.{ Comment, Comments, Gamebook, Shape, Shapes }
import lila.tree.{ Branch, Branches, Metas, NewBranch, NewRoot, Root, Clock }

object BSONHandlers:

  private given BSONHandler[Square] = chessPosKeyHandler

  given BSON[Shape] with
    def reads(r: Reader) =
      val brush = r.str("b")
      r.getO[Square]("p")
        .map(Shape.Circle(brush, _))
        .getOrElse(Shape.Arrow(brush, r.get[Square]("o"), r.get[Square]("d")))
    def writes(w: Writer, t: Shape) =
      t match
        case Shape.Circle(brush, pos)       => $doc("b" -> brush, "p" -> pos.key)
        case Shape.Arrow(brush, orig, dest) => $doc("b" -> brush, "o" -> orig.key, "d" -> dest.key)

  given chessRoleHandler: BSONHandler[PromotableRole] = tryHandler[PromotableRole](
    { case BSONString(v) =>
      v.headOption.flatMap(Role.allPromotableByForsyth.get).toTry(s"No such role: $v")
    },
    x => BSONString(x.forsyth.toString)
  )

  given BSONHandler[Role] = tryHandler[Role](
    { case BSONString(v) => v.headOption.flatMap(Role.allByForsyth.get).toTry(s"No such role: $v") },
    x => BSONString(x.forsyth.toString)
  )

  given BSONHandler[Uci] = tryHandler[Uci](
    { case BSONString(v) => Uci(v).toTry(s"Bad UCI: $v") },
    x => BSONString(x.uci)
  )

  given BSONHandler[UciCharPair] = tryHandler[UciCharPair](
    { case BSONString(v) =>
      v.toArray match
        case Array(a, b) => Success(UciCharPair(a, b))
        case _           => handlerBadValue(s"Invalid UciCharPair $v")
    },
    x => BSONString(x.toString)
  )

  given studyIdNameHandler: BSONDocumentHandler[lila.core.study.IdName] = Macros.handler
  given chapterIdNameHandler: BSONDocumentHandler[Chapter.IdName]       = Macros.handler

  given BSONHandler[Comment.Author] = quickHandler[Comment.Author](
    {
      case BSONString(n) if n == UserId.lichess.value || n == "l" => Comment.Author.Lichess
      case BSONString(name)                                       => Comment.Author.External(name)
      case doc: Bdoc                                              =>
        {
          for
            id   <- doc.getAsOpt[UserId]("id")
            name <- doc.getAsOpt[String]("name")
          yield Comment.Author.User(id, name)
        }.err(s"Invalid comment author $doc")
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

  given BSONHandler[Clock] = isoHandler(_.centis, Clock(_))

  private given BSON[Crazyhouse.Data] with
    private def writePocket(p: Crazyhouse.Pocket) =
      p.flatMap((role, nb) => List.fill(nb)(role.forsyth)).mkString
    private def readPocket(p: String) = Crazyhouse.Pocket(p.view.flatMap(chess.Role.forsyth).toList)
    def reads(r: Reader)              =
      Crazyhouse.Data(
        promoted = chess.Bitboard(r.getsD[Square]("o")),
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
        intReader.readTry(arr).map(ints => Glyphs.fromList(ints.flatMap(Glyph.find)))
      },
      x => BSONArray(x.toList.map(_.id).map(BSONInteger.apply))
    )

  given BSONHandler[Score] =
    val mateFactor = 1000000
    BSONIntegerHandler.as[Score](
      v =>
        if v >= mateFactor || v <= -mateFactor then Score.mate(v / mateFactor)
        else Score.cp(v),
      _.fold(
        cp => cp.value.atLeast(-mateFactor + 1).atMost(mateFactor - 1),
        mate => mate.value * mateFactor
      )
    )

  // shallow read, as not reading children
  private[study] def readBranch(doc: Bdoc, id: UciCharPair): Option[Branch] =
    import Node.BsonFields as F
    for
      ply <- doc.getAsOpt[Ply](F.ply)
      uci <- doc.getAsOpt[Uci](F.uci)
      san <- doc.getAsOpt[SanStr](F.san)
      fen <- doc.getAsOpt[Fen.Full](F.fen)
      check          = ~doc.getAsOpt[Check](F.check)
      shapes         = doc.getAsOpt[Shapes](F.shapes).getOrElse(Shapes.empty)
      comments       = doc.getAsOpt[Comments](F.comments).getOrElse(Comments.empty)
      gamebook       = doc.getAsOpt[Gamebook](F.gamebook)
      glyphs         = doc.getAsOpt[Glyphs](F.glyphs).getOrElse(Glyphs.empty)
      eval           = doc.getAsOpt[Score](F.score).map(lila.tree.evals.fromScore)
      clock          = doc.getAsOpt[Clock](F.clock)
      crazyData      = doc.getAsOpt[Crazyhouse.Data](F.crazy)
      forceVariation = ~doc.getAsOpt[Boolean](F.forceVariation)
    yield Branch(
      id = id,
      ply = ply,
      move = Uci.WithSan(uci, san),
      fen = fen,
      check = check,
      shapes = shapes,
      comments = comments,
      gamebook = gamebook,
      glyphs = glyphs,
      eval = eval,
      clock = clock,
      crazyData = crazyData,
      children = Branches.empty,
      forceVariation = forceVariation
    )

  // shallow read, as not reading children
  private[study] def readNewBranch(doc: Bdoc, path: UciPath): Option[NewBranch] =
    import Node.BsonFields as F
    for
      id  <- path.lastId
      ply <- doc.getAsOpt[Ply](F.ply)
      uci <- doc.getAsOpt[Uci](F.uci)
      san <- doc.getAsOpt[SanStr](F.san)
      fen <- doc.getAsOpt[Fen.Full](F.fen)
      check          = ~doc.getAsOpt[Check](F.check)
      shapes         = doc.getAsOpt[Shapes](F.shapes).getOrElse(Shapes.empty)
      comments       = doc.getAsOpt[Comments](F.comments).getOrElse(Comments.empty)
      gamebook       = doc.getAsOpt[Gamebook](F.gamebook)
      glyphs         = doc.getAsOpt[Glyphs](F.glyphs).getOrElse(Glyphs.empty)
      eval           = doc.getAsOpt[Score](F.score).map(lila.tree.evals.fromScore)
      clock          = doc.getAsOpt[Clock](F.clock)
      crazyData      = doc.getAsOpt[Crazyhouse.Data](F.crazy)
      forceVariation = ~doc.getAsOpt[Boolean](F.forceVariation)
    yield NewBranch(
      id = id,
      forceVariation = forceVariation,
      move = Uci.WithSan(uci, san),
      metas = Metas(
        ply = ply,
        fen = fen,
        check = check,
        shapes = shapes,
        comments = comments,
        gamebook = gamebook,
        glyphs = glyphs,
        eval = eval,
        clock = clock,
        crazyData = crazyData
      )
    )

  // shallow write, as not writing children
  private[study] def writeBranch(n: Branch) =
    import Node.BsonFields as F
    val w = new Writer
    $doc(
      F.ply            -> n.ply,
      F.uci            -> n.move.uci,
      F.san            -> n.move.san,
      F.fen            -> n.fen,
      F.check          -> w.yesnoO(n.check),
      F.shapes         -> n.shapes.value.nonEmpty.option(n.shapes),
      F.comments       -> n.comments.value.nonEmpty.option(n.comments),
      F.gamebook       -> n.gamebook,
      F.glyphs         -> n.glyphs.nonEmpty,
      F.score          -> n.eval.flatMap(_.score), // BC stored as score (maybe its better to keep this way?)
      F.clock          -> n.clock,
      F.crazy          -> n.crazyData,
      F.forceVariation -> w.boolO(n.forceVariation)
    )

  private[study] def writeNewBranch(n: NewBranch) =
    import Node.BsonFields as F
    val w = new Writer
    $doc(
      F.ply      -> n.metas.ply,
      F.uci      -> n.move.uci,
      F.san      -> n.move.san,
      F.fen      -> n.metas.fen,
      F.check    -> w.yesnoO(n.metas.check),
      F.shapes   -> n.metas.shapes.value.nonEmpty.option(n.metas.shapes),
      F.comments -> n.metas.comments.value.nonEmpty.option(n.metas.comments),
      F.gamebook -> n.metas.gamebook,
      F.glyphs   -> n.metas.glyphs.nonEmpty,
      F.score    -> n.metas.eval.flatMap(_.score), // BC stored as score (maybe its better to keep this way?)
      F.clock    -> n.metas.clock,
      F.crazy    -> n.metas.crazyData,
      F.forceVariation -> w.boolO(n.forceVariation)
    )

  private[study] given BSON[Root] with
    import Node.BsonFields as F
    def reads(fullReader: Reader) =
      val rootNode = fullReader.doc.getAsOpt[Bdoc](UciPathDb.rootDbKey).err("Missing root")
      val r        = Reader(rootNode)
      Root(
        ply = r.get[Ply](F.ply),
        fen = r.get[Fen.Full](F.fen),
        check = r.yesnoD(F.check),
        shapes = r.getO[Shapes](F.shapes) | Shapes.empty,
        comments = r.getO[Comments](F.comments) | Comments.empty,
        gamebook = r.getO[Gamebook](F.gamebook),
        glyphs = r.getO[Glyphs](F.glyphs) | Glyphs.empty,
        eval = r.getO[Score](F.score).map(lila.tree.evals.fromScore),
        clock = r.getO[Clock](F.clock),
        crazyData = r.getO[Crazyhouse.Data](F.crazy),
        children = StudyFlatTree.reader.rootChildren(fullReader.doc)
      )
    def writes(w: Writer, r: Root) = $doc(
      StudyFlatTree.writer.rootChildren(r).appended {
        UciPathDb.rootDbKey -> $doc(
          F.ply      -> r.ply,
          F.fen      -> r.fen,
          F.check    -> w.yesnoO(r.check),
          F.shapes   -> r.shapes.value.nonEmpty.option(r.shapes),
          F.comments -> r.comments.value.nonEmpty.option(r.comments),
          F.gamebook -> r.gamebook,
          F.glyphs   -> r.glyphs.nonEmpty,
          F.score    -> r.eval.flatMap(_.score), // BC stored as score (maybe its better to keep this way?)
          F.clock    -> r.clock,
          F.crazy    -> r.crazyData
        )
      }
    )

  private[study] given BSON[NewRoot] with
    import Node.BsonFields as F
    def reads(fullReader: Reader) =
      val rootNode = fullReader.doc.getAsOpt[Bdoc](UciPathDb.rootDbKey).err("Missing root")
      val r        = Reader(rootNode)
      NewRoot(
        Metas(
          ply = r.get[Ply](F.ply),
          fen = r.get[Fen.Full](F.fen),
          check = r.yesnoD(F.check),
          shapes = r.getO[Shapes](F.shapes) | Shapes.empty,
          comments = r.getO[Comments](F.comments) | Comments.empty,
          gamebook = r.getO[Gamebook](F.gamebook),
          glyphs = r.getO[Glyphs](F.glyphs) | Glyphs.empty,
          eval = r.getO[Score](F.score).map(lila.tree.evals.fromScore),
          clock = r.getO[Clock](F.clock),
          crazyData = r.getO[Crazyhouse.Data](F.crazy)
        ),
        tree = StudyFlatTree.reader.newRoot(fullReader.doc)
      )
    def writes(w: Writer, r: NewRoot) = $doc(
      StudyFlatTree.writer.newRootChildren(r).appended {
        UciPathDb.rootDbKey -> $doc(
          F.ply      -> r.metas.ply,
          F.fen      -> r.metas.fen,
          F.check    -> w.yesnoO(r.metas.check),
          F.shapes   -> r.metas.shapes.value.nonEmpty.option(r.metas.shapes),
          F.comments -> r.metas.comments.value.nonEmpty.option(r.metas.comments),
          F.gamebook -> r.metas.gamebook,
          F.glyphs   -> r.metas.glyphs.nonEmpty,
          F.score -> r.metas.eval.flatMap(_.score), // BC stored as score (maybe its better to keep this way?)
          F.clock -> r.metas.clock,
          F.crazy -> r.metas.crazyData
        )
      }
    )
  given BSONHandler[Variant] = variantByIdHandler

  given BSONHandler[Tag] = tryHandler[Tag](
    { case BSONString(v) =>
      v.split(":", 2) match
        case Array(name, value) => Success(Tag(name, value))
        case _                  => handlerBadValue(s"Invalid pgn tag $v")
    },
    t => BSONString(s"${t.name}:${t.value}")
  )
  given (using handler: BSONHandler[List[Tag]]): BSONHandler[Tags] = handler.as[Tags](Tags.apply, _.value)
  private given BSONDocumentHandler[Chapter.Setup]                 = Macros.handler
  given BSONHandler[Option[FideId]]                                = quickHandler(
    { case BSONInteger(v) => (v > 0).option(FideId(v)) },
    id => BSONInteger(id.so(_.value))
  )
  given BSONDocumentHandler[Chapter.Relay]      = Macros.handler
  given BSONDocumentHandler[Chapter.ServerEval] = Macros.handler

  private val clockPair: BSONHandler[PairOf[Option[Centis]]] = optionPairHandler
  given BSONHandler[Chapter.BothClocks] = clockPair.as[Chapter.BothClocks](ByColor.fromPair, _.toPair)
  given BSONHandler[Chapter.Check]      = quickHandler[Chapter.Check](
    { case BSONString(v) => if v == "#" then Chapter.Check.Mate else Chapter.Check.Check },
    v => BSONString(if v == Chapter.Check.Mate then "#" else "+")
  )
  given BSON[Chapter.LastPosDenorm] with
    def reads(r: Reader) = Chapter.LastPosDenorm(
      fen = r.getO[Fen.Full]("fen") | Fen.initial,
      uci = r.getO[Uci]("uci"),
      check = r.getO[Chapter.Check]("check"),
      clocks = ~r.getO[Chapter.BothClocks]("clocks")
    )
    def writes(w: Writer, l: Chapter.LastPosDenorm) = $doc(
      "fen"    -> l.fen.some.filterNot(Fen.Full.isInitial),
      "uci"    -> l.uci,
      "check"  -> l.check,
      "clocks" -> l.clocks.some.filter(_.exists(_.isDefined))
    )

  given BSONDocumentHandler[Chapter] = Macros.handler

  given BSONHandler[Position.Ref] = tryHandler(
    { case BSONString(v) => Position.Ref.decode(v).toTry(s"Invalid position $v") },
    x => BSONString(x.encode)
  )
  given studyRoleHandler: BSONHandler[StudyMember.Role] = tryHandler(
    { case BSONString(v) => StudyMember.Role.byId.get(v).toTry(s"Invalid role $v") },
    x => BSONString(x.id)
  )
  private[study] case class DbMember(role: StudyMember.Role)
  private[study] given dbMemberHandler: BSONDocumentHandler[DbMember] = Macros.handler
  private[study] given BSONDocumentWriter[StudyMember] with
    def writeTry(x: StudyMember) = Success($doc("role" -> x.role))

  private[study] given (using handler: BSONHandler[Map[String, DbMember]]): BSONHandler[StudyMembers] =
    handler.as[StudyMembers](
      members =>
        StudyMembers(members.map { (id, dbMember) =>
          UserId(id) -> StudyMember(UserId(id), dbMember.role)
        }),
      _.members.view.map((id, m) => id.value -> DbMember(m.role)).toMap
    )

  import lila.core.study.Visibility
  given visibilityHandler: BSONHandler[Visibility] = tryHandler[Visibility](
    { case BSONString(v) => Visibility.byKey.get(v).toTry(s"Invalid visibility $v") },
    v => BSONString(v.toString)
  )
  import Study.From
  private[study] given BSONHandler[From] = tryHandler[From](
    { case BSONString(v) =>
      v.split(' ') match
        case Array("scratch")   => Success(From.Scratch)
        case Array("game", id)  => Success(From.Game(GameId(id)))
        case Array("study", id) => Success(From.Study(StudyId(id)))
        case Array("relay")     => Success(From.Relay(none))
        case Array("relay", id) => Success(From.Relay(StudyId(id).some))
        case _                  => handlerBadValue(s"Invalid from $v")
    },
    x =>
      BSONString(x match
        case From.Scratch   => "scratch"
        case From.Game(id)  => s"game $id"
        case From.Study(id) => s"study $id"
        case From.Relay(id) => s"relay${id.fold("")(" " + _)}")
  )
  import Settings.UserSelection
  private[study] given BSONHandler[UserSelection] = tryHandler[UserSelection](
    { case BSONString(v) => UserSelection.byKey.get(v).toTry(s"Invalid user selection $v") },
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
          isPublic = doc.string("visibility").has("public"),
          contributors = doc.getAsOpt[StudyMembers]("members").so(_.contributorIds)
        )
      )
