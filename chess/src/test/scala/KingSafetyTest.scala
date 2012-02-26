package lila.chess

import Pos._

class KingSafetyTest extends LilaSpec {

  "in order to save the king" should {
    "the king" in {
      "not commit suicide" in {
        """
    P n
PPPP   P
RNBQK  R""" movesFrom E1 must bePoss(F2)
      }
      "not commit suicide even if immobilized" in {
        """
    b n
PPPP   P
RNBQK  R""" movesFrom E1 must bePoss()
      }
      "escape from danger" in {
        """
    r

PPPP   P
RNBQK  R""" movesFrom E1 must bePoss(F1, F2)
      }
    }
    "pieces" in {
      "move to defend" in {
        "queen" in {
          """
    r

PPPP   P
RNBQK  R""" movesFrom D1 must bePoss(E2)
        }
        "knight" in {
          """
    r

PPPP   P
RNBQK NR""" movesFrom G1 must bePoss(E2)
        }
        "pawn" in {
          """
  K    r
PPPP   P
RNBQ  NR""" movesFrom D2 must bePoss(D3)
        }
        "pawn double square" in {
          """
  K    r

PPPP   P
RNBQ  NR""" movesFrom D2 must bePoss(D4)
        }
      }
      "eat to defend" in {
        "queen" in {
          """
    r

PPPPK Q
RNB    R""" movesFrom G2 must bePoss(E4)
        }
        "queen defender" in {
          """
    r

PPPPQ
RNB K  R""" movesFrom E2 must bePoss(E3, E4)
        }
        "pawn" in {
          """
    r
     P
PPPP
RNB K  R""" movesFrom F3 must bePoss(E4)
        }
      }
      "stay to defend" in {
        "bishop" in {
          """
    r

PPPPB
RNB K  R""" movesFrom E2 must bePoss()
        }
        "pawn" in {
          """

 K P  r
PPP
RNB    R""" movesFrom D3 must bePoss()
        }
      }
    }
  }
}
