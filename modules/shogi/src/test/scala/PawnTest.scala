package shogi

import Pos._

class PawnTest extends ShogiTest {

  "a sente pawn" should {

    "move towards rank by 1 square" in {
      makeBoard(
        SQ9F -> Sente.pawn
      ) destsFrom SQ9F must bePoss(SQ9E)
    }

    "not move to positions that are occupied by the same color" in {
      makeBoard(
        SQ9F -> Sente.pawn,
        SQ9E -> Sente.pawn
      ) destsFrom SQ9F must bePoss()
    }

    "capture forward" in {
      makeBoard(
        SQ6F -> Sente.pawn,
        SQ6E -> Gote.pawn
      ) destsFrom SQ6F must bePoss(SQ6E)
    }
  }

  "a gote pawn" should {

    "move towards rank by 1 square" in {
      makeBoard(
        SQ9E -> Gote.pawn
      ) destsFrom SQ9E must bePoss(SQ9F)
    }

    "not move to positions that are occupied by the same color" in {
      makeBoard(
        SQ9F -> Gote.pawn,
        SQ9G -> Gote.pawn
      ) destsFrom SQ9F must bePoss()
    }

    "capture forward" in {
      makeBoard(
        SQ6E -> Gote.pawn,
        SQ6F -> Sente.pawn
      ) destsFrom SQ6E must bePoss(SQ6F)
    }
  }
}
