package lila.game

import chess.format.Fen
import chess.variant.Standard
import chess.{ ByColor, Centis, Clock, Color, Game as ChessGame, Ply, Position, Rated, Status }

import lila.core.game.{ Game, Player, Source, newGame }
import lila.core.id.GamePlayerId
import lila.game.GameExt.computeMoveTimes

class ComputeMoveTimesTest extends munit.FunSuite:

  private val startFen = Fen.initial.board

  /* Builds a game with an arbitrary clock history, so that computeMoveTimes can be
   * exercised without replaying an actual game.
   * `plies` and `turn` are set independently, which is what lets us reproduce both
   * ways a game can end (see the tests below). */
  private def mkGame(
      white: Vector[Int], // centis remaining after each of white's clock records
      black: Vector[Int],
      plies: Int,
      turn: Color,
      status: Status,
      incrementSeconds: Int
  ): Game =
    val fen = Fen.Full(s"$startFen ${turn.fold("w", "b")} KQkq - 0 1")
    val chessGame = ChessGame(
      position = Position(Standard, fen.some),
      sans = Vector.empty,
      clock = Clock(Clock.LimitSeconds(600), Clock.IncrementSeconds(incrementSeconds)).some,
      ply = Ply(plies),
      startedAtPly = Ply(0)
    )
    newGame(
      chessGame,
      ByColor(c => Player(GamePlayerId("abcd"), c, aiLevel = none)),
      rated = Rated.No,
      source = Source.Api,
      pgnImport = none
    ).sloppy
      .copy(
        status = status,
        loadClockHistory = _ => ByColor(white.map(Centis(_)), black.map(Centis(_))).some
      )

  private def times(g: Game, color: Color): List[Int] =
    computeMoveTimes(g, color).get.map(_.centis)

  /* Ended by a move (mate, stalemate, autodraw): Game.finish sees a game that is
   * already finished, so it records no extra clock entry, and clocksRecorded == playedPlies.
   * The move that ended the game got no increment, and it belongs to !turnColor. */
  test("ended by a move: only the mating side loses the last increment"):
    val g = mkGame(
      white = Vector(59700, 59100, 58200),
      black = Vector(59500, 58900),
      plies = 5,
      turn = Color.Black, // black is mated, so black is still "to move"
      status = Status.Mate,
      incrementSeconds = 3
    )
    // 600 + inc, then 900 with no inc because it is the mating move
    assertEquals(times(g, Color.White), List(0, 900, 900))
    // black's last move was answered, so it kept its increment
    assertEquals(times(g, Color.Black), List(0, 900))

  /* Ended by an async event (resign, flag, draw agreement): Game.finish records one
   * extra clock entry for turnColor, so clocksRecorded == playedPlies + 1.
   * That last entry is time spent thinking before resigning, not a move, so it must
   * not receive an increment. */
  test("ended by an async event: the resigning side's last entry gets no increment"):
    val g = mkGame(
      white = Vector(59700, 59100, 58200, 55000), // last entry: thought, then resigned
      black = Vector(59500, 58900, 58000),
      plies = 6,
      turn = Color.White, // white resigned on its own turn
      status = Status.Resign,
      incrementSeconds = 3
    )
    // 3200, not 3500: no move was played, so no increment was awarded
    assertEquals(times(g, Color.White), List(0, 900, 1200, 3200))
    assertEquals(times(g, Color.Black), List(0, 900, 1200))

  /* Regression guard for the change proposed in #16074, which suggested replacing
   *   finished && (playedPlies >= clocksRecorded) == (color != turnColor)
   * with
   *   finished && (playedPlies >= clocksRecorded) && (color != turnColor)
   *
   * The `==` is deliberate: it is an XNOR that covers both ways a game can end.
   * With `&&`, noLastInc is false whenever clocksRecorded > playedPlies, so the
   * resigning player's final thinking time is credited an increment it never got.
   * The two spellings only diverge here, which is why this case is pinned down. */
  test("#16074: async ending is where `==` and `&&` disagree"):
    val g = mkGame(
      white = Vector(59700, 59100, 58200, 55000),
      black = Vector(59500, 58900, 58000),
      plies = 6,
      turn = Color.White,
      status = Status.Resign,
      incrementSeconds = 3
    )
    val last = times(g, Color.White).last
    assertEquals(last, 3200, "the `&&` spelling would return 3500 here")

  test("unfinished game: every move keeps its increment"):
    val g = mkGame(
      white = Vector(59700, 59100, 58200),
      black = Vector(59500, 58900),
      plies = 5,
      turn = Color.Black,
      status = Status.Started,
      incrementSeconds = 3
    )
    assertEquals(times(g, Color.White), List(0, 900, 1200))
    assertEquals(times(g, Color.Black), List(0, 900))

  test("no increment: move times are plain clock differences"):
    val g = mkGame(
      white = Vector(59700, 59100, 58200),
      black = Vector(59500, 58900),
      plies = 5,
      turn = Color.Black,
      status = Status.Mate,
      incrementSeconds = 0
    )
    assertEquals(times(g, Color.White), List(0, 600, 900))
    assertEquals(times(g, Color.Black), List(0, 600))

  /* Some old games (see #5543) have one fewer clock entry than plies played, so
   * clocksRecorded < playedPlies for a game that ended by a move. computeMoveTimes
   * should still return one entry per recorded clock rather than fail. */
  test("truncated historical clock history is handled without failing"):
    val g = mkGame(
      white = Vector(59700, 59100),
      black = Vector(59500, 58900),
      plies = 5,
      turn = Color.Black,
      status = Status.Mate,
      incrementSeconds = 3
    )
    assertEquals(times(g, Color.White).size, 2)
    assertEquals(times(g, Color.Black).size, 2)

  test("clock differences never go negative"):
    val g = mkGame(
      white = Vector(59100, 59700), // clock went up: moretime was granted
      black = Vector(59500, 58900),
      plies = 5,
      turn = Color.Black,
      status = Status.Mate,
      incrementSeconds = 0
    )
    assertEquals(times(g, Color.White), List(0, 0))
