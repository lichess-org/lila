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
LNSGK  NL""" destsFrom SQ5I must bePoss(SQ4H)
      }
      "not commit suicide even if immobilized" in {
        """
    h n
PPPP   PP
LNSGK  NL""" destsFrom SQ5I must bePoss()
      }
      "escape from danger" in {
        """
    r

PPPP   PP
LNSGK SNL""" destsFrom SQ5I must bePoss(SQ4I, SQ4H)
      }
    }
    "pieces" in {
      "move to defend" in {
        "gold" in {
          """
    r

PPPP   PP

LNSGK  NL""" destsFrom SQ6I must bePoss(SQ5H)
        }
        "knight" in {
          """
    r

PPPP   PP
   N
L SGK SNL""" destsFrom SQ6H must bePoss(SQ5F)
        }
        "pawn" in {
          """
  K    r
PPPP   PP

LNSG GSNL""" destsFrom SQ6G must bePoss(SQ6F)
        }
      }
      "eat to defend" in {
        "bishop" in {
          """
    r

PPPPK B
RNB     L""" destsFrom SQ3H must bePoss(SQ5F)
        }
        "rook defender" in {
          """
    r

PPPPR
RNB K   L""" destsFrom SQ5H must bePoss(SQ5G, SQ5F)
        }
        "pawn" in {
          """
K    r
     P
PPPP
LNSG GSNL""" destsFrom SQ4G must bePoss(SQ4F)
        }
      }
      "stay to defend" in {
        "bishop" in {
          """
    r

PPPPB
RNB K  R""" destsFrom SQ5H must bePoss()
        }
        "pawn" in {
          """

 K P  r
PPP

LNSG    L""" destsFrom SQ6F must bePoss()
        }
      }
    }
  }
}
