package lila.study

import chess.format.pgn.{ Glyph, Tags, Comment as CommentStr }
import chess.variant.*
import chess.{ Square, White }
import play.api.libs.json.*

import java.time.Instant

import lila.study.StudySocket.Protocol.In.AtPosition
import lila.tree.Node.{ Comment, Shape, Shapes }
import lila.tree.Root

val studyId = StudyId("studyId")
val chapterId = StudyChapterId("chapterId")
val userId = UserId("nt9")
val user = Comment.Author.User(userId, "nt9")
val studyInstant = Instant.ofEpochSecond(1685031726L)

class StudyIntegrationTest extends munit.FunSuite:

  def defaultChapter(variant: Variant): Chapter =
    val root = Root.default(variant)
    Chapter(
      chapterId,
      studyId,
      StudyChapterName("chapterName"),
      Chapter.Setup(None, variant, White, None),
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
      None,
      studyInstant
    ).updateDenorm

  import Helpers.*
  import StudyAction.*

  test("all actions"):
    TestCase.all.foreach: testCase =>
      val chapter = defaultChapter(testCase.variant)
      val output = chapter.execute(testCase.actions).get
      assertEquals(rootToPgn(output.root).value, testCase.expected)

case class TestCase(variant: Variant, actions: List[StudyAction], expected: String)

object TestCase:
  def apply(variant: Variant, actionStr: String, expected: String): TestCase =
    val actions = actionStr.linesIterator.toList.map(StudyAction.parse)
    TestCase(variant, actions, expected)

  import Fixtures.*
  val all = standards.map((str, expected) => TestCase(Standard, str, expected))
    ++ crazyhouses.map((str, expected) => TestCase(Crazyhouse, str, expected))
    ++ antichess.map((str, expected) => TestCase(Antichess, str, expected))

enum StudyAction:
  case Move(m: AnaMove)
  case Drop(m: AnaDrop)
  case DeleteNode(p: AtPosition)
  case SetComment(p: AtPosition, text: CommentStr)
  // case DeleteComment(id: String) because ids are generated, so we don't have them statically at the beginning
  case SetShapes(p: AtPosition, shapes: List[Shape])
  case Promote(p: AtPosition, toMainline: Boolean)
  case ToggleGlyph(p: AtPosition, glyph: Glyph)
  case ClearVariations
  case ClearAnnotations

object StudyAction:

  // copy/pasted from JsonView
  private given Reads[Square] = Reads: v =>
    (v.asOpt[String].flatMap { Square.fromKey(_) }).fold[JsResult[Square]](JsError(Nil))(JsSuccess(_))
  private[study] given Reads[Shape] = Reads: js =>
    js.asOpt[JsObject]
      .flatMap { o =>
        for
          brush <- o.str("brush")
          orig <- o.get[Square]("orig")
        yield o.get[Square]("dest") match
          case Some(dest) => Shape.Arrow(brush, orig, dest)
          case _ => Shape.Circle(brush, orig)
      }
      .fold[JsResult[Shape]](JsError(Nil))(JsSuccess(_))

  import StudySocket.Protocol.In.given
  def parse(str: String): StudyAction =
    val jsObject = Json.parse(str).asInstanceOf[JsObject]
    jsObject.str("t") match
      case Some("anaMove") =>
        Move(AnaMove.parse(jsObject).get)
      case Some("anaDrop") =>
        Drop(AnaDrop.parse(jsObject).get)
      case Some("deleteNode") =>
        DeleteNode(jsObject.get[AtPosition]("d").get)
      case Some("shapes") =>
        val p = jsObject.get[AtPosition]("d").get
        val shapes = (jsObject \ "d" \ "shapes").asOpt[List[Shape]].get
        SetShapes(p, shapes)
      case Some("setComment") =>
        val p = jsObject.get[AtPosition]("d").get
        val text = (jsObject \ "d" \ "text").asOpt[String].get
        SetComment(p, Comment.sanitize(text))
      case Some("promote") =>
        val p = jsObject.get[AtPosition]("d").get
        val toMainline = (jsObject \ "d" \ "toMainline").asOpt[Boolean].get
        Promote(p, toMainline)
      case Some("toggleGlyph") =>
        val p = jsObject.get[AtPosition]("d").get
        val glyph = (jsObject \ "d" \ "id").asOpt[Int].flatMap(Glyph.find).get
        ToggleGlyph(p, glyph)
      case Some("clearAnnotations") =>
        ClearAnnotations
      case Some("clearVariations") =>
        ClearVariations
      case _ => throw Exception(s"cannot parse $str")

  // combined of StudySocket.moveOrDrop & StudyApi.addNode
  def moveOrDrop(chapter: Chapter, move: AnaAny): Option[Chapter] =
    move.branch.toOption.flatMap(b => chapter.addNode(b.withoutChildren, move.path, None))

  def deleteNodeAt(chapter: Chapter, position: Position.Ref) =
    chapter.updateRoot: root =>
      root.withChildren(_.deleteNodeAt(position.path))

  def setComment(chapter: Chapter, position: Position.Ref, text: CommentStr) =
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
          else children.promoteUpAt(position.path)._1F

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

