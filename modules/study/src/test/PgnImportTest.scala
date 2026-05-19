package lila.study

import chess.Ply
import chess.format.pgn.{ PgnStr, Tags, Comment }

import scala.language.implicitConversions

import lila.core.LightUser
import lila.tree.Clock

class PgnImportTest extends LilaTest:

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

  val user = LightUser.fallback(UserName("Annotator"))

  test("import pgn"):
    StudyPgnImport
      .result(pgn, List(user))
      .assertRight: parsed =>
        assertEquals(parsed.tags, Tags.empty)
        assertEquals(parsed.root.children.toList.size, 3)
        assertEquals(parsed.root.ply, Ply.initial)

  test("import a simple pgn"):
    StudyPgnImport
      .result("1.d4 d5 2.e4 e5", List(user))
      .assertRight: parsed =>
        assertEquals(parsed.tags, Tags.empty)
        assertEquals(parsed.root.children.toList.size, 1)
        assertEquals(parsed.root.ply, Ply.initial)

  test("comment ordering"):
    StudyPgnImport
      .result("{test 1 } {test 2} 1.d4 {test 3} { test 4}", Nil)
      .assertRight: parsed =>
        val rootComments = parsed.root.comments.value.map(_.text)
        assertEquals(rootComments, Comment.from(List("test 1", "test 2")))
        val firstMoveComments = parsed.root.mainlineNodeList(1).comments.value.map(_.text)
        assertEquals(firstMoveComments, Comment.from(List("test 3", "test 4")))

  test("import a simple pgn with a clock comment"):
    val x = StudyPgnImport.result("1.d4 {[%clk 1:59:59]}", Nil).toOption.get
    assert(x.root.mainlineNodeList(1).clock.isDefined)

  test("import a simple pgn with a clock and emt"):
    val x = StudyPgnImport.result("1.d4 {[%clk 1:59:59]} d5 {[%emt 00:00:12]}", Nil).toOption.get
    assert(x.root.mainlineNodeList(2).clock.isEmpty)

  test("import pgn with a clock and emt"):
    val x = StudyPgnImport
      .result(
        "1.d4 {[%clk 1:59:59]} d5 {[%clk 1:59:50]} 2.c4 {[%emt 00:00:12]} Nf6 {[%emt 00:00:13]}",
        Nil
      )
      .toOption
      .get
    assertEquals(x.root.mainlineNodeList(3).clock.get, Clock(chess.Centis(718700), false.some))
    assertEquals(x.root.mainlineNodeList(4).clock.get, Clock(chess.Centis(717700), false.some))

  test("import a broadcast pgn"):
    val x = StudyPgnImport
      .result(
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
      )
      .toOption
      .get
    assert(x.root.mainlineNodeList(1).clock.contains(Clock(chess.Centis(719900), true.some)))
    assert(x.root.mainlineNodeList(2).clock.contains(Clock(chess.Centis(718000), true.some)))

  test("import a broadcast pgn with missing clock"):
    val x = StudyPgnImport
      .result(
        """
        1. d4 {[%clk 01:59:00]} {[%emt 00:00:58]} d5 {[%clk 01:59:50]} {[%emt
        00:00:11]} 2. c4 {[%clk 01:58:54]} {[%emt 00:00:06]} e6 {[%clk 01:59:21]}
        {[%emt 00:00:30]} 3. Nf3 {[%clk 01:58:28]} {[%emt 00:00:26]} Nf6 {[%clk
        01:59:06]} {[%emt 00:00:16]} 4. g3 {[%emt 00:00:43]} Bb4+ {[%clk 01:57:33]}
        {[%emt 00:00:59]} 5. Nc3 {[%emt 00:04:45]} dxc4 {[%clk 01:50:41]} {[%emt
        00:02:21]} 6. Bg2 {[%clk 01:57:37]} {[%emt 00:00:28]} O-O {[%clk 01:50:03]}
        {[%emt 00:00:38]} 7. O-O {[%clk 01:57:24]} {[%emt 00:00:12]} Nc6 {[%clk
        01:48:59]} {[%emt 00:01:05]} 8. a3 {[%emt 00:02:23]} Be7 {[%emt 00:00:19]} 9.
        e4 {[%emt 00:11:30]} a6 {[%emt 00:00:58]} 10. Be3 {[%emt 00:05:04]} b5 {[%emt
        00:00:42]} 11. Qe2 {[%emt 00:07:06]} Bb7 {[%emt 00:01:03]} 12. Rad1 {[%emt
        00:08:05]} Na5 {[%emt 00:00:48]} 13. d5 {[%clk 01:51:56]} {[%emt 00:05:37]}
        exd5 {[%emt 00:05:37]} 14. e5 {[%emt 00:03:41]} Ne8 {[%emt 00:00:44]}
        15. e6 {[%emt 00:26:35]} f5 {[%emt 00:00:48]} 16. Ne5 {[%clk 01:50:10]}
        {[%emt 00:00:11]} Nf6 {[%emt 00:38:31]} 17. Qc2 {[%emt 00:05:02]} c6
        {[%emt 00:05:03]} 18. Qxf5 {[%clk 00:54:54]} {[%emt 00:05:49]} Qe8 {[%emt 00:05:02]}
        19. Nf7 {[%emt 00:14:50]} *
      """,
        Nil
      )
      .toOption
      .get
    assertEquals(x.root.mainlineNodeList.size, 38)
    x.root.mainlineNodeList
      .drop(1) // skip the root
      .foreach: node =>
        assert(node.clock.isDefined)

  import lila.tree.{ Node, Branch }
  extension (node: Node)
    def next: Branch = node.children.first.get
    def nextSans: List[String] = node.children.toList.map(_.move.san.value)

  test("merge duplicated children from engine analysis variations (issue #20342)"):
    val bugPgn = """
    [FEN "8/8/4kpp1/3p1b2/p6P/2B5/6P1/7K w - - 1 47"]

    47. Kg1 Bh3 48. gxh3 Kf5 
        ( 48... Kf5 49. Kf2 Ke4 50. h5 gxh5 )
        ( 48... f5 49. Kf2 Kd6 50. Ke3 Kc5 )
        ( 48... g5 49. Kf2 gxh4 50. Ke3 Kf5 )
    49. Kf2 Ke4 50. Bxf6 d4 51. Be7 Kd3
    """

    StudyPgnImport
      .result(bugPgn, Nil)
      .assertRight: parsed =>
        val gxh3 = parsed.root.next.next.next

        assertEquals(gxh3.move.san.value, "gxh3")

        assertEquals(gxh3.nextSans, List("Kf5", "f5", "g5"))

        val pgnDump = Helpers.rootToPgn(parsed.root)
        val pgnExpected =
          """47. Kg1 Bh3 48. gxh3 Kf5 
            |  (48... f5 49. Kf2 Kd6 50. Ke3 Kc5) 
            |  (48... g5 49. Kf2 gxh4 50. Ke3 Kf5) 
            |49. Kf2 Ke4 50. Bxf6 
            |  (50. h5 gxh5) 
            |50... d4 51. Be7 Kd3
            |""".stripMargin.replaceAll("\n", "").replaceAll("\\s+", " ")
        assertEquals(pgnDump.value, pgnExpected)

  test("merge multiple duplicate variations with same first move"):
    val multiDupPgn = """
    1. e4 e5 2. Nf3 Nc6
        ( 2... Nc6 3. Bb5 a6 )
        ( 2... Nc6 3. Bc4 Nf6 )
        ( 2... d6 3. d4 exd4 )
    3. d4 exd4
    """

    StudyPgnImport
      .result(multiDupPgn, Nil)
      .assertRight: parsed =>
        val e4 = parsed.root.next
        val e5 = e4.next
        val nf3 = e5.next
        val nc6 = nf3.next

        assertEquals(nc6.move.san.value, "Nc6")
        assertEquals(nf3.nextSans, List("Nc6", "d6"))
        assertEquals(nc6.nextSans, List("d4", "Bb5", "Bc4"))
        val expectedPgn =
          """1. e4 e5 2. Nf3 Nc6 (2... d6 3. d4 exd4) 3. d4 (3. Bb5 a6) (3. Bc4 Nf6) 3... exd4"""
        assertEquals(Helpers.rootToPgn(parsed.root).value, expectedPgn)

  test("merge duplicate from code comment pattern: same move appearing three times"):
    val threeDupPgn = """1. d4 ( 1. d4 Nf6 ) ( 1. d4 d5 ) 1... e5"""
    StudyPgnImport
      .result(threeDupPgn, Nil)
      .assertRight: parsed =>
        assertEquals(parsed.root.nextSans, List("d4"))
        val d4 = parsed.root.next
        assertEquals(d4.nextSans, List("e5", "Nf6", "d5"))
        val expectedPgn = "1. d4 e5 (1... Nf6) (1... d5)"
        assertEquals(Helpers.rootToPgn(parsed.root).value, expectedPgn)

  test("merge sibling variations that duplicate each other but NOT the mainline"):
    val siblingDupPgn = """
    1. e4 e5 2. Nf3 Nc6 (2... d6 3. d4) (2... d6 3. Bc4)
    """
    StudyPgnImport
      .result(siblingDupPgn, Nil)
      .assertRight: parsed =>
        val e4 = parsed.root.next
        val nf3 = e4.next.next
        assertEquals(nf3.nextSans, List("Nc6", "d6"))
        val d6 = nf3.children.variations.head
        assertEquals(d6.nextSans, List("d4", "Bc4"))
        val expectedPgn = "1. e4 e5 2. Nf3 Nc6 (2... d6 3. d4 (3. Bc4))"
        assertEquals(Helpers.rootToPgn(parsed.root).value, expectedPgn)

  test("merge duplicated children: sibling clone (Philidor variations)"):
    val pgn = "1. e4 e5 2. Nf3 Nc6 (2... d6 3. d4) (2... d6 3. Bc4)"
    StudyPgnImport
      .result(pgn, Nil)
      .assertRight: parsed =>
        val expected = "1. e4 e5 2. Nf3 Nc6 (2... d6 3. d4 (3. Bc4))"
        assertEquals(Helpers.rootToPgn(parsed.root).value, expected)

  test("merge duplicated children: deep fork"):
    val pgn = "1. e4 e5 2. Nf3 Nc6 (2... Nc6 3. Bc4 Bc5 4. c3) 3. Bc4 Bc5 4. d3"
    StudyPgnImport
      .result(pgn, Nil)
      .assertRight: parsed =>
        val expected = "1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. d3 (4. c3)"
        assertEquals(Helpers.rootToPgn(parsed.root).value, expected)

  test("merge duplicated children: nested matryoshka (Philidor Exchange)"):
    val pgn = "1. e4 e5 2. Nf3 Nc6 (2... d6 3. d4 exd4 (3... exd4 4. Nxd4) (3... exd4 4. Qxd4))"
    StudyPgnImport
      .result(pgn, Nil)
      .assertRight: parsed =>
        val expected = "1. e4 e5 2. Nf3 Nc6 (2... d6 3. d4 exd4 4. Nxd4 (4. Qxd4))"
        assertEquals(Helpers.rootToPgn(parsed.root).value, expected)

  test("merge duplicated children: triplicate branches"):
    val pgn = "1. e4 e5 2. Nf3 Nc6 (2... Nc6 3. Bc4) (2... Nc6 3. d4) 3. Bb5"
    StudyPgnImport
      .result(pgn, Nil)
      .assertRight: parsed =>
        val expected = "1. e4 e5 2. Nf3 Nc6 3. Bb5 (3. Bc4) (3. d4)"
        assertEquals(Helpers.rootToPgn(parsed.root).value, expected)
