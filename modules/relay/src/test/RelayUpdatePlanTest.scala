package lila.relay

import lila.relay.RelayUpdatePlan.*
import lila.study.Chapter

class RelayUpdatePlanTest extends munit.FunSuite:

  import RelayPlanUpdateFixtures.*

  def output(input: Input)(check: PartialFunction[Output, Unit]): Unit =
    val out = RelayUpdatePlan(input).output
    check.applyOrElse(out, _ => fail(s"Unexpected output: $out"))

  test("add no game to empty relay"):
    assertEquals(RelayUpdatePlan(Input(Nil, games.take(0))).output, Output(Nil, Nil, Vector.empty))

  test("add one game to empty relay"):
    output(Input(Nil, games.take(1))):
      case Output(Nil, Nil, append) => assertEquals(append, games.take(1))

  test("add all games to empty relay"):
    output(Input(Nil, games)):
      case Output(Nil, Nil, append) => assertEquals(append, games)

  test("1 chapter, 1 game, matching tags"):
    output(Input(chapters.take(1), games.take(1))):
      case Output(Nil, update, append) =>
        assertEquals(update, List(chapters(0) -> games(0)))
        assert(append.isEmpty)

  test("1 chapter, 1 game, not matching tags"):
    output(Input(chapters.take(1), games.drop(1).take(1))):
      case Output(Nil, Nil, append) => assertEquals(append, games.drop(1).take(1))

  test("1 chapter, 5 games, first matching tags"):
    output(Input(chapters.take(1), games)):
      case Output(Nil, update, append) =>
        assertEquals(update, List(chapters(0) -> games(0)))
        assertEquals(append, games.drop(1))

  test("5 chapters, 5 games, matching tags"):
    output(Input(chapters, games)):
      case Output(Nil, update, append) =>
        assertEquals(update, chapters.zip(games))
        assert(append.isEmpty)

  test("2 chapters, 2 games, reverse order"):
    val in = Input(chapters.take(2), games.take(2).reverse)
    output(in):
      case Output(reorder, update, Vector()) =>
        assertEquals(update, in.chapters.reverse.zip(in.games))
        assertEquals(reorder, in.chapters.reverse.map(_.id))

  test("5 chapters, 5 games, reverse order"):
    val in = Input(chapters, games.reverse)
    output(in):
      case Output(reorder, update, Vector()) =>
        assertEquals(update, in.chapters.reverse.zip(in.games))
        assertEquals(reorder, in.chapters.reverse.map(_.id))

  test("5 chapters, 2 games, reverse order"):
    output(Input(chapters, games.take(2).reverse)):
      case Output(reorder, update, Vector()) =>
        assertEquals(update, List(chapters(1) -> games(1), chapters(0) -> games(0)))
        assertEquals(reorder, Nil)

  test("2 chapters, 3 games, reverse order"):
    // A B, C B A
    val in = Input(chapters.take(2), games.take(3).reverse)
    output(in):
      case Output(reorder, update, append) =>
        assertEquals(update, List(chapters(1) -> games(1), chapters(0) -> games(0)))
        assertEquals(reorder, in.chapters.reverse.map(_.id))
        assertEquals(append, Vector(games(2)))
