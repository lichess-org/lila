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
  test("split mutiple games"):
    val pgn = PgnStr(games.mkString("\n\n"))
    val multi = MultiPgn.split(pgn, max)
    assertEquals(multi.value.size, games.size)

  val taggedGames = games.tail.map(game => PgnStr(game.value.trim))

  test("split whitespace only"):
    for whitespace <- List(" ", "\t", "\r\n", " \t\r\n\r\n") do
      assertEquals(MultiPgn.split(PgnStr(whitespace), max).value, Nil)

  test("split with leading whitespace"):
    for prefix <- List(" \t\n\n", " ", "\t", "\n\n", "\t\r\n\r\n") do
      val pgn = PgnStr(prefix + taggedGames.mkString("\n\n"))
      assertEquals(MultiPgn.split(pgn, max).value, taggedGames)

  test("split with leading whitespace and game limit"):
    for size <- List(63, 64, 65) do
      val selected = List.tabulate(size)(i => taggedGames(i % taggedGames.size))
      val pgn = PgnStr(" \n\n" + selected.mkString("\n\n"))
      assertEquals(MultiPgn.split(pgn, Max(64)).value, selected.take(64))

  test("split without trimming games"):
    val game = PgnStr(games.head.value + "  \n")
    val pgn = PgnStr(" \n\n" + game.value)
    assertEquals(MultiPgn.split(pgn, max).value, List(game))
