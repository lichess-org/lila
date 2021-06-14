package shogi

import Pos._

class KingSafetyTest extends ShogiTest {

  "in order to save the king" should {
    "the king" in {
      "not commit suicide" in {
        """
    P
PPPP  nPP
   GG
LNSGK  NL""" destsFrom E1 must bePoss(F2)
      }
      "not commit suicide even if immobilized" in {
        """
    h n
PPPP   PP
LNSGK  NL""" destsFrom E1 must bePoss()
      }
      "escape from danger" in {
        """
    r

PPPP   PP
LNSGK SNL""" destsFrom E1 must bePoss(F1, F2)
      }
    }
    "pieces" in {
      "move to defend" in {
        "gold" in {
          """
    r

PPPP   PP

LNSGK  NL""" destsFrom D1 must bePoss(E2)
        }
        "knight" in {
          """
    r

PPPP   PP
   N
L SGK SNL""" destsFrom D2 must bePoss(E4)
        }
        "pawn" in {
          """
  K    r
PPPP   PP

LNSG GSNL""" destsFrom D3 must bePoss(D4)
        }
      }
      "eat to defend" in {
        "bishop" in {
          """
    r

PPPPK B
RNB     L""" destsFrom G2 must bePoss(E4)
        }
        "rook defender" in {
          """
    r

PPPPR
RNB K   L""" destsFrom E2 must bePoss(E3, E4)
        }
        "pawn" in {
          """
K    r
     P
PPPP
LNSG GSNL""" destsFrom F3 must bePoss(F4)
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

LNSG    L""" destsFrom D4 must bePoss()
        }
      }
    }
  }
}
