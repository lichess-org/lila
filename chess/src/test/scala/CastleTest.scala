package lila.chess

import Pos._

class CastleTest extends LilaTest {

  "a king castle" should {

    "king side" in {
        val goodHist = """
PPPPPPPP
R  QK  R"""
        val badHist = goodHist updateHistory (_ withoutCastles White)
      "impossible" in {
        "standard chess" in {
          "near bishop in the way" in {
            goodHist place White.bishop at F1 flatOption (_ destsFrom E1) must bePoss()
          }
          "distant knight in the way" in {
            goodHist place White.knight at G1 flatOption (_ destsFrom E1) must bePoss(F1)
          }
          "not allowed by history" in {
            badHist destsFrom E1 must bePoss(F1)
          }
        }
        "chess960" in {
        val board960 = """
PPPPPPPP
RQK   R """ withHistory History.castle(White, true, true)
          "near bishop in the way" in {
            board960 place White.bishop at D1 flatOption (_ destsFrom C1) must bePoss()
          }
          "distant knight in the way" in {
            board960 place White.knight at F1 flatOption (_ destsFrom C1) must bePoss(D1)
          }
        }
      }
      "possible" in {
        val game = Game(goodHist, White)
        "viable moves" in {
          game.board destsFrom E1 must bePoss(F1, G1)
        }
        "correct new board" in {
          game.playMove(E1, G1) must beGame("""
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
          goodHist place White.queen at D1 flatOption (_ destsFrom E1) must bePoss()
        }
        "bishop in the way" in {
          goodHist place White.bishop at C1 flatOption (_ destsFrom E1) must bePoss(D1)
        }
        "distant knight in the way" in {
          goodHist place White.knight at C1 flatOption (_ destsFrom E1) must bePoss(D1)
        }
        "not allowed by history" in {
          badHist destsFrom E1 must bePoss(D1)
        }
      }
      "possible" in {
        val game = Game(goodHist, White)
        "viable moves" in {
          game.board destsFrom E1 must bePoss(D1, C1)
        }
        "correct new board" in {
          game.playMove(E1, C1) must beGame("""
PPPPPPPP
  KR B R""")
        }
      }
    }

    "impact history" in {
        val board = """
PPPPPPPP
R   K  R""" withHistory History.castle(White, true, true)
        val game = Game(board, White)
      "if king castles kingside" in {
        val g2 = game.playMove(E1, G1)
        "correct new board" in {
          g2 must beGame("""
PPPPPPPP
R    RK """)
        }
        "cannot castle queenside anymore" in {
          g2 flatOption (_.board destsFrom G1) must bePoss(H1)
        }
        "cannot castle kingside anymore even if the position looks good" in {
          g2 flatMap (_.board.seq(
            _ move F1 to H1,
            _ move G1 to E1
          )) flatOption (_ destsFrom E1) must bePoss(D1, F1)
        }
      }
      "if king castles queenside" in {
        val g2 = game.playMove(E1, C1)
        "correct new board" in {
          g2 must beGame("""
PPPPPPPP
  KR   R""")
        }
        "cannot castle kingside anymore" in {
          g2 flatOption (_.board destsFrom C1) must bePoss(B1)
        }
        "cannot castle queenside anymore even if the position looks good" in {
          g2 flatMap (_.board.seq(
            _ move D1 to A1,
            _ move C1 to E1
          )) flatOption (_ destsFrom E1) must bePoss(D1, F1)
        }
      }
      "if king moves" in {
        "to the right" in {
          val g2 = game.playMove(E1, F1) map (_ as White)
          "cannot castle anymore" in {
            g2 flatOption (_.board destsFrom F1) must bePoss(E1, G1)
          }
          "neither if the king comes back" in {
            val g3 = g2 flatMap (_.playMove(F1, E1)) map (_ as White)
            g3 flatOption (_.board destsFrom E1) must bePoss(D1, F1)
          }
        }
        "to the left" in {
          val g2 = game.playMove(E1, D1) map (_ as White)
          "cannot castle anymore" in {
            g2 flatOption (_.board destsFrom D1) must bePoss(C1, E1)
          }
          "neither if the king comes back" in {
            val g3 = g2 flatMap (_.playMove(D1, E1)) map (_ as White)
            g3 flatOption (_.board destsFrom E1) must bePoss(D1, F1)
          }
        }
      }
      "if kingside rook moves" in {
        val g2 = game.playMove(H1, G1) map (_ as White)
        "can only castle queenside" in {
          g2 flatOption (_.board destsFrom E1) must bePoss(C1, D1, F1)
        }
        "if queenside rook moves" in {
          val g3 = g2 flatMap (_.playMove(A1, B1))
          "can not castle at all" in {
            g3 flatOption (_.board destsFrom E1) must bePoss(D1, F1)
          }
        }
      }
      "if queenside rook moves" in {
        val g2 = game.playMove(A1, B1) map (_ as White)
        "can only castle kingside" in {
          g2 flatOption (_.board destsFrom E1) must bePoss(D1, F1, G1)
        }
        "if kingside rook moves" in {
          val g3 = g2 flatMap (_.playMove(H1, G1))
          "can not castle at all" in {
            g3 flatOption (_.board destsFrom E1) must bePoss(D1, F1)
          }
        }
      }
    }
    "threat on king prevents castling" in {
      val board: Board = """R   K  R"""
      "by a rook" in {
        board place Black.rook at E3 flatOption (_ destsFrom E1) must bePoss(D1, D2, F2, F1)
      }
      "by a knight" in {
        board place Black.knight at D3 flatOption (_ destsFrom E1) must bePoss(D1, D2, E2, F1)
      }
    }
    "threat on castle trip prevents castling" in {
      "king side" in {
        val board: Board = """R  QK  R"""
        "close" in {
          board place Black.rook at F3 flatOption (_ destsFrom E1) must bePoss(D2, E2)
        }
        "far" in {
          board place Black.rook at G3 flatOption (_ destsFrom E1) must bePoss(D2, E2, F2, F1)
        }
      }
      "queen side" in {
        val board: Board = """R   KB R"""
        "close" in {
          board place Black.rook at D3 flatOption (_ destsFrom E1) must bePoss(E2, F2)
        }
        "far" in {
          board place Black.rook at C3 flatOption (_ destsFrom E1) must bePoss(D1, D2, E2, F2)
        }
      }
      "chess 960" in {
        "far kingside" in {
          val board: Board = """BK     R"""
          "rook threat" in {
            board place Black.rook at F3 flatOption (_ destsFrom B1) must bePoss(A2, B2, C2, C1)
          }
          "enemy king threat" in {
            board place Black.king at E2 flatOption (_ destsFrom B1) must bePoss(A2, B2, C2, C1)
          }
        }
      }
    }
    "threat on rook does not prevent castling" in {
      "king side" in {
        val board: Board = """R  QK  R"""
        board place Black.rook at H3 flatOption (_ destsFrom E1) must bePoss(D2, E2, F1, F2, G1)
      }
      "queen side" in {
        val board: Board = """R   KB R"""
        board place Black.rook at A3 flatOption (_ destsFrom E1) must bePoss(C1, D1, D2, E2, F2)
      }
    }
  }
}
