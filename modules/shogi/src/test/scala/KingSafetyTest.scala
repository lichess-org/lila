package shogi

import Pos._

class KingSafetyTest extends ShogiTest {

  "in order to save the king" should {
    "the king" in {
      "not commit suicide" in {
        """
. . . . P . . . .
P P P P . . n P P
. . . G G . . . .
L N S G K . . N L""" moveDestsFrom SQ5I must bePoss(SQ4H)
      }
      "not commit suicide even if immobilized" in {
        """
. . . .+b . n . .
P P P P . . . P P
L N S G K . . N L""" moveDestsFrom SQ5I must bePoss()
      }
      "escape from danger" in {
        """
. . . . r . . . .
. . . . . . . . .
P P P P . . . P P
L N S G K . S N L""" moveDestsFrom SQ5I must bePoss(SQ4I, SQ4H)
      }
    }
    "pieces" in {
      "move to defend" in {
        "gold" in {
          """
. . . . r . . . .
. . . . . . . . .
P P P P . . . P P
. . . . . . . . .
L N S G K . . N L""" moveDestsFrom SQ6I must bePoss(SQ5H)
        }
        "knight" in {
          """
. . . . r . . . .
. . . . . . . . .
P P P P . . . P P
. . . N . . . . .
L . S G K . S N L""" moveDestsFrom SQ6H must bePoss(SQ5F)
        }
        "pawn" in {
          """
. . K . . . . r .
P P P P . . . P P
. . . . . . . . .
L N S G . G S N L""" moveDestsFrom SQ6G must bePoss(SQ6F)
        }
      }
      "eat to defend" in {
        "bishop" in {
          """
. . . . r . . . .
. . . . . . . . .
P P P P K . B . .
R N B . . . . . L""" moveDestsFrom SQ3H must bePoss(SQ5F)
        }
        "rook defender" in {
          """
. . . . r . . . .
. . . . . . . . .
P P P P R . . . .
R N B . K . . . L""" moveDestsFrom SQ5H must bePoss(SQ5G, SQ5F)
        }
        "pawn" in {
          """
K . . . . r . . .
. . . . . P . . .
P P P P . . . . .
L N S G . G S N L""" moveDestsFrom SQ4G must bePoss(SQ4F)
        }
      }
      "stay to defend" in {
        "bishop" in {
          """
. . . . r . . . .
. . . . . . . . .
P P P P B . . . .
R N B . K . . R .""" moveDestsFrom SQ5H must bePoss()
        }
        "pawn" in {
          """
. . . . . . . . .
. K . P . . r . .
P P P . . . . . .
. . . . . . . . .
L N S G . . . . L""" moveDestsFrom SQ6F must bePoss()
        }
      }
    }
  }
}
