package lila.study

import chess.format.pgn.PgnStr

class MultiPgnTest extends munit.FunSuite:

  val max = Max(100)

  test("split empty string"):
    val pgn = PgnStr("")
    val multi = MultiPgn.split(pgn, max)
    assertEquals(multi.value, Nil)

  test("split one game"):
    for
      pgn <- PgnFixtures.all
      multi = MultiPgn.split(pgn, max)
    yield assertEquals(multi.value.size, 1)

  val games = List(PgnFixtures.pgn3, PgnFixtures.pgn4, PgnFixtures.pgn5, PgnFixtures.pgn6, PgnFixtures.pgn7)
    .map(PgnStr(_))
  test("split mutitple games"):
    val pgn = PgnStr(games.mkString("\n\n"))
    val multi = MultiPgn.split(pgn, max)
    assertEquals(multi.value.size, games.size)
