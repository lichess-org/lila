package lila.chess

import Pos._
import format.Visual.addNewLines

class PlayTest extends ChessTest {

  "playing a game" should {
    "opening one" in {
      val game = Game().playMoves(
        E2 -> E4,
        E7 -> E5,
        F1 -> C4,
        G8 -> F6,
        D2 -> D3,
        C7 -> C6,
        C1 -> G5,
        H7 -> H6)
      "current game" in {
        game must beSuccess.like {
          case g ⇒ addNewLines(g.board.visual) must_== """
rnbqkb r
pp p pp
  p  n p
    p B
  B P
   P
PPP  PPP
RN QK NR
"""
        }
      }
      "after recapture" in {
        game flatMap { _.playMoves(G5 -> F6, D8 -> F6) } must beSuccess.like {
          case g ⇒ addNewLines(g.board.visual) must_== """
rnb kb r
pp p pp
  p  q p
    p
  B P
   P
PPP  PPP
RN QK NR
"""
        }
      }
    }
    "Deep Blue vs Kasparov 1" in {
      Game().playMoves(
        E2 -> E4,
        C7 -> C5,
        C2 -> C3,
        D7 -> D5,
        E4 -> D5,
        D8 -> D5,
        D2 -> D4,
        G8 -> F6,
        G1 -> F3,
        C8 -> G4,
        F1 -> E2,
        E7 -> E6,
        H2 -> H3,
        G4 -> H5,
        E1 -> G1,
        B8 -> C6,
        C1 -> E3,
        C5 -> D4,
        C3 -> D4,
        F8 -> B4
      ) must beSuccess.like {
          case g ⇒ addNewLines(g.board.visual) must_== """
r   k  r
pp   ppp
  n pn
   q   b
 b P
    BN P
PP  BPP
RN Q RK
"""
      }
    }
    "Peruvian Immortal" in {
      Game().playMoves(
        E2 -> E4,
        D7 -> D5,
        E4 -> D5,
        D8 -> D5,
        B1 -> C3,
        D5 -> A5,
        D2 -> D4,
        C7 -> C6,
        G1 -> F3,
        C8 -> G4,
        C1 -> F4,
        E7 -> E6,
        H2 -> H3,
        G4 -> F3,
        D1 -> F3,
        F8 -> B4,
        F1 -> E2,
        B8 -> D7,
        A2 -> A3,
        E8 -> C8,
        A3 -> B4,
        A5 -> A1,
        E1 -> D2,
        A1 -> H1,
        F3 -> C6,
        B7 -> C6,
        E2 -> A6
      ) must beSuccess.like {
          case g ⇒ addNewLines(g.board.visual) must_== """
  kr  nr
p  n ppp
B p p

 P P B
  N    P
 PPK PP
       q
"""
      }
    }
  }
}
