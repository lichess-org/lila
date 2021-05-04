package chess

class ReplayTest extends ChessTest {

  "from prod" in {
    "replay from position close chess" in {
      val fen   = """8/rnbqkbnr/pppppppp/8/8/PPPPPPPP/RNBQKBNR/8 w - - 0 1"""
      val moves = """d4 d5 Nf4 Nf5 g4 g5 gxf5 exf5""".split(' ').toList
      Replay.gameMoveWhileValid(moves, fen, variant.FromPosition) must beLike {
        case (_, games, None) =>
          games.size must_== 8
        case (init, games, Some(err)) =>
          println(err)
          println(init)
          games.size must_== 8
      }
    }
  }
}
