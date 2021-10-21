package shogi

import Pos._

class LanceTest extends ShogiTest {

  "a rook" should {

    val senteRook = Sente - Lance
    val goteRook  = Gote - Lance

    "move to any position in front" in {
      pieceMoves(senteRook, SQ5E) must bePoss(SQ5D, SQ5C, SQ5B, SQ5A)
      pieceMoves(goteRook, SQ5E) must bePoss(SQ5I, SQ5H, SQ5G, SQ5F)
    }

    "threaten" in {
      val board = """
k B
  r  r
p

n L    P

PPPPPPPPP

    K
"""
      "an unreachable enemy to the left" in {
        board actorAt SQ7E map (_ threatens SQ9E) must beSome(false)
      }
      "a reachable enemy to the top" in {
        board actorAt SQ7E map (_ threatens SQ7B) must beSome(true)
      }
      "an unreachable enemy" in {
        board actorAt SQ7E map (_ threatens SQ9C) must beSome(false)
      }
      "a unreachable friend" in {
        board actorAt SQ7E map (_ threatens SQ2E) must beSome(false)
      }
      "nothing up" in {
        board actorAt SQ7E map (_ threatens SQ7D) must beSome(true)
      }
      "nothing down" in {
        board actorAt SQ7E map (_ threatens SQ7F) must beSome(false)
      }
    }
  }
}
