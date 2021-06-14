package shogi
package format.pgn

import format.Forsyth
import Pos._

class DumperTest extends ShogiTest {

  val game1 = makeGame.playMoves(
    C3 -> C4,
    B7 -> B6,
    C1 -> D2,
    G7 -> G6,
    D2 -> C3,
    C9 -> D8,
    H3 -> H4,
    G9 -> F8,
    G1 -> F2,
    F9 -> G8,
    D1 -> C2,
    E9 -> F9
  )

  "standard game" should {
    "move list" in {
      "Game 1." in {
        game1 map (_.pgnMoves) must beSuccess.like { case ms =>
          ms must_== "Pc4 Pb6 Sd2 Pg6 Sc3 Sd8 Ph4 Sf8 Sf2 Gg8 Gc2 Kf9".split(' ').toList
        }
      }
    }
  }

  "dump a promotion/unpromotion move" should {
    "unpromotion" in {
      val game = Game("""


P    k



 PP   PPP

KNSG GSNL
""")
      game.playMove(A7, A8, false) map (_.pgnMoves) must beSuccess.like { case ms =>
        ms must_== List("Pa8=")
      }
    }
    "with check" in {
      val game = Game("""
 
    k
P



 PP   PPPP

KNSG GSNL
""")
      game.playMove(A7, A8, true) map (_.pgnMoves) must beSuccess.like { case ms =>
        ms must_== List("Pa8+")
      }
    }
    "forced promotion" in {
      val game = Game("""
    k
P




 PP   PPP

KNSG GSNL
""")
      game.playMove(A8, A9, true) map (_.pgnMoves) must beSuccess.like { case ms =>
        ms must_== List("Pa9+")
      }
    }
  }

  "ambiguous moves" should {
    "ambiguous rook" in {
      val game = Game("""
k





P   K   P
LR     RL
""")
      game.playMoves(H1 -> C1) map (_.pgnMoves) must beSuccess.like { case ms =>
        ms must_== List("Rh1c1")
      }
    }
    "not ambiguous bishop promotion, but still..." in {
      val game = Game("""
B   k





PPPP
    K  B  
LNSG GSNL
""")
      game.playMove(A9, E5, true) map (_.pgnMoves) must beSuccess.like { case ms =>
        ms must_== List("Ba9e5+")
      }
    }
    "not ambiguous bishop promotion - other bishop" in {
      val game = Game("""
B   k





PPPP
    K  B  
LNSG GSNL
""")
      game.playMoves(H2 -> E5) map (_.pgnMoves) must beSuccess.like { case ms =>
        ms must_== List("Bh2e5")
      }
    }
    "1. ambiguous knight" in {
      val game = Game("""
k
 R
P
    N N


P      P
 B
L SGKGS L
""")
      game.playMove(E6, F8, true) map (_.pgnMoves) must beSuccess.like { case ms =>
        ms must_== List("Ne6f8+")
      }
    }
    "2. ambiguous knight" in {
      val game = Game("""
k
 R
P
    N N


P      P
 B
L SGKGS L
""")
      game.playMove(G6, F8, true) map (_.pgnMoves) must beSuccess.like { case ms =>
        ms must_== List("Ng6f8+")
      }
    }
  }

  "move comment" should {
    "simple" in {
      Move("Pe4", List("Some comment")).toString must_== "Pe4 { Some comment }"
    }
    "one line break" in {
      Move(
        "Pe4",
        List("""Some
comment""")
      ).toString must_== """Pe4 { Some
comment }"""
    }
    "two line breaks" in {
      Move(
        "Pe4",
        List("""Some

comment""")
      ).toString must_== """Pe4 { Some
comment }"""
    }
    "three line breaks" in {
      Move(
        "Pe4",
        List("""Some


comment""")
      ).toString must_== """Pe4 { Some
comment }"""
    }
  }
}
