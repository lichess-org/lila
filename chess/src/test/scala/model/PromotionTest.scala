package lila.chess
package model

import Pos._

class PromotionTest extends LilaSpec {

  "pawn promotion" should {
    val situation = """
  p
K      """ as Black
     "promote to a queen" in {
       situation.playMove(C2, C1, Some(Queen)) must beSituation("""

K q    """)
     }
     "promote to a knight" in {
       situation.playMove(C2, C1, Some(Knight)) must beSituation("""

K n    """)
     }
  }
}
