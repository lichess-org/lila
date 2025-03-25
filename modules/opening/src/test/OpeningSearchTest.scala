package lila.opening

import chess.opening.OpeningName

class OpeningSearchTest extends munit.FunSuite:

  def search(q: String) = OpeningSearch(q, 10)

  test("tokenize"):
    assertEquals(OpeningSearch.tokenize("foo"), Set("foo"))
    assertEquals(OpeningSearch.tokenize("FoO"), Set("foo"))
    assertEquals(OpeningSearch.tokenize("FoO bar"), Set("foo", "bar"))
    assertEquals(OpeningSearch.tokenize("FòO-_ bar**-king"), Set("foo", "bar", "king"))
  test("literal"):
    assertEquals(search("Sicilian Defense").headOption.map(_.name), OpeningName("Sicilian Defense").some)
  test("partial"):
    assertEquals(search("Sicilian").headOption.map(_.name), OpeningName("Sicilian Defense").some)
  test("normalize"):
    assertEquals(search("Sìcîlián").headOption.map(_.name), OpeningName("Sicilian Defense").some)
    assertEquals(search("  --sIcIlIaN**__").headOption.map(_.name), OpeningName("Sicilian Defense").some)
  test("progressive"):
    assertEquals(search("Sicil").headOption.map(_.name), OpeningName("Sicilian Defense").some)
