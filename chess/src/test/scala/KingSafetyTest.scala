package lila.chess

import Pos._

class KingSafetyTest extends ChessTest {

  "in order to save the king" should {
    "the king" in {
      "not commit suicide" in {
        """
    P n
PPPP   P
RNBQK  R""" destsFrom E1 must bePoss(F2)
      }
      "not commit suicide even if immobilized" in {
        """
    b n
PPPP   P
RNBQK  R""" destsFrom E1 must bePoss()
      }
      "escape from danger" in {
        """
    r

PPPP   P
RNBQK  R""" destsFrom E1 must bePoss(F1, F2)
      }
    }
    "pieces" in {
      "move to defend" in {
        "queen" in {
          """
    r

PPPP   P
RNBQK  R""" destsFrom D1 must bePoss(E2)
        }
        "knight" in {
          """
    r

PPPP   P
RNBQK NR""" destsFrom G1 must bePoss(E2)
        }
        "pawn" in {
          """
  K    r
PPPP   P
RNBQ  NR""" destsFrom D2 must bePoss(D3)
        }
        "pawn double square" in {
          """
  K    r

PPPP   P
RNBQ  NR""" destsFrom D2 must bePoss(D4)
        }
      }
      "eat to defend" in {
        "queen" in {
          """
    r

PPPPK Q
RNB    R""" destsFrom G2 must bePoss(E4)
        }
        "queen defender" in {
          """
    r

PPPPQ
RNB K  R""" destsFrom E2 must bePoss(E3, E4)
        }
        "pawn" in {
          """
    r
     P
PPPP
RNB K  R""" destsFrom F3 must bePoss(E4)
        }
      }
      "stay to defend" in {
        "bishop" in {
          """
    r

PPPPB
RNB K  R""" destsFrom E2 must bePoss()
        }
        "pawn" in {
          """

 K P  r
PPP
RNB    R""" destsFrom D3 must bePoss()
        }
      }
    }
  }
}
