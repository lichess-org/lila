package lila.study

import chess.Centis

import lila.tree.Node.Shapes

class CommentParserTest extends LilaTest:

  import chess.format.pgn.Comment
  import scala.language.implicitConversions
  given Conversion[String, Comment] = Comment(_)

  val C = CommentParser

  test("parse comment: empty"):
    assertEquals(C("").comment, Comment(""))

  test("parse comment: without"):
    assertEquals(C("Hello there").comment, Comment("Hello there"))

  test("parse comment: at start"):
    assertEquals(C("[%clk 10:40:33] Hello there").comment, Comment("Hello there"))

  test("parse comment: at end"):
    assertEquals(C("Hello there [%clk 10:40:33]").comment, Comment("Hello there"))

  test("parse comment: multiple"):
    assertEquals(C("Hello there [%clk 10:40:33][%clk 10:40:33]").comment, Comment("Hello there"))

  test("parse comment: new lines"):
    assertEquals(C("Hello there [%clk\n10:40:33]").comment, Comment("Hello there"))

  test("parse clock: empty"):
    assertEquals(C("").clock, None)

  test("parse clock: without"):
    assertEquals(C("Hello there").clock, None)

  test("parse clock: only"):
    assertEquals(C("[%clk 10:40:33]").clock, Some(Centis(3843300)))

  test("parse emt only seconds"):
    assertEquals(C("[%clk 00:00:33]").clock, Some(Centis(3300)))

  test("parse clock: one hour"):
    assertEquals(C("[%clk 1:40:33]").clock, Some(Centis(603300)))

  test("parse clock: at start"):
    assertEquals(C("[%clk 10:40:33] Hello there").clock, Some(Centis(3843300)))

  test("parse clock: at end"):
    assertEquals(C("Hello there [%clk 10:40:33]").clock, Some(Centis(3843300)))

  test("parse clock: in the middle"):
    assertEquals(C("Hello there [%clk 10:40:33] something else").clock, Some(Centis(3843300)))

  test("parse clock: multiple"):
    assertEquals(C("Hello there [%clk 10:40:33][%clk 10:40:33]").clock, Some(Centis(3843300)))

  test("parse clock: new lines"):
    assertEquals(C("Hello there [%clk\n10:40:33]").clock, Some(Centis(3843300)))

  test("parse clock: no seconds"):
    assertEquals(C("Hello there [%clk 2:10] something else").clock, Some(Centis(780000)))

  test("parse emt: no seconds"):
    assertEquals(C("Hello there [%emt 2:10] something else").emt, Some(Centis(780000)))

  test("parse clock: alt format"):
    assertEquals(C("Hello there [%clk 2:10.33] something else").clock, Some(Centis(783300)))

  test("parse emt: alt format"):
    assertEquals(C("Hello there [%emt 2:10.33] something else").emt, Some(Centis(783300)))

  test("parse clock: TCEC"):
    assertEquals(
      C(
        "d=29, pd=Bb7, mt=00:01:01, tl=00:37:47, s=27938 kN/s, n=1701874274, pv=e4 Bb7 exf5 exf5 d5 Qf6 Rab1 Rae8 g3 Bc8 Kg2 Rxe1 Rxe1, tb=0, R50=49, wv=0.45,"
      ).clock,
      Some(Centis(100 * 47 + 37 * 60 * 100))
    )

  test("parse clock: TCEC millis"):
    assertEquals(
      C(
        "d=31, sd=101, mt=225513, tl=3380487, s=55170, n=13264996, pv=d5 Nb4, tb=45, h=0.0, ph=0.0, wv=0.83, R50=50, Rd=-9, Rr=-1000, mb=+0+0+0+0+0,"
      ).clock,
      Some(Centis(338049))
    )

  test("parse clock: fractional second"):
    assertEquals(C("Hello there [%clk 10:40:33.55] something else").clock, Some(Centis(3843355)))

  test("parse clock: fractional second rounded to centisecond"):
    assertEquals(C("Hello there [%clk 10:40:33.556] something else").clock, Some(Centis(3843356)))

  test("parse clock: garbage 2:.:13"):
    assertEquals(C("Hello there [%clk 2:.:13] something else").clock, None)

  test("parse clock: garbage 2..:30:0"):
    assertEquals(C("Hello there [%clk 2..:30:0] something else").clock, None)

  test("parse clock: garbage :30"):
    assertEquals(C("Hello there [%clk :30] something else").clock, None)

  test("parse shapes: empty"):
    assertEquals(C(""), C.ParsedComment(Shapes(Nil), None, None, ""))

  test("parse shapes: without"):
    assertEquals(C("Hello there"), C.ParsedComment(Shapes(Nil), None, None, "Hello there"))

  test("parse shapes: at start"):
    assertMatch(C("[%csl Gb4,Yd5,Rf6] Hello there")):
      case C.ParsedComment(shapes, None, None, c) =>
        c == Comment("Hello there") && shapes.value.size == 3

  test("parse shapes: at end"):
    assertMatch(C("Hello there [%csl Gb4,Yd5,Rf6]")):
      case C.ParsedComment(shapes, None, None, c) =>
        c == Comment("Hello there") && shapes.value.size == 3

  test("parse shapes: multiple"):
    assertMatch(C("Hello there [%csl Gb4,Yd5,Rf6][%cal Ge2e4,Ye2d4,Re2g4]")):
      case C.ParsedComment(shapes, None, None, c) =>
        c == Comment("Hello there") && shapes.value.size == 6

  test("parse shapes: new lines"):
    assertMatch(C("Hello there [%csl\nGb4,Yd5,Rf6]")):
      case C.ParsedComment(shapes, None, None, _) =>
        shapes.value.size == 3

  test("parse shapes: multiple, one new line"):
    assertMatch(C("Hello there [%csl\nGb4,Yd5,Rf6][%cal Ge2e4,Ye2d4,Re2g4]")):
      case C.ParsedComment(shapes, None, None, _) => shapes.value.size == 6
    assertMatch(C("Hello there [%csl Gb4,Yd5,Rf6][%cal\nGe2e4,Ye2d4,Re2g4]")):
      case C.ParsedComment(shapes, None, None, _) => shapes.value.size == 6

  test("parse shapes: multiple mess"):
    assertMatch(C("Hello there [%csl \n\n Gb4,Yd5,Rf6][%cal\nGe2e4,Ye2d4,Re2g4]")):
      case C.ParsedComment(shapes, None, None, _) => shapes.value.size == 6

  test("multiple shapes + clock"):
    assertMatch(C("Hello there [%clk 10:40:33][%csl \n\n Gb4,Yd5,Rf6][%cal\nGe2e4,Ye2d4,Re2g4]")):
      case C.ParsedComment(shapes, clock, None, _) =>
        shapes.value.size == 6 &&
        clock == Some(Centis(3843300))

  test("multiple shapes + clock + emt"):
    assertMatch(
      C("Hello there [%emt 10:40:33][%clk 10:40:33][%csl \n\n Gb4,Yd5,Rf6][%cal\nGe2e4,Ye2d4,Re2g4]")
    ) { case C.ParsedComment(shapes, clock, emt, c) =>
      c == Comment("Hello there") &&
      shapes.value.size == 6 &&
      clock == Some(Centis(3843300)) &&
      emt == Some(Centis(3843300))
    }
