package lila
package model

import Pos._

class CastleTest extends LilaSpec {

  "a king" should {

    val king = White - King

    "castle" in {
      "king side" in {
          val board: Board = """
PPPPPPPP
R  QK  R"""
        "impossible" in {
          "pieces in the way" in {
            Board().movesFrom(E1) must bePoss()
          }
          "not allowed by history" in {
            board movesFrom E1 must bePoss(F1)
          }
        }
        "possible" in {
          val board2 = board withHistory History.castle(White, true, true)
          val situation = board2 as White
          "viable moves" in {
            board2 movesFrom E1 must bePoss(F1, G1)
          }
          "correct new board" in {
            situation.playMove(E1, G1) must beSituation("""
PPPPPPPP
R  Q RK """)
          }
        }
      }
      //"queen side" in {
      //"impossible" in {
      //"pieces in the way" in {
      //Board() movesFrom E1 must bePoss()
      //}
      //}
      //"possible" in {
      //val board = """
      //PPPPPP
      //R   KB"""
      //"viable moves" in {
      //board movesFrom E1 must bePoss(D1, C1)
      //}
      //}
      //}
    }
  }
}
