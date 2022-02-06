package shogi

import Pos._

class BoardTest extends ShogiTest {

  val board = makeSituation.board

  "a board" should {

    "position pieces correctly" in {
      board.pieces must havePairs(
        SQ9I -> (Sente - Lance),
        SQ8I -> (Sente - Knight),
        SQ7I -> (Sente - Silver),
        SQ6I -> (Sente - Gold),
        SQ5I -> (Sente - King),
        SQ4I -> (Sente - Gold),
        SQ3I -> (Sente - Silver),
        SQ2I -> (Sente - Knight),
        SQ1I -> (Sente - Lance),
        SQ8H -> (Sente - Bishop),
        SQ2H -> (Sente - Rook),
        SQ9G -> (Sente - Pawn),
        SQ8G -> (Sente - Pawn),
        SQ7G -> (Sente - Pawn),
        SQ6G -> (Sente - Pawn),
        SQ5G -> (Sente - Pawn),
        SQ4G -> (Sente - Pawn),
        SQ3G -> (Sente - Pawn),
        SQ2G -> (Sente - Pawn),
        SQ1G -> (Sente - Pawn),
        SQ9C -> (Gote - Pawn),
        SQ8C -> (Gote - Pawn),
        SQ7C -> (Gote - Pawn),
        SQ6C -> (Gote - Pawn),
        SQ5C -> (Gote - Pawn),
        SQ4C -> (Gote - Pawn),
        SQ3C -> (Gote - Pawn),
        SQ2C -> (Gote - Pawn),
        SQ1C -> (Gote - Pawn),
        SQ8B -> (Gote - Rook),
        SQ2B -> (Gote - Bishop),
        SQ9A -> (Gote - Lance),
        SQ8A -> (Gote - Knight),
        SQ7A -> (Gote - Silver),
        SQ6A -> (Gote - Gold),
        SQ5A -> (Gote - King),
        SQ4A -> (Gote - Gold),
        SQ3A -> (Gote - Silver),
        SQ2A -> (Gote - Knight),
        SQ1A -> (Gote - Lance)
      )
    }

    "have pieces by default" in {
      board.pieces must not beEmpty
    }

    "allow a piece to be placed" in {
      board.place(Sente - Rook, SQ5E) must beSome.like { case b =>
        b(SQ5E) mustEqual Some(Sente - Rook)
      }
    }

    "allow a piece to be taken" in {
      board take SQ9I must beSome.like { case b =>
        b(SQ9I) must beNone
      }
    }

    "allow a piece to move" in {
      board.move(SQ5G, SQ5F) must beSome.like { case b =>
        b(SQ5F) mustEqual Some(Sente - Pawn)
      }
    }

    "not allow an empty position to move" in {
      board.move(SQ5E, SQ5D) must beNone
    }

    "allow a piece to move to an occupied position" in {
      board.move(SQ9I, SQ9G) must beSome.like { case b =>
        b(SQ9G) mustEqual Some(Sente - Lance)
      }
    }

    "allow a pawn to be promoted" in {
      makeEmptySituation.board
        .place(Gote.pawn, SQ9F) flatMap (_.promote(SQ9F, SQ9G, _ => Some(Tokin))) must beSome.like { case b =>
        b(SQ9G) must beSome(Gote.tokin)
      }
    }

    "allow chaining actions" in {
      makeEmptySituation.board.seq(
        _.place(Sente - Pawn, SQ9H),
        _.place(Sente - Pawn, SQ9G),
        _.move(SQ9H, SQ9F)
      ) must beSome.like { case b =>
        b(SQ9F) mustEqual Some(Sente - Pawn)
      }
    }

    "fail on bad actions chain" in {
      makeEmptySituation.board.seq(
        _.place(Sente - Pawn, SQ9H),
        _.place(Sente - Pawn, SQ7G),
        _.move(SQ8G, SQ8F)
      ) must beNone
    }

    "navigate in pos based on pieces" in {
      "right to end" in {
        val board: Board = """
R . . . K . . . R""".board
        SQ5I >| (p => board.pieces contains p) must_== List(SQ4I, SQ3I, SQ2I, SQ1I)
      }
      "right to next" in {
        val board: Board = """
R . . . K B . . R""".board
        SQ5I >| (p => board.pieces contains p) must_== List(SQ4I)
      }
      "left to end" in {
        val board: Board = """
R . . . K . . . R""".board
        SQ5I |< (p => board.pieces contains p) must_== List(SQ6I, SQ7I, SQ8I, SQ9I)
      }
      "right to next" in {
        val board: Board = """
R . . B K . . . R""".board
        SQ5I |< (p => board.pieces contains p) must_== List(SQ6I)
      }
    }
  }
}
