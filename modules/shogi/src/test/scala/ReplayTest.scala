package shogi

import shogi.format.usi._
import shogi.format._

class ReplayTest extends ShogiTest {

  "from prod" in {
    "replay" in {
      val fen = """lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1"""
      val usis =
        Usi.readList(format.usi.Fixtures.fromProd2).get

      Replay.gamesWhileValid(usis, Some(FEN(fen)), variant.FromPosition) must beLike {
        case (games, None) =>
          games.tail.size must_== format.usi.Fixtures.fromProd2.split(' ').size
        case (games, Some(err)) =>
          println(err)
          games.tail.size must_== format.usi.Fixtures.fromProd2.split(' ').size
      }
    }
  }
}
