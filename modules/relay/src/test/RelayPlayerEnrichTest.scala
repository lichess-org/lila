package lila.relay

import chess.PlayerName
import chess.FideId
import chess.IntRating

class RelayPlayerEnrichTest extends munit.FunSuite:

  import RelayPlayerLine.Matching.*

  private def makeReplacements(txt: String): RelayPlayerLines =
    RelayPlayersTextarea(txt).parse

  private def foundFideId(id: Int): RelayPlayerLine.Matching =
    Found(RelayPlayerLine(none, none, none, FideId(id).some))

  test("name matching"):
    val r = makeReplacements("""Aqilah Husna Binti Ainul Fikri / 35805110""")
    assertEquals(r.findMatching(PlayerName("Aqilah Husna Binti Ainul Fikri")), foundFideId(35805110))
    assertEquals(r.findMatching(PlayerName("Husna Binti Ainul Fikri")), foundFideId(35805110))
    assertEquals(r.findMatching(PlayerName("Binti Ainul Fikri")), foundFideId(35805110))
    assertEquals(r.findMatching(PlayerName("Ainul Fikri")), foundFideId(35805110))
    assertEquals(r.findMatching(PlayerName("Ainul Aqilah")), foundFideId(35805110))
    assertEquals(r.findMatching(PlayerName("Ainul uuuuuuuuuuuuuuuuuh Aqilah")), NotFound)
    assertEquals(r.findMatching(PlayerName("Aqilah")), NotFound)
    assertEquals(r.findMatching(PlayerName("Fikri")), NotFound)

  test("tokenize"):
    assertEquals(RelayPlayerLine.tokenize("Aqilah Husna Binti Ainul Fikri"), "ainul aqilah binti fikri husna")
    assertEquals(RelayPlayerLine.tokenize("Manish A/L Dhanpal Rajkumar"), "al dhanpal manish rajkumar")

  test("a / in the name"):
    val r = makeReplacements("""Manish A/L Dhanpal Rajkumar / 35805110""")
    assertEquals(r.findMatching(PlayerName("Manish A/L Dhanpal Rajkumar")), foundFideId(35805110))

  test("RelayPlayersTextarea line parse"):
    assertEquals(
      RelayPlayersTextarea.parse("foo bar / 3333"),
      Some(PlayerName("foo bar") -> RelayPlayerLine(none, none, none, FideId(3333).some))
    )
    assertEquals(
      RelayPlayersTextarea.parse("foo bar/3333"),
      Some(PlayerName("foo bar") -> RelayPlayerLine(none, none, none, FideId(3333).some))
    )
    assertEquals(
      RelayPlayersTextarea.parse("YouGotLittUp / / / 1890 / Louis Litt / FID"),
      Some(
        PlayerName("YouGotLittUp") -> RelayPlayerLine(
          PlayerName("Louis Litt").some,
          IntRating(1890).some,
          none,
          none,
          "FID".some
        )
      )
    )
    assertEquals(
      RelayPlayersTextarea.parse("foo A/L bar / 3333"),
      Some(PlayerName("foo A/L bar") -> RelayPlayerLine(none, none, none, FideId(3333).some))
    )
