package lila.study

import cats.syntax.all.*

import chess.{ Square, White }
import chess.variant.Standard
import chess.format.UciPath
import chess.format.pgn.{ Glyph, Tags }

import lila.socket.AnaMove
import java.time.Instant

import lila.tree.{ Branch, Root }
import play.api.libs.json.*
import lila.study.StudySocket.Protocol.In.AtPosition
import lila.tree.Node.{ defaultNodeJsonWriter, Comment, Gamebook, Shape, Shapes }
import lila.common.Json.given
import lila.tree.Node.Comment.Text
import lila.user.User
import lila.socket.AnaDrop
import chess.format.pgn.Glyph
import lila.socket.AnaAny

val studyId      = StudyId("studyId")
val chapterId    = StudyChapterId("chapterId")
val userId       = UserId("nt9")
val user         = Comment.Author.User(userId, "nt9")
val studyInstant = Instant.ofEpochSecond(1685031726L)

class StudyIntegrationTest extends munit.FunSuite:

  val root = Root.default(Standard)
  val chapter = Chapter(
    chapterId,
    studyId,
    StudyChapterName("chapterName"),
    Chapter.Setup(None, Standard, White, None),
    root,
    Tags.empty,
    0,
    userId,
    None,
    None,
    None,
    None,
    None,
    None,
    studyInstant
  )

  import Helpers.*
  import StudyAction.*
  test("moves"):
    Fixtures.all.foreach: (str, expected) =>
      val output = chapter.execute(str).get
      assertEquals(rootToPgn(output.root).value, expected)

  extension (s: String)
    def toMoves: List[AnaMove] =
      s.linesIterator.toList
        .map(Json.parse(_).asInstanceOf[JsObject])
        .traverse(AnaMove.parse)
        .get

enum StudyAction:
  case Move(m: AnaMove)
  case Drop(m: AnaDrop)
  case DeleteNode(p: AtPosition)
  case SetComment(p: AtPosition, text: Text)
  // case DeleteComment(id: String) because ids are generated, so we don't have them statically at the beginning
  case SetShapes(p: AtPosition, shapes: List[Shape])
  case Promote(p: AtPosition, toMainline: Boolean)
  case ToggleGlyph(p: AtPosition, glyph: Glyph)
  case ClearVariations
  case ClearAnnotations

object StudyAction:

  // copy/pasted from JsonView
  private given Reads[Square] = Reads: v =>
    (v.asOpt[String] flatMap { Square.fromKey(_) }).fold[JsResult[Square]](JsError(Nil))(JsSuccess(_))
  private[study] given Reads[Shape] = Reads: js =>
    js.asOpt[JsObject]
      .flatMap { o =>
        for
          brush <- o str "brush"
          orig  <- o.get[Square]("orig")
        yield o.get[Square]("dest") match
          case Some(dest) => Shape.Arrow(brush, orig, dest)
          case _          => Shape.Circle(brush, orig)
      }
      .fold[JsResult[Shape]](JsError(Nil))(JsSuccess(_))

  import StudySocket.Protocol.In.given
  def parse(str: String): StudyAction =
    val jsObject = Json.parse(str).asInstanceOf[JsObject]
    jsObject.str("t") match
      case Some("anaMove") =>
        Move(AnaMove.parse(jsObject).get)
      case Some("AnaDrop") =>
        ???
        Drop(AnaDrop.parse(jsObject).get)
      case Some("deleteNode") =>
        DeleteNode(jsObject.get[AtPosition]("d").get)
      case Some("shapes") =>
        val p      = jsObject.get[AtPosition]("d").get
        val shapes = (jsObject \ "d" \ "shapes").asOpt[List[Shape]].get
        SetShapes(p, shapes)
      case Some("setComment") =>
        val p    = jsObject.get[AtPosition]("d").get
        val text = (jsObject \ "d" \ "text").asOpt[String].get
        SetComment(p, Comment.sanitize(text))
      case Some("promote") =>
        val p          = jsObject.get[AtPosition]("d").get
        val toMainline = (jsObject \ "d" \ "toMainLine").asOpt[Boolean].get
        Promote(p, toMainline)
      case Some("toggleGlyph") =>
        val p     = jsObject.get[AtPosition]("d").get
        val glyph = (jsObject \ "d" \ "id").asOpt[Int].flatMap(Glyph.find).get
        ToggleGlyph(p, glyph)
      case Some("clearAnnotations") =>
        ClearAnnotations
      case Some("clearVariation") =>
        ClearVariations
      case _ => throw Exception(s"cannot parse $str")

  // combined of StudySocket.moveOrDrop & StudyApi.addNode
  def moveOrDrop(chapter: Chapter, move: AnaAny): Option[Chapter] =
    move.branch.toOption.flatMap(b => chapter.addNode(b.withoutChildren, move.path, None))

  def deleteNodeAt(chapter: Chapter, position: Position.Ref) =
    chapter.updateRoot: root =>
      root.withChildren(_.deleteNodeAt(position.path))

  def setComment(chapter: Chapter, position: Position.Ref, text: Comment.Text) =
    val comment = Comment(
      id = Comment.Id.make,
      text = text,
      by = user
    )
    chapter.setComment(comment, position.path)

  def setShapes(chapter: Chapter, position: Position.Ref, shapes: Shapes) =
    chapter.setShapes(shapes, position.path)

  def promote(chapter: Chapter, position: Position.Ref, toMainline: Boolean) =
    chapter
      .updateRoot:
        _.withChildren: children =>
          if toMainline then children.promoteToMainlineAt(position.path)
          else children.promoteUpAt(position.path).map(_._1)

  def toggleGlyph(chapter: Chapter, position: Position.Ref, glyph: Glyph) =
    chapter.toggleGlyph(glyph, position.path)

  def clearAnnotations(chapter: Chapter) =
    chapter.updateRoot: root =>
      root.withChildren: children =>
        children.updateAllWith(_.clearAnnotations).some

  def clearVariations(chapter: Chapter) =
    chapter.copy(root = chapter.root.clearVariations).some

  extension (chapter: Chapter)
    def execute(action: StudyAction): Option[Chapter] =
      action match
        case StudyAction.Move(move) =>
          moveOrDrop(chapter, move)
        case StudyAction.Drop(drop) =>
          moveOrDrop(chapter, drop)
        case StudyAction.DeleteNode(pos) =>
          deleteNodeAt(chapter, pos.ref)
        case StudyAction.SetComment(pos, text) =>
          setComment(chapter, pos.ref, text)
        case StudyAction.SetShapes(pos, shapes) =>
          setShapes(chapter, pos.ref, Shapes(shapes))
        case StudyAction.Promote(p, toMainline) =>
          promote(chapter, p.ref, toMainline)
        case StudyAction.ToggleGlyph(p, glyph) =>
          toggleGlyph(chapter, p.ref, glyph)
        case StudyAction.ClearVariations =>
          clearVariations(chapter)
        case StudyAction.ClearAnnotations =>
          clearAnnotations(chapter)

    def execute(actions: List[StudyAction]): Option[Chapter] =
      actions.foldLeft(chapter.some): (chapter, action) =>
        chapter.flatMap(_.execute(action))

    def execute(str: String): Option[Chapter] =
      chapter.execute(
        str.linesIterator.toList
          .map(StudyAction.parse)
      )

