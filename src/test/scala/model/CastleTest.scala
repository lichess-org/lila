package lila
package model

import Pos._

class CastleTest extends LilaSpec {

  "a king castle" should {

    "king side" in {
        val badHist = """
PPPPPPPP
R  QK  R""" withHistory History.castle(White, false, false)
        val goodHist = badHist withHistory History.castle(White, true, true)
      "impossible" in {
        "near bishop in the way" in {
          goodHist place White.bishop at F1 flatMap (_ movesFrom E1) must bePoss()
        }
        "distant knight in the way" in {
          goodHist place White.knight at G1 flatMap (_ movesFrom E1) must bePoss(F1)
        }
        "not allowed by history" in {
          badHist movesFrom E1 must bePoss(F1)
        }
      }
      "possible" in {
        val situation = goodHist as White
        "viable moves" in {
          goodHist movesFrom E1 must bePoss(F1, G1)
        }
        "correct new board" in {
          situation.playMove(E1, G1) must beSituation("""
PPPPPPPP
R  Q RK """)
        }
      }
    }

    "queen side" in {
        val badHist = """
PPPPPPPP
R   KB R""" withHistory History.castle(White, false, false)
        val goodHist = badHist withHistory History.castle(White, true, true)
      "impossible" in {
        "near queen in the way" in {
          goodHist place White.queen at D1 flatMap (_ movesFrom E1) must bePoss()
        }
        "bishop in the way" in {
          goodHist place White.bishop at C1 flatMap (_ movesFrom E1) must bePoss(D1)
        }
        "distant knight in the way" in {
          goodHist place White.knight at C1 flatMap (_ movesFrom E1) must bePoss(D1)
        }
        "not allowed by history" in {
          badHist movesFrom E1 must bePoss(D1)
        }
      }
      "possible" in {
        val situation = goodHist as White
        "viable moves" in {
          goodHist movesFrom E1 must bePoss(D1, B1)
        }
        "correct new board" in {
          situation.playMove(E1, B1) must beSituation("""
PPPPPPPP
  KR B R""")
        }
      }
    }
  }
}
