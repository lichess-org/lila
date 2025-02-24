package lila.puzzle

import chess.format.{ Fen, Uci }

import lila.rating.Glicko
import lila.tree
import lila.tree.NewTree

class JsonViewTest extends munit.FunSuite:

  val ps: List[Puzzle] = List(
    "q3k1nr/1pp1nQpp/3p4/1P2p3/4P3/B1PP1b2/B5PP/5K2 b k - 0 17" -> "e8d7 a2e6 d7d8 f7f8",
    "r3r1k1/p4ppp/2p2n2/1p6/3P1qb1/2NQR3/PPB2PP1/R1B3K1 w - - 5 18" -> "e3g3 e8e1 g1h2 e1c1 a1c1 f4h6 h2g1 h6c1",
    "Q1b2r1k/p2np2p/5bp1/q7/5P2/4B3/PPP3PP/2KR1B1R w - - 1 17"  -> "d1d7 a5e1 d7d1 e1e3 c1b1 e3b6",
    "1k1r4/pp3pp1/2p1p3/4b3/P3n1P1/8/KPP2PN1/3rBR1R b - - 2 31" -> "b8c7 e1a5 b7b6 f1d1"
  ).map(makePuzzle.tupled)

  def makePuzzle(fen: String, ucis: String): Puzzle =
    Puzzle(
      id = PuzzleId("12345"),
      gameId = GameId("12345678"),
      fen = Fen.Full(fen),
      line = NonEmptyList.fromListUnsafe(ucis.split(' ').toList.map(Uci.Move.apply)).sequence.get,
      glicko = Glicko.default,
      plays = 1,
      vote = 1f,
      themes = Set.empty
    )

  test("compare tree"):
    ps.foreach: p =>
      val newTree = JsonView.makeTree(p).get
      val branch  = makeBranch(p).get
      assertEquals(newTree, NewTree.fromBranch(branch, Nil))

  test("compare json"):
    ps.foreach: puzzle =>
      val newTree = NewTree.defaultNodeJsonWriter.writes(JsonView.makeTree(puzzle).get)
      val branch  = tree.Node.defaultNodeJsonWriter.writes(makeBranch(puzzle).get)
      assertEquals(newTree, branch)

  def makeBranch(puzzle: Puzzle): Option[tree.Branch] =
    import chess.format.*
    val init = chess.Game(none, puzzle.fenAfterInitialMove.some).withTurns(puzzle.initialPly + 1)
    val (_, branchList) = puzzle.line.tail.foldLeft[(chess.Game, List[tree.Branch])]((init, Nil)):
      case ((prev, branches), uci) =>
        val (game, move) =
          prev(uci.orig, uci.dest, uci.promotion)
            .fold(err => sys.error(s"puzzle ${puzzle.id} $err"), identity)
        val branch = tree.Branch(
          id = UciCharPair(move.toUci),
          ply = game.ply,
          move = Uci.WithSan(move.toUci, game.sans.last),
          fen = chess.format.Fen.write(game),
          check = game.situation.check,
          crazyData = none
        )
        (game, branch :: branches)
    branchList.reduceOption((child, branch) => branch.addChild(child))
