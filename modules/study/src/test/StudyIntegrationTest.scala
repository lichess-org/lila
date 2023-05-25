package lila.study

import cats.syntax.all.*

import chess.White
import chess.variant.Standard
import chess.format.UciPath
import chess.format.pgn.Tags

import lila.socket.AnaMove
import java.time.Instant

import lila.tree.{ Branch, Root }
import play.api.libs.json.Json
import play.api.libs.json.JsObject

class StudyIntegrationTest extends munit.FunSuite:

  val studyId      = StudyId("studyId")
  val chapterId    = StudyChapterId("chapterId")
  val userId       = UserId("nt9")
  val studyInstant = Instant.ofEpochSecond(1685031726L)

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

  // combined of StudySocket.moveOrDrop & StudyApi.addNode
  def addNode(chapter: Chapter, move: AnaMove): Chapter =
    val rawNode = move.branch.getOrElse(throw Exception("no branch"))
    chapter.addNode(rawNode.withoutChildren, move.path, None).get

  import Helpers.*
  test("moves"):
    Fixtures.all.foreach: (str, expected) =>
      val output = str.toMoves.foldLeft(chapter): (chapter, move) =>
        addNode(chapter, move)
      assertEquals(rootToPgn(output.root).value, expected)

  extension (s: String)
    def toMoves: List[AnaMove] =
      s.linesIterator.toList
        .map(Json.parse(_).asInstanceOf[JsObject])
        .traverse(AnaMove.parse)
        .get

  object Fixtures:
    val m0   = ""
    val pgn1 = ""
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
    val pgn2 = "1. d4 d5 2. c4 e6 3. Nc3 Nf6 4. cxd5 exd5 5. Bg5 Be7"
    val ms   = List(m0, m1)
    val ps   = List(pgn1, pgn2)
    val all  = ms.zip(ps)
