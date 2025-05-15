package lila.opening

import chess.opening.OpeningName
import OpeningSearch.*

class OpeningSearchTest extends munit.FunSuite:

  def search(q: String) = OpeningSearch(q, 10)

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
    assertEquals(makeQuery("1.e4 e5 2.d4").numberedPgn, makeQuery("1. e4 e5 2. d4").numberedPgn)
