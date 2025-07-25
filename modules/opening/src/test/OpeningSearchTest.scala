package lila.opening

import chess.opening.OpeningName
import OpeningSearch.*
import chess.format.pgn.PgnMovesStr

class OpeningSearchTest extends munit.FunSuite:

  val max = 10

  def search(q: String) = OpeningSearch(q, max)

  test("tokenize"):
    assertEquals(tokenize("foo"), Set("foo"))
    assertEquals(tokenize("FoO"), Set("foo"))
    assertEquals(tokenize("FoO bar"), Set("foo", "bar"))
    assertEquals(tokenize("FòO-_ bar**-king"), Set("foo", "bar", "king"))
  test("literal"):
    assertEquals(search("Sicilian Defense").headOption.map(_.name), OpeningName("Sicilian Defense").some)
  test("partial"):
    assertEquals(search("Sicilian").headOption.map(_.name), OpeningName("Sicilian Defense").some)
  test("normalize"):
    assertEquals(search("Sìcîlián").headOption.map(_.name), OpeningName("Sicilian Defense").some)
    assertEquals(search("  --sIcIlIaN**__").headOption.map(_.name), OpeningName("Sicilian Defense").some)
  test("progressive"):
    assertEquals(search("Sicil").headOption.map(_.name), OpeningName("Sicilian Defense").some)

  test("makeQuery numbered"):
    assertEquals(makeQuery("e4 e5 d4").numberedPgn, "1. e4 e5 2. d4")
    assertEquals(makeQuery("1. e4 e5 2. d4").numberedPgn, "1. e4 e5 2. d4")

  test("makeQuery works without spaces"):
    val querySpaces = makeQuery("1. e4 e5 2. d4")
    val queryNoSpaces = makeQuery("1.e4 e5 2.d4")
    assertEquals(querySpaces.numberedPgn, queryNoSpaces.numberedPgn)
    assertEquals(querySpaces.tokens, queryNoSpaces.tokens)
    // As 1. h3 a6 isn't an opening, the following assertion only passes if "h3" is a query token, not "1.h3".
    assert(search("1.h3 a6").headOption.map(_.pgn.value).exists(_.startsWith("1. h3")))

  test("long opening names penalized if matched using name"):
    assertEquals(
      search("Advance").headOption.map(_.name),
      OpeningName("Réti Opening: Advance Variation").some
    )

  test("long opening name not penalized if matched using notation"):
    assertEquals(search("1. d4").headOption.map(_.name), OpeningName("Queen's Pawn Game").some)

  test("piece letter in notation"):
    assertEquals(search("1. d4 Nf6").headOption.map(_.pgn), PgnMovesStr("1. d4 Nf6").some)
    assert(search("Nc6").size == max)
