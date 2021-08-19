package shogi

import Pos._
import format.Uci

class PawnTest extends ShogiTest {

  "a sente pawn" should {

    "move towards rank by 1 square" in {
      makeBoard(
        A4 -> Sente.pawn
      ) destsFrom A4 must bePoss(A5)
    }

    "not move to positions that are occupied by the same color" in {
      makeBoard(
        A4 -> Sente.pawn,
        A5 -> Sente.pawn
      ) destsFrom A4 must bePoss()
    }

    "capture forward" in {
      makeBoard(
        D4 -> Sente.pawn,
        D5 -> Gote.pawn
      ) destsFrom D4 must bePoss(D5)
    }
  }

  "a gote pawn" should {

    "move towards rank by 1 square" in {
      makeBoard(
        A5 -> Gote.pawn
      ) destsFrom A5 must bePoss(A4)
    }

    "not move to positions that are occupied by the same color" in {
      makeBoard(
        A4 -> Gote.pawn,
        A3 -> Gote.pawn
      ) destsFrom A4 must bePoss()
    }

    "capture forward" in {
      makeBoard(
        D5 -> Gote.pawn,
        D4 -> Sente.pawn
      ) destsFrom D5 must bePoss(D4)
    }
  }
}
