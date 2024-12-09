package lila.relay

import lila.relay.RelayUpdatePlan.*
import lila.study.Chapter

class RelayUpdatePlanTest extends munit.FunSuite:

  import RelayPlanUpdateFixtures.*

  def output(input: Input)(check: PartialFunction[Plan, Unit]): Unit =
    val out = RelayUpdatePlan(input)
    check.applyOrElse(out, _ => fail(s"Unexpected output: $out"))

  test("add no game to empty relay"):
    val input = Input(Nil, games.take(0))
    assertEquals(RelayUpdatePlan(input), Plan(input, None, Nil, Vector.empty, Nil))

  test("add one game to empty relay"):
    output(Input(Nil, games.take(1))):
      case Plan(_, None, Nil, append, Nil) => assertEquals(append, games.take(1))

  test("add all games to empty relay"):
    output(Input(Nil, games)):
      case Plan(_, None, Nil, append, Nil) => assertEquals(append, games)

  test("add no game to relay with initial chapter"):
    output(Input(List(initialChapter), games.take(0))):
      case Plan(_, None, Nil, Vector(), List(c)) => assertEquals(c, initialChapter)

  test("add one game to relay with initial chapter"):
    output(Input(List(initialChapter), games.take(1))):
      case Plan(_, None, update, Vector(), Nil) =>
        assertEquals(update, List(initialChapter -> games(0)))

  test("add all games to relay with initial chapter"):
    output(Input(List(initialChapter), games)):
      case Plan(_, None, update, append, Nil) =>
        assertEquals(update, List(initialChapter -> games(0)))
        assertEquals(append, games.drop(1))

  test("1 chapter, 1 game, matching tags"):
    output(Input(chapters.take(1), games.take(1))):
      case Plan(_, None, update, append, Nil) =>
        assertEquals(update, List(chapters(0) -> games(0)))
        assert(append.isEmpty)

  test("1 chapter, 1 game, not matching tags"):
    output(Input(chapters.take(1), games.drop(1).take(1))):
      case Plan(_, None, Nil, append, orphans) =>
        assertEquals(append, games.drop(1).take(1))
        assertEquals(orphans, chapters.take(1))

  test("1 chapter, 5 games, first matching tags"):
    output(Input(chapters.take(1), games)):
      case Plan(_, None, update, append, Nil) =>
        assertEquals(update, List(chapters(0) -> games(0)))
        assertEquals(append, games.drop(1))

  test("5 chapters, 5 games, matching tags"):
    output(Input(chapters, games)):
      case Plan(_, None, update, append, Nil) =>
        assertEquals(update, chapters.zip(games))
        assert(append.isEmpty)

  test("2 chapters, 2 games, reverse order"):
    val in = Input(chapters.take(2), games.take(2).reverse)
    output(in):
      case Plan(_, reorder, update, Vector(), Nil) =>
        assertEquals(update, in.chapters.reverse.zip(in.games))
        assertEquals(reorder, in.chapters.reverse.map(_.id).some)

  test("5 chapters, 5 games, reverse order"):
    val in = Input(chapters, games.reverse)
    output(in):
      case Plan(_, Some(reorder), update, Vector(), Nil) =>
        assertEquals(update, in.chapters.reverse.zip(in.games))
        assertEquals(reorder, in.chapters.reverse.map(_.id))

  test("5 chapters, 2 games, reverse order"):
    output(Input(chapters, games.take(2).reverse)):
      case Plan(_, None, update, Vector(), orphans) =>
        assertEquals(update, List(chapters(1) -> games(1), chapters(0) -> games(0)))
        assertEquals(orphans, List(chapters(2), chapters(3), chapters(4)))

  test("2 chapters, 3 games, reverse order"):
    // A B, C B A
    val in = Input(chapters.take(2), games.take(3).reverse)
    output(in):
      case Plan(_, Some(reorder), update, append, Nil) =>
        assertEquals(update, List(chapters(1) -> games(1), chapters(0) -> games(0)))
        assertEquals(reorder, in.chapters.reverse.map(_.id))
        assertEquals(append, Vector(games(2)))
