package lila.study

import chess.{ ErrorStr, Ply }
import chess.format.pgn.{ Tags, PgnStr }

import lila.common.LightUser
import lila.tree.{ Root, Branch, Branches }
import lila.tree.Node.{ Comment, Comments, Shapes }

import scala.language.implicitConversions

class PgnImportTest extends lila.common.LilaTest:

  import PgnImport.*

  given Conversion[String, PgnStr] = PgnStr(_)
  given Conversion[PgnStr, String] = _.value

  val pgn = """
  {This move:} 1.e4! {, was considered by R.J.Fischer as "best by test"}
    ( {This other move:} {looks pretty} 1.d4?! {not.} )
    ( ;Neither does :
      ;this or that
      {or whatever}
      1.b4?! {this one} ) 1... e5 2 c4
  """

  val user = LightUser(UserId("lichess"), UserName("Annotator"), None, false)

  test("import pgn"):
    PgnImport(pgn, List(user)).assertRight: parsed =>
      assertEquals(parsed.tags, Tags.empty)
      assertEquals(parsed.root.children.nodes.size, 3)
      assertEquals(parsed.root.ply, Ply.initial)

  test("import a simple pgn"):
    PgnImport("1.d4 d5 2.e4 e5", List(user)).assertRight: parsed =>
      assertEquals(parsed.tags, Tags.empty)
      assertEquals(parsed.root.children.nodes.size, 1)
      assertEquals(parsed.root.ply, Ply.initial)

  test("import a simple pgn with a clock comment"):
    val x = PgnImport("1.d4 {[%clk 1:59:59]}", Nil).toOption.get
    assert(x.root.mainlineNodeList(1).clock.isDefined)

  test("import a broadcast pgn"):
    val x = PgnImport(
      """[Event "Norway Chess"]
[Site "-"]
[Date "2023.05.31"]
[Round "3"]
[WhiteTitle "GM"] [White "Carlsen, Magnus"]
[Black "So, Wesley"]
[Result "*"]
[Board "1"]
[WhiteClock "01:03:52"]
[WhiteElo "2853"]
[WhiteTitle "GM"]
[WhiteCountry "NOR"]
[WhiteFideId "1503014"]
[BlackClock "01:09:48"]
[BlackElo "2760"]
[BlackTitle "GM"]
[BlackCountry "USA"]
[BlackFideId "5202213"]

1. e4 {[%clk 1:59:59]} e5 {[%clk 1:59:40]} 2. Nf3 {[%clk 1:59:51]} Nc6 {[%clk
1:59:07]} 3. Bb5 {[%clk 1:59:44]} Nf6 {[%clk 1:59:01]} 4. d3 {[%clk 1:59:36]} Bc5
{[%clk 1:58:57]} 5. Bxc6 {[%clk 1:59:30]} dxc6 {[%clk 1:58:53]} 6. O-O {[%clk
1:59:07]} Nd7 {[%clk 1:57:22]} 7. h3 {[%clk 1:55:18]} O-O {[%clk 1:55:11]} 8. Nc3
{[%clk 1:54:21]} a5 {[%clk 1:52:37]} 9. a4 {[%clk 1:53:36]} f6 {[%clk 1:49:06]}
10. Qe2 {[%clk 1:45:14]} Re8 {[%clk 1:41:33]} 11. Be3 {[%clk 1:44:27]} Bd6 {[%clk
1:36:47]} 12. Nd2 {[%clk 1:42:23]} Nf8 {[%clk 1:36:24]} 13. f3 {[%clk 1:32:47]}
Ng6 {[%clk 1:17:22]} 14. Qf2 {[%clk 1:32:39]} Be6 {[%clk 1:16:24]} 15. Ne2 {[%clk
1:31:54]} Qd7 {[%clk 1:16:14]} 16. b3 {[%clk 1:27:21]} Bb4 {[%clk 1:13:57]} 17.
Rad1 {[%clk 1:24:50]} b6 {[%clk 1:09:49]} 18. g4 {[%clk 1:03:52]} *""",
      Nil
    ).toOption.get
    assert(x.root.mainlineNodeList(1).clock contains chess.Centis(719900))
    assert(x.root.mainlineNodeList(2).clock contains chess.Centis(718000))