object Fixtures:

  val m0 = ""
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

  // https://lichess.org/study/Q41XcI0B/XTMYNVqi
  val m4 = """
{"t":"anaMove","d":{"orig":"e2","dest":"e4","fen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","path":"","ch":"XTMYNVqi"}}
{"t":"anaMove","d":{"orig":"e7","dest":"e6","fen":"rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1","path":"/?","ch":"XTMYNVqi"}}
{"t":"anaMove","d":{"orig":"d2","dest":"d4","fen":"rnbqkbnr/pppp1ppp/4p3/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2","path":"/?WO","ch":"XTMYNVqi"}}
{"t":"anaMove","d":{"orig":"d7","dest":"d5","fen":"rnbqkbnr/pppp1ppp/4p3/8/3PP3/8/PPP2PPP/RNBQKBNR b KQkq - 0 2","path":"/?WO.>","ch":"XTMYNVqi"}}
{"t":"anaMove","d":{"orig":"e4","dest":"d5","fen":"rnbqkbnr/ppp2ppp/4p3/3p4/3PP3/8/PPP2PPP/RNBQKBNR w KQkq - 0 3","path":"/?WO.>VF","ch":"XTMYNVqi"}}
{"t":"anaMove","d":{"orig":"e6","dest":"d5","fen":"rnbqkbnr/ppp2ppp/4p3/3P4/3P4/8/PPP2PPP/RNBQKBNR b KQkq - 0 3","path":"/?WO.>VF?F","ch":"XTMYNVqi"}}
{"t":"setComment","d":{"ch":"XTMYNVqi","path":"/?WO.>VF?FOF","text":"We have French exchange, the most exciting opening ever"}}
{"t":"toggleGlyph","d":{"id":3,"ch":"XTMYNVqi","path":"/?WO.>VF?FOF"}}
{"t":"toggleGlyph","d":{"id":15,"ch":"XTMYNVqi","path":"/?WO.>VF?FOF"}}
{"t":"anaMove","d":{"orig":"f1","dest":"d3","fen":"rnbqkbnr/ppp2ppp/8/3p4/3P4/8/PPP2PPP/RNBQKBNR w KQkq - 0 4","path":"/?WO.>VF?FOF","ch":"XTMYNVqi"}}
{"t":"anaMove","d":{"orig":"e4","dest":"e5","fen":"rnbqkbnr/ppp2ppp/4p3/3p4/3PP3/8/PPP2PPP/RNBQKBNR w KQkq - 0 3","path":"/?WO.>VF","ch":"XTMYNVqi"}}
{"t":"anaMove","d":{"orig":"c7","dest":"c5","fen":"rnbqkbnr/ppp2ppp/4p3/3pP3/3P4/8/PPP2PPP/RNBQKBNR b KQkq - 0 3","path":"/?WO.>VF?G","ch":"XTMYNVqi"}}
{"t":"setComment","d":{"ch":"XTMYNVqi","path":"/?WO.>VF?GUE","text":"French Defence: Advance Variation, another better position for Black"}}
{"t":"shapes","d":{"path":"/?WO.>VF?GUE","shapes":[{"orig":"c5","dest":"d4","brush":"green"}],"ch":"XTMYNVqi"}}
{"t":"shapes","d":{"path":"/?WO.>VF?GUE","shapes":[{"orig":"c5","dest":"d4","brush":"green"},{"orig":"h2","dest":"h4","brush":"green"}],"ch":"XTMYNVqi"}}
{"t":"anaMove","d":{"orig":"b1","dest":"c3","fen":"rnbqkbnr/ppp2ppp/4p3/3p4/3PP3/8/PPP2PPP/RNBQKBNR w KQkq - 0 3","path":"/?WO.>VF","ch":"XTMYNVqi"}}
{"t":"anaMove","d":{"orig":"d5","dest":"e4","fen":"rnbqkbnr/ppp2ppp/4p3/3p4/3PP3/2N5/PPP2PPP/R1BQKBNR b KQkq - 1 3","path":"/?WO.>VF$5","ch":"XTMYNVqi"}}
{"t":"anaMove","d":{"orig":"c3","dest":"e4","fen":"rnbqkbnr/ppp2ppp/4p3/8/3Pp3/2N5/PPP2PPP/R1BQKBNR w KQkq - 0 4","path":"/?WO.>VF$5F?","ch":"XTMYNVqi"}}
{"t":"anaMove","d":{"orig":"b8","dest":"d7","fen":"rnbqkbnr/ppp2ppp/4p3/8/3PN3/8/PPP2PPP/R1BQKBNR b KQkq - 0 4","path":"/?WO.>VF$5F?5?","ch":"XTMYNVqi"}}
{"t":"setComment","d":{"ch":"XTMYNVqi","path":"/?WO.>VF$5F?","text":"3. Nc3 is the main weapon of White, but it doesn't match for the powerful Rubinstein. White is scrwed "}}
{"t":"setComment","d":{"ch":"XTMYNVqi","path":"/?WO.>VF$5F?","text":"3. Nc3 is the main weapon of White, but it doesn't match for the powerful Rubinstein. White is screwed here"}}
{"t":"anaMove","d":{"orig":"f8","dest":"b4","fen":"rnbqkbnr/ppp2ppp/4p3/3p4/3PP3/2N5/PPP2PPP/R1BQKBNR b KQkq - 1 3","path":"/?WO.>VF$5","ch":"XTMYNVqi"}}
{"t":"promote","d":{"toMainline":false,"path":"/?WO.>VF$5`<","ch":"XTMYNVqi"}}
{"t":"promote","d":{"toMainline":true,"path":"/?WO.>VF$5F?5?\\V","ch":"XTMYNVqi"}}
""".trim

  val pgn4 =
    "1. e4 e6 2. d4 d5 3. Nc3 (3. exd5 exd5!! $15 { We have French exchange, the most exciting opening ever } 4. Bd3) (3. e5 c5 { French Defence: Advance Variation, another better position for Black } { [%cal Gc5d4,Gh2h4] }) 3... dxe4 { 3. Nc3 is the main weapon of White, but it doesn't match for the powerful Rubinstein. White is scrwed } { 3. Nc3 is the main weapon of White, but it doesn't match for the powerful Rubinstein. White is screwed here } (3... Bb4) 4. Nxe4 Nd7"

  val m5 = s"""$m4\n{"t":"clearAnnotations","d":"kMOZO15F"}"""
  val pgn5 = "1. e4 e6 2. d4 d5 3. Nc3 (3. exd5 exd5 4. Bd3) (3. e5 c5) 3... dxe4 (3... Bb4) 4. Nxe4 Nd7"

  val m6 = s"""$m4\n{"t":"clearVariations","d":"kMOZO15F"}"""
  val pgn6 =
    "1. e4 e6 2. d4 d5 3. Nc3 dxe4 { 3. Nc3 is the main weapon of White, but it doesn't match for the powerful Rubinstein. White is scrwed } { 3. Nc3 is the main weapon of White, but it doesn't match for the powerful Rubinstein. White is screwed here } 4. Nxe4 Nd7"

  val ms = List(m0, m1, m2, m3, m4, m5, m6)
  val ps = List(pgn0, pgn1, pgn2, pgn3, pgn4, pgn5, pgn6)
  val standards = ms.zip(ps)

  val m7 = """
{"t":"anaMove","d":{"orig":"c2","dest":"c4","variant":"crazyhouse","fen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR/ w KQkq - 0 1","path":"","ch":"Drb6pzLF","sticky":false}}
{"t":"anaMove","d":{"orig":"b7","dest":"b5","variant":"crazyhouse","fen":"rnbqkbnr/pppppppp/8/8/2P5/8/PP1PPPPP/RNBQKBNR/ b KQkq - 0 1","path":"-=","ch":"Drb6pzLF","sticky":false}}
{"t":"anaMove","d":{"orig":"c4","dest":"b5","variant":"crazyhouse","fen":"rnbqkbnr/p1pppppp/8/1p6/2P5/8/PP1PPPPP/RNBQKBNR/ w KQkq - 0 2","path":"-=TD","ch":"Drb6pzLF","sticky":false}}
{"t":"anaMove","d":{"orig":"c7","dest":"c6","variant":"crazyhouse","fen":"rnbqkbnr/p1pppppp/8/1P6/8/8/PP1PPPPP/RNBQKBNR/P b KQkq - 0 2","path":"-=TD=D","ch":"Drb6pzLF","sticky":false}}
{"t":"anaMove","d":{"orig":"b5","dest":"c6","variant":"crazyhouse","fen":"rnbqkbnr/p2ppppp/2p5/1P6/8/8/PP1PPPPP/RNBQKBNR/P w KQkq - 0 3","path":"-=TD=DUM","ch":"Drb6pzLF","sticky":false}}
{"t":"anaMove","d":{"orig":"b8","dest":"c6","variant":"crazyhouse","fen":"rnbqkbnr/p2ppppp/2P5/8/8/8/PP1PPPPP/RNBQKBNR/PP b KQkq - 0 3","path":"-=TD=DUMDM","ch":"Drb6pzLF","sticky":false}}
{"t":"anaDrop","d":{"role":"pawn","pos":"c7","variant":"crazyhouse","fen":"r1bqkbnr/p2ppppp/2n5/8/8/8/PP1PPPPP/RNBQKBNR/PPp w KQkq - 0 4","path":"-=TD=DUMDM\\M","ch":"Drb6pzLF","sticky":false}}
{"t":"anaMove","d":{"orig":"d8","dest":"c7","variant":"crazyhouse","fen":"r1bqkbnr/p1Pppppp/2n5/8/8/8/PP1PPPPP/RNBQKBNR/Pp b KQkq - 0 4","path":"-=TD=DUMDM\\MU\u008f","ch":"Drb6pzLF","sticky":false}}
{"t":"anaDrop","d":{"role":"pawn","pos":"b6","variant":"crazyhouse","fen":"r1b1kbnr/p1qppppp/2n5/8/8/8/PP1PPPPP/RNBQKBNR/Ppp w KQkq - 0 5","path":"-=TD=DUMDM\\MU\u008f^U","ch":"Drb6pzLF","sticky":false}}
{"t":"anaDrop","d":{"role":"pawn","pos":"d6","variant":"crazyhouse","fen":"r1b1kbnr/p1qppppp/2n5/8/8/8/PP1PPPPP/RNBQKBNR/Ppp w KQkq - 0 5","path":"-=TD=DUMDM\\MU\u008f^U","ch":"Drb6pzLF","sticky":false}}
""".trim

  val pgn7 = "1. c4 b5 2. cxb5 c6 3. bxc6 Nxc6 4. P@c7 Qxc7 5. P@b6 (5. P@d6)"

  val crazyhouses = List(m7 -> pgn7)

  val m9 = """
{"t":"anaMove","d":{"orig":"e2","dest":"e4","variant":"antichess","fen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1","path":"","ch":"BWRqIKUe","sticky":false}}
{"t":"anaMove","d":{"orig":"f7","dest":"f5","variant":"antichess","fen":"rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b - - 0 1","path":"/?","ch":"BWRqIKUe","sticky":false}}
{"t":"anaMove","d":{"orig":"e4","dest":"f5","variant":"antichess","fen":"rnbqkbnr/ppppp1pp/8/5p2/4P3/8/PPPP1PPP/RNBQKBNR w - - 0 2","path":"/?XH","ch":"BWRqIKUe","sticky":false}}
{"t":"anaMove","d":{"orig":"g8","dest":"f6","variant":"antichess","fen":"rnbqkbnr/ppppp1pp/8/5P2/8/8/PPPP1PPP/RNBQKBNR b - - 0 2","path":"/?XH?H","ch":"BWRqIKUe","sticky":false}}
{"t":"anaMove","d":{"orig":"d1","dest":"h5","variant":"antichess","fen":"rnbqkb1r/ppppp1pp/5n2/5P2/8/8/PPPP1PPP/RNBQKBNR w - - 1 3","path":"/?XH?HaP","ch":"BWRqIKUe","sticky":false}}
{"t":"anaMove","d":{"orig":"f6","dest":"h5","variant":"antichess","fen":"rnbqkb1r/ppppp1pp/5n2/5P1Q/8/8/PPPP1PPP/RNB1KBNR b - - 2 3","path":"/?XH?HaP&J","ch":"BWRqIKUe","sticky":false}}
""".trim

  val pgn9 = "1. e4 f5 2. exf5 Nf6 3. Qh5 Nxh5"

  val antichess = List(m9 -> pgn9)
  