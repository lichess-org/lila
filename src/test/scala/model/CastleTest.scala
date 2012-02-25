package lila
package model

import Pos._

class CastleTest extends LilaSpec {

  "a king" should {

    val king = White - King

    "castle" in {
      "king side" in {
        "impossible" in {
          "pieces in the way" in {
            Board().movesFrom(E1) must bePoss()
          }
        }
        "possible" in {
          val board = """
PPPPPPPP
R  QK  R"""
          "viable moves" in {
            board movesFrom E1 must bePoss(F1, G1)
          }
        }
      }
      "queen side" in {
        "impossible" in {
          "pieces in the way" in {
            Board() movesFrom E1 must bePoss()
          }
        }
        "possible" in {
          val board = """
PPPPPP
R   KB"""
          "viable moves" in {
            board movesFrom E1 must bePoss(D1, C1)
          }
        }
      }
    }
  }
}
