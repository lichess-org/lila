package lila
package model

import Pos._
import format.Visual

class KingSafetyTest extends LilaSpec {

  "in order to save the king" should {
    "the king" in {
      "not commit suicide" in { """
    P n
PPPP   P
RNBQK  R""" movesFrom E1 must bePoss(F2)
      }
      "escape from danger" in { """
    r

PPPP   P
RNBQK  R""" movesFrom E1 must bePoss(F1, F2)
      }
    }
  }
}
