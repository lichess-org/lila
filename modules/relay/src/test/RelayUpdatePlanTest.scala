package lila.relay

import lila.relay.RelayUpdatePlan.*
import lila.study.{ Chapter, MultiPgn }
import chess.format.pgn.{ Tag, Tags, Parser, PgnStr }

class RelayUpdatePlanTest extends munit.FunSuite:

  import RelayUpdatePlanFixtures.*
  import RelayUpdatePlan.isSameGameBasedOnTags

  def output(input: Input)(check: PartialFunction[Plan, Unit])(using munit.Location): Unit =
    val out = RelayUpdatePlan(input)
    check.applyOrElse(out, _ => fail(s"Unexpected output: $out"))

  test("fixtures"):
    assertEquals(games.size, 5)
    assertEquals(chapters.size, 5)

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

  test("repeated pairings"):
    import repeatedPairings.{ games, chapters }
    assertEquals(games.size, 5)
    output(Input(chapters, games)):
      case Plan(_, reorder, update, append, orphans) =>
        assert(reorder.isEmpty)
        assertEquals(update, chapters.zip(games))
        assert(append.isEmpty)
        assert(orphans.isEmpty)

  test("switched boards"):
    import switchedBoards.*
    assertEquals(games.size, 3)
    output(Input(chapters, games)):
      case Plan(_, reorder, update, append, orphans) =>
        assert(reorder.isEmpty)
        assertEquals(update, chapters.zip(games))
        assert(append.isEmpty)
        assert(orphans.isEmpty)
    output(Input(chapters, switchedGames)):
      case Plan(_, reorder, update, append, orphans) =>
        assertEquals(reorder, chapters.reverse.map(_.id).some)
        assertEquals(update, chapters.reverse.zip(switchedGames))
        assert(append.isEmpty)
        assert(orphans.isEmpty)

  def mkTags(w: String, b: String) = Tags(List(Tag(_.White, w), Tag(_.Black, b)))

  test("isSameGame based on names only"):
    assert(isSameGameBasedOnTags(mkTags("a", "b"), mkTags("a", "b")), "same names")
    assert(!isSameGameBasedOnTags(mkTags("a", "b"), mkTags("a", "c")), "different names")

  test("isSameGame based on names and round"):
    assert(
      isSameGameBasedOnTags(mkTags("a", "b") + Tag(_.Round, "1.1"), mkTags("a", "b") + Tag(_.Round, "1.1")),
      "same names and round.board"
    )
    assert(
      isSameGameBasedOnTags(mkTags("a", "b") + Tag(_.Round, "1"), mkTags("a", "b") + Tag(_.Round, "1")),
      "same names and round"
    )
    assert(
      !isSameGameBasedOnTags(mkTags("a", "b") + Tag(_.Round, "1.1"), mkTags("a", "b")),
      "same names and missing round"
    )
    assert(
      !isSameGameBasedOnTags(mkTags("a", "b") + Tag(_.Round, "1"), mkTags("a", "b") + Tag(_.Round, "2")),
      "same names and different round"
    )
    assert(
      !isSameGameBasedOnTags(
        mkTags("a", "b") + Tag(_.Round, "1.1"),
        mkTags("a", "b") + Tag(_.Round, "1.2")
      ),
      "same names and different round.board"
    )
    assert(
      !isSameGameBasedOnTags(
        mkTags("a", "b") + Tag(_.Round, "1") + Tag(_.Board, 1),
        mkTags("a", "b") + Tag(_.Round, "1") + Tag(_.Board, 2)
      ),
      "same names and different board"
    )

  test("isSameGame based on some tags and first moves"):
    def mainline(moves: String) =
      if moves.isEmpty then List(lila.tree.Root.default(chess.variant.Standard))
      else RelayGame.iso.to(MultiPgn.split(PgnStr(moves), Max(1))).head.root.mainlineNodeList

    assert(sameFirstMoves(mainline("e4 e5"), mainline("e4 e5")))
    assert(sameFirstMoves(mainline("e4 e5"), mainline("e4")))
    assert(sameFirstMoves(mainline("e4 e5"), mainline("")))
    assert(sameFirstMoves(mainline("e4 d6"), mainline("e4")))
    assert(!sameFirstMoves(mainline("e3 d6"), mainline("e4")))
    assert(
      sameFirstMoves(
        mainline("e4 e5 Nf3 Nc6 Nc3 Bb4 Nd5 Nf6 Nxb4 Nxb4 c3 Nc6 Nxe5 Nxe5 d4 Ng6 Bg5"),
        mainline("e4 e5 Nf3 Nc6 Nc3 Bb4 Nd5 Nf6 Nxb4 Nxb4 c3 Nc6 Nxe5 Nxe5 d4")
      )
    )
    assert(
      sameFirstMoves(
        mainline("e4 e5 Nf3 Nc6 Nc3 Bb4 Nd5 Nf6 Nxb4 Nxb4 c3 Nc6 Nxe5 Nxe5 d4 Ng6 Bg5"),
        mainline("e4 e5 Nf3 Nc6 Nc3 Bb4 Nd5 Nf6 Nxb4 Nxb4 c3 Nc6")
      )
    )
    assert(
      !sameFirstMoves(
        mainline("e4 e5 Nf3 Nc6 Nc3 Bb4 Nd5 Nf6 Nxb4 Nxb4 c3 Nc6 Nxe5 Nxe5 d4 Ng6 Bg5"),
        mainline("e4 e5 Nf3 Nc6 Nc3 Bb4 Nd5 Nf6 Nxb4 Nxb4 c3 Nc6 Nxe5 Nxe5 d3")
      )
    )
