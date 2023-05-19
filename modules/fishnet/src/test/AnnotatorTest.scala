package lila.fishnet

import JsonApi.*
import play.api.libs.json.Reads
import play.api.libs.json.*
import lila.game.PgnImport
import lila.fishnet.JsonApi.Request.Evaluation
import chess.format.pgn.{
  Move,
  Dumper,
  SanStr,
  Pgn,
  PgnStr,
  Initial,
  Tag,
  Tags,
  Parser,
  PgnTree,
  ParsedPgnTree,
  ParsedPgn,
  PgnNodeData
}
import chess.{ Node, Ply, MoveOrDrop, Situation }
import chess.MoveOrDrop.*
import lila.analyse.{ Analysis, Annotator }
import lila.common.config.NetDomain
import lila.game.PgnDump
import lila.common.config.BaseUrl
import chess.variant.Standard
import lila.fishnet.Work.Analysis
import java.time.Instant
import chess.Clock

final class AnnotatorTest extends munit.FunSuite:

  test("annotated game with fishnet input"):
    TestFixtures.testCases.foreach { tc =>
      val (output, expected) = tc.test
      assertEquals(output, expected)
    }
