package lila.chess

import Pos._

class PawnTest extends LilaTest {

  "a white pawn" should {

    "move towards rank by 1 square" in {
      Board(
        A4 -> White.pawn
      ) movesFrom A4 must bePoss(A5)
    }

    "not move to positions that are occupied by the same color" in {
      Board(
        A4 -> White.pawn,
        A5 -> White.pawn
      ) movesFrom A4 must bePoss()
    }

    "capture in diagonal" in {
      Board(
        D4 -> White.pawn,
        C5 -> Black.pawn,
        E5 -> Black.bishop
      ) movesFrom D4 must bePoss(C5, D5, E5)
    }

    "require a capture to move in diagonal" in {
      Board(
        A4 -> White.pawn,
        C5 -> White.pawn
      ) movesFrom A4 must bePoss(A5)
    }

    "move towards rank by 2 squares" in {
      "if the path is free" in {
        Board(
          A2 -> White.pawn
        ) movesFrom A2 must bePoss(A3, A4)
      }
      "if the path is occupied by a friend" in {
        "close" in {
          Board(
            A2 -> White.pawn,
            A3 -> White.rook
          ) movesFrom A2 must bePoss()
        }
        "far" in {
          Board(
            A2 -> White.pawn,
            A4 -> White.rook
          ) movesFrom A2 must bePoss(A3)
        }
      }
      "if the path is occupied by a enemy" in {
        "close" in {
          Board(
            A2 -> White.pawn,
            A3 -> Black.rook
          ) movesFrom A2 must bePoss()
        }
        "far" in {
          Board(
            A2 -> White.pawn,
            A4 -> Black.rook
          ) movesFrom A2 must bePoss(A3)
        }
      }
    }
    "capture en passant" in {
      "with proper position" in {
        val board = Board(
          D5 -> White.pawn,
          C5 -> Black.pawn,
          E5 -> Black.pawn
        )
        "without history" in {
          board movesFrom D5 must bePoss(D6)
        }
        "with irrelevant history" in {
          board withHistory History(
            lastMove = Some(A2 -> A4)
          ) movesFrom D5 must bePoss(D6)
        }
        "with relevant history on the left" in {
          board withHistory History(
            lastMove = Some(C7 -> C5)
          ) movesFrom D5 must bePoss(D6, C6)
        }
        "with relevant history on the right" in {
          board withHistory History(
            lastMove = Some(E7 -> E5)
          ) movesFrom D5 must bePoss(D6, E6)
        }
      }
      "enemy not-a-pawn" in {
        Board(
          D5 -> White.pawn,
          E5 -> Black.rook
        ) withHistory History(
          lastMove = Some(E7 -> E5)
        ) movesFrom D5 must bePoss(D6)
      }
      "friend pawn (?!)" in {
        Board(
          D5 -> White.pawn,
          E5 -> White.pawn
        ) withHistory History(
          lastMove = Some(E7 -> E5)
        ) movesFrom D5 must bePoss(D6)
      }
    }
  }

  "a black pawn" should {

    "move towards rank by 1 square" in {
      Board(
        A4 -> Black.pawn
      ) movesFrom A4 must bePoss(A3)
    }

    "not move to positions that are occupied by the same color" in {
      Board(
        A4 -> Black.pawn,
        A3 -> Black.pawn
      ) movesFrom A4 must bePoss()
    }

    "capture in diagonal" in {
      Board(
        D4 -> Black.pawn,
        C3 -> White.pawn,
        E3 -> White.bishop
      ) movesFrom D4 must bePoss(C3, D3, E3)
    }

    "require a capture to move in diagonal" in {
      Board(
        A4 -> Black.pawn,
        C3 -> Black.pawn
      ) movesFrom A4 must bePoss(A3)
    }

    "move towards rank by 2 squares" in {
      "if the path is free" in {
        Board(
          A7 -> Black.pawn
        ) movesFrom A7 must bePoss(A6, A5)
      }
      "if the path is occupied by a friend" in {
        "close" in {
          Board(
            A7 -> Black.pawn,
            A6 -> Black.rook
          ) movesFrom A7 must bePoss()
        }
        "far" in {
          Board(
            A7 -> Black.pawn,
            A5 -> Black.rook
          ) movesFrom A7 must bePoss(A6)
        }
      }
      "if the path is occupied by a enemy" in {
        "close" in {
          Board(
            A7 -> Black.pawn,
            A6 -> White.rook
          ) movesFrom A7 must bePoss()
        }
        "far" in {
          Board(
            A7 -> Black.pawn,
            A5 -> White.rook
          ) movesFrom A7 must bePoss(A6)
        }
      }
    }
    "capture en passant" in {
      "with proper position" in {
        val board = Board(
          D4 -> Black.pawn,
          C4 -> White.pawn,
          E4 -> White.pawn
        )
        "without history" in {
          board movesFrom D4 must bePoss(D3)
        }
        "with relevant history on the left" in {
          board withHistory History(
            lastMove = Some(C2 -> C4)
          ) movesFrom D4 must bePoss(D3, C3)
        }
      }
      "enemy not-a-pawn" in {
        Board(
          D4 -> Black.pawn,
          E4 -> White.rook
        ) withHistory History(
          lastMove = Some(E2 -> E4)
        ) movesFrom D4 must bePoss(D3)
      }
    }
  }
}