object Fixtures:

  val m0   = ""
  val pgn0 = ""

  val m1 = """
{"t":"anaMove","d":{"orig":"d2","dest":"d4","fen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","path":"","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"d7","dest":"d5","fen":"rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq - 0 1","path":".>","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"c2","dest":"c4","fen":"rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2","path":".>VF","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"e7","dest":"e6","fen":"rnbqkbnr/ppp1pppp/8/3p4/2PP4/8/PP2PPPP/RNBQKBNR b KQkq - 0 2","path":".>VF-=","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"b1","dest":"c3","fen":"rnbqkbnr/ppp2ppp/4p3/3p4/2PP4/8/PP2PPPP/RNBQKBNR w KQkq - 0 3","path":".>VF-=WO","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"g8","dest":"f6","fen":"rnbqkbnr/ppp2ppp/4p3/3p4/2PP4/2N5/PP2PPPP/R1BQKBNR b KQkq - 1 3","path":".>VF-=WO$5","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"c4","dest":"d5","fen":"rnbqkb1r/ppp2ppp/4pn2/3p4/2PP4/2N5/PP2PPPP/R1BQKBNR w KQkq - 2 4","path":".>VF-=WO$5aP","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"e6","dest":"d5","fen":"rnbqkb1r/ppp2ppp/4pn2/3P4/3P4/2N5/PP2PPPP/R1BQKBNR b KQkq - 0 4","path":".>VF-=WO$5aP=F","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"c1","dest":"g5","fen":"rnbqkb1r/ppp2ppp/5n2/3p4/3P4/2N5/PP2PPPP/R1BQKBNR w KQkq - 0 5","path":".>VF-=WO$5aP=FOF","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"f8","dest":"e7","fen":"rnbqkb1r/ppp2ppp/5n2/3p2B1/3P4/2N5/PP2PPPP/R2QKBNR b KQkq - 1 5","path":".>VF-=WO$5aP=FOF%I","ch":"kMOZO15F"}}
  """.trim
  val pgn1 = "1. d4 d5 2. c4 e6 3. Nc3 Nf6 4. cxd5 exd5 5. Bg5 Be7"

  val m2 = """
{"t":"anaMove","d":{"orig":"d2","dest":"d4","fen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","path":"","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"d7","dest":"d5","fen":"rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq - 0 1","path":".>","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"c2","dest":"c4","fen":"rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2","path":".>VF","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"e7","dest":"e6","fen":"rnbqkbnr/ppp1pppp/8/3p4/2PP4/8/PP2PPPP/RNBQKBNR b KQkq - 0 2","path":".>VF-=","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"b1","dest":"c3","fen":"rnbqkbnr/ppp2ppp/4p3/3p4/2PP4/8/PP2PPPP/RNBQKBNR w KQkq - 0 3","path":".>VF-=WO","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"g8","dest":"f6","fen":"rnbqkbnr/ppp2ppp/4p3/3p4/2PP4/2N5/PP2PPPP/R1BQKBNR b KQkq - 1 3","path":".>VF-=WO$5","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"c4","dest":"d5","fen":"rnbqkb1r/ppp2ppp/4pn2/3p4/2PP4/2N5/PP2PPPP/R1BQKBNR w KQkq - 2 4","path":".>VF-=WO$5aP","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"e6","dest":"d5","fen":"rnbqkb1r/ppp2ppp/4pn2/3P4/3P4/2N5/PP2PPPP/R1BQKBNR b KQkq - 0 4","path":".>VF-=WO$5aP=F","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"c1","dest":"g5","fen":"rnbqkb1r/ppp2ppp/5n2/3p4/3P4/2N5/PP2PPPP/R1BQKBNR w KQkq - 0 5","path":".>VF-=WO$5aP=FOF","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"f8","dest":"e7","fen":"rnbqkb1r/ppp2ppp/5n2/3p2B1/3P4/2N5/PP2PPPP/R2QKBNR b KQkq - 1 5","path":".>VF-=WO$5aP=FOF%I","ch":"kMOZO15F"}}
{"t":"deleteNode","d":{"path":".>VF-=WO$5aP=FOF%I","jumpTo":".>VF-=WO$5aP=FOF","ch":"kMOZO15F"}}
  """.trim
  val pgn2 = "1. d4 d5 2. c4 e6 3. Nc3 Nf6 4. cxd5 exd5"

  val m3 = """
{"t":"anaMove","d":{"orig":"d2","dest":"d4","fen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","path":"","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"d7","dest":"d5","fen":"rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq - 0 1","path":".>","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"c2","dest":"c4","fen":"rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2","path":".>VF","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"e7","dest":"e6","fen":"rnbqkbnr/ppp1pppp/8/3p4/2PP4/8/PP2PPPP/RNBQKBNR b KQkq - 0 2","path":".>VF-=","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"b1","dest":"c3","fen":"rnbqkbnr/ppp2ppp/4p3/3p4/2PP4/8/PP2PPPP/RNBQKBNR w KQkq - 0 3","path":".>VF-=WO","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"g8","dest":"f6","fen":"rnbqkbnr/ppp2ppp/4p3/3p4/2PP4/2N5/PP2PPPP/R1BQKBNR b KQkq - 1 3","path":".>VF-=WO$5","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"c4","dest":"d5","fen":"rnbqkb1r/ppp2ppp/4pn2/3p4/2PP4/2N5/PP2PPPP/R1BQKBNR w KQkq - 2 4","path":".>VF-=WO$5aP","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"e6","dest":"d5","fen":"rnbqkb1r/ppp2ppp/4pn2/3P4/3P4/2N5/PP2PPPP/R1BQKBNR b KQkq - 0 4","path":".>VF-=WO$5aP=F","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"c1","dest":"g5","fen":"rnbqkb1r/ppp2ppp/5n2/3p4/3P4/2N5/PP2PPPP/R1BQKBNR w KQkq - 0 5","path":".>VF-=WO$5aP=FOF","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"f8","dest":"e7","fen":"rnbqkb1r/ppp2ppp/5n2/3p2B1/3P4/2N5/PP2PPPP/R2QKBNR b KQkq - 1 5","path":".>VF-=WO$5aP=FOF%I","ch":"kMOZO15F"}}
{"t":"anaMove","d":{"orig":"f8","dest":"e7","fen":"rnbqkbnr/ppp2ppp/4p3/3p4/2PP4/2N5/PP2PPPP/R1BQKBNR b KQkq - 1 3","path":".>VF-=WO$5","ch":"kMOZO15F"}}
{"t":"setComment","d":{"ch":"kMOZO15F","path":".>VF-=WO$5`W","text":"A better move, prevent 5. Bg5"}}
{"t":"shapes","d":{"path":".>VF-=WO$5`W","shapes":[{"orig":"f2","brush":"green"}],"ch":"kMOZO15F"}}

  """.trim
  val pgn3 =
    "1. d4 d5 2. c4 e6 3. Nc3 Nf6 (3... Be7 { A better move, prevent 5. Bg5 } { [%csl Gf2] }) 4. cxd5 exd5 5. Bg5 Be7"

  val ms  = List(m0, m1, m2, m3)
  val ps  = List(pgn0, pgn1, pgn2, pgn3)
  val all = ms.zip(ps)
