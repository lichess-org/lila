package lila.chess
package model

import Pos._

class PromotionTest extends LilaSpec {

  "pawn promotion" should {
    val situation = """
  p
K      """ as Black
     "promote to a queen" in {
       situation.playMove(C2, C1, Queen) must beSituation("""

K q    """)
     }
     "promote to a queen by default" in {
       situation.playMove(C2, C1) must beSituation("""

K q    """)
     }
     "promote to a knight" in {
       situation.playMove(C2, C1, Knight) must beSituation("""

K n    """)
     }
  }
}
