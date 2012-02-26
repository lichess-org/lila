package lila
package model

import Pos._

class CastleTest extends LilaSpec {

  "a king castle" should {

    "king side" in {
        val goodHist = """
PPPPPPPP
R  QK  R"""
        val badHist = goodHist updateHistory (_ withoutCastles White)
      "impossible" in {
        "standard chess" in {
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
        "chess960" in {
        val board960 = """
PPPPPPPP
RQK   R """ withHistory History.castle(White, true, true)
          "near bishop in the way" in {
            board960 place White.bishop at D1 flatMap (_ movesFrom C1) must bePoss()
          }
          "distant knight in the way" in {
            board960 place White.knight at F1 flatMap (_ movesFrom C1) must bePoss(D1)
          }
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
        val goodHist = """
PPPPPPPP
R   KB R"""
        val badHist = goodHist updateHistory (_ withoutCastles White)
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
          goodHist movesFrom E1 must bePoss(D1, C1)
        }
        "correct new board" in {
          situation.playMove(E1, C1) must beSituation("""
PPPPPPPP
  KR B R""")
        }
      }
    }

    "impact history" in {
        val board = """
PPPPPPPP
R   K  R""" withHistory History.castle(White, true, true)
        val situation = board as White
      "if king castles kingside" in {
        val s2 = situation.playMove(E1, G1)
        "correct new board" in {
          s2 must beSituation("""
PPPPPPPP
R    RK """)
        }
        "cannot castle queenside anymore" in {
          s2 flatMap (_.board movesFrom G1) must bePoss(H1)
        }
        "cannot castle kingside anymore even if the position looks good" in {
          s2 flatMap (_.board.seq(
            _ move F1 to H1,
            _ move G1 to E1
          )) flatMap (_ movesFrom E1) must bePoss(D1, F1)
        }
      }
      "if king castles queenside" in {
        val s2 = situation.playMove(E1, C1)
        "correct new board" in {
          s2 must beSituation("""
PPPPPPPP
  KR   R""")
        }
        "cannot castle kingside anymore" in {
          s2 flatMap (_.board movesFrom C1) must bePoss(B1)
        }
        "cannot castle queenside anymore even if the position looks good" in {
          s2 flatMap (_.board.seq(
            _ move D1 to A1,
            _ move C1 to E1
          )) flatMap (_ movesFrom E1) must bePoss(D1, F1)
        }
      }
      "if kingside rook moves" in {
        val s2 = situation.playMove(H1, G1) map (_ as White)
        "can only castle queenside" in {
          s2 flatMap (_.board movesFrom E1) must bePoss(C1, D1, F1)
        }
        "if queenside rook moves" in {
          val s3 = s2 flatMap (_.playMove(A1, B1))
          "can not castle at all" in {
            s3 flatMap (_.board movesFrom E1) must bePoss(D1, F1)
          }
        }
      }
      "if queenside rook moves" in {
        val s2 = situation.playMove(A1, B1) map (_ as White)
        "can only castle kingside" in {
          s2 flatMap (_.board movesFrom E1) must bePoss(D1, F1, G1)
        }
        "if kingside rook moves" in {
          val s3 = s2 flatMap (_.playMove(H1, G1))
          "can not castle at all" in {
            s3 flatMap (_.board movesFrom E1) must bePoss(D1, F1)
          }
        }
      }
    }
  }
}
