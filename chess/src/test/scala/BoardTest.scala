package lila.chess

import Pos._

class BoardTest extends ChessTest {

  val board = Board()

  "a board" should {

    "position pieces correctly" in {
      board.pieces must havePairs(A1 -> (White - Rook), B1 -> (White - Knight), C1 -> (White - Bishop), D1 -> (White - Queen), E1 -> (White - King), F1 -> (White - Bishop), G1 -> (White - Knight), H1 -> (White - Rook), A2 -> (White - Pawn), B2 -> (White - Pawn), C2 -> (White - Pawn), D2 -> (White - Pawn), E2 -> (White - Pawn), F2 -> (White - Pawn), G2 -> (White - Pawn), H2 -> (White - Pawn), A7 -> (Black - Pawn), B7 -> (Black - Pawn), C7 -> (Black - Pawn), D7 -> (Black - Pawn), E7 -> (Black - Pawn), F7 -> (Black - Pawn), G7 -> (Black - Pawn), H7 -> (Black - Pawn), A8 -> (Black - Rook), B8 -> (Black - Knight), C8 -> (Black - Bishop), D8 -> (Black - Queen), E8 -> (Black - King), F8 -> (Black - Bishop), G8 -> (Black - Knight), H8 -> (Black - Rook))
    }

    "have pieces by default" in {
      board.pieces must not beEmpty
    }

    "allow a piece to be placed" in {
      board place White - Rook at E3 must beSuccess.like {
        case b ⇒ b(E3) mustEqual Some(White - Rook)
      }
    }

    "allow a piece to be taken" in {
      board take A1 must beSome.like {
        case b ⇒ b(A1) must beNone
      }
    }

    "allow a piece to move" in {
      board move E2 to E4 must beSuccess.like {
        case b ⇒ b(E4) mustEqual Some(White - Pawn)
      }
    }

    "not allow an empty position to move" in {
      board move E5 to E6 must beFailure
    }

    "not allow a piece to move to an occupied position" in {
      board move A1 to A2 must beFailure
    }

    "allow a pawn to be promoted to a queen" in {
      Board.empty.place(Black.pawn, A7) flatMap (_.promote(A7, A8)) must beSome.like {
        case b ⇒ b(A8) must beSome(Black.queen)
      }
    }

    "allow chaining actions" in {
      Board.empty.seq(
        _ place White - Pawn at A2,
        _ place White - Pawn at A3,
        _ move A2 to A4
      ) must beSuccess.like {
          case b ⇒ b(A4) mustEqual Some(White - Pawn)
        }
    }

    "fail on bad actions chain" in {
      Board.empty.seq(
        _ place White - Pawn at A2,
        _ place White - Pawn at A3,
        _ move B2 to B4
      ) must beFailure
    }

    "provide occupation map" in {
      Board(
        A2 -> (White - Pawn),
        A3 -> (White - Pawn),
        D1 -> (White - King),
        E8 -> (Black - King),
        H4 -> (Black - Queen)
      ).occupation must havePairs(
          White -> Set(A2, A3, D1),
          Black -> Set(E8, H4)
        )
    }

    "navigate in pos based on pieces" in {
      "right to end" in {
        val board: Board = """
R   K  R"""
        E1 >| (p ⇒ board occupations p) must_== List(F1, G1, H1)
      }
      "right to next" in {
        val board: Board = """
R   KB R"""
        E1 >| (p ⇒ board occupations p) must_== List(F1)
      }
      "left to end" in {
        val board: Board = """
R   K  R"""
        E1 |< (p ⇒ board occupations p) must_== List(D1, C1, B1, A1)
      }
      "right to next" in {
        val board: Board = """
R  BK  R"""
        E1 |< (p ⇒ board occupations p) must_== List(D1)
      }
    }
    "detect" in {
      "automatic draw" in {
        "by lack of pieces" in {
          "empty" in {
            Board.empty.autoDraw must_== true
          }
          "new" in {
            Board().autoDraw must_== false
          }
          "opened" in {
            Game().playMoves(E2 -> E4, C7 -> C5, C2 -> C3, D7 -> D5, E4 -> D5) map { g ⇒
              g.board.autoDraw
            } must beSuccess(false)
          }
          "two kings" in {
            """
        k
  K      """.autoDraw must_== true
          }
          "two kings and one pawn" in {
            """
    P   k
  K      """.autoDraw must_== false
          }
          "two kings and one bishop" in {
            """
        k
  K     B""".autoDraw must_== true
          }
          "two kings, one bishop and one knight of different colors" in {
            """
        k
  K n   B""".autoDraw must_== true
          }
          "two kings, one bishop and one knight of same color" in {
            """
    B   k
  K N    """.autoDraw must_== false
          }
          "two kings, one bishop and one rook of different colors" in {
            """
        k
  K r   B""".autoDraw must_== false
          }
        }
        "by fifty moves" in {
          "new" in {
            Board().autoDraw must_== false
          }
          "opened" in {
            Game().playMoves(E2 -> E4, C7 -> C5, C2 -> C3, D7 -> D5, E4 -> D5) map { g ⇒
              g.board.autoDraw
            } must beSuccess(false)
          }
          "tons of pointless moves" in {
            val moves = List.fill(30)(List(B1 -> C3, B8 -> C6, C3 -> B1, C6 -> B8))
            Game().playMoves(moves.flatten: _*) must beSuccess.like {
              case g ⇒ g.board.autoDraw must_== true
            }
          }
        }
      }
    }
  }
}
