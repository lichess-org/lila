package shogi

class ReplayTest extends ShogiTest {

  "from prod" in {
    "replay" in {
      val fen   = """lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1"""
      val moves = """Pc4 Pb6 Sd2 Pg6 Sc3 Sd8 Ph4 Sf8""".split(' ').toList
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
