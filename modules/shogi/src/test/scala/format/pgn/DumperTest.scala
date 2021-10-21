package shogi
package format.pgn

import format.Forsyth
import Pos._

class DumperTest extends ShogiTest {

  val game1 = makeGame.playMoves(
    SQ7G -> SQ7F,
    SQ8C -> SQ8D,
    SQ7I -> SQ6H,
    SQ3C -> SQ3D,
    SQ6H -> SQ7G,
    SQ7A -> SQ6B,
    SQ2G -> SQ2F,
    SQ3A -> SQ4B,
    SQ3I -> SQ4H,
    SQ4A -> SQ3B,
    SQ6I -> SQ7H,
    SQ5A -> SQ4A
  )

  "standard game" should {
    "move list" in {
      "Game 1." in {
        game1 map (_.pgnMoves) must beValid.like { case ms =>
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
      game.playMove(SQ9C, SQ9B, false) map (_.pgnMoves) must beValid.like { case ms =>
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
      game.playMove(SQ9C, SQ9B, true) map (_.pgnMoves) must beValid.like { case ms =>
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
      game.playMove(SQ9B, SQ9A, true) map (_.pgnMoves) must beValid.like { case ms =>
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
      game.playMoves(SQ2I -> SQ7I) map (_.pgnMoves) must beValid.like { case ms =>
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
      game.playMove(SQ9A, SQ5E, true) map (_.pgnMoves) must beValid.like { case ms =>
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
      game.playMoves(SQ2H -> SQ5E) map (_.pgnMoves) must beValid.like { case ms =>
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
      game.playMove(SQ5D, SQ4B, true) map (_.pgnMoves) must beValid.like { case ms =>
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
      game.playMove(SQ3D, SQ4B, true) map (_.pgnMoves) must beValid.like { case ms =>
        ms must_== List("Ng6f8+")
      }
    }
  }
}
