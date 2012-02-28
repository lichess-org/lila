package lila.chess
package format

import Pos._

class PgnDumpTest extends LilaTest {

  "complete game dump" should {
    "only moves" in {
      "Gioachine Greco" in {
        val game = Game().playMoves(D2 -> D4, D7 -> D5, C2 -> C4, D5 -> C4, E2 -> E3, B7 -> B5, A2 -> A4, C7 -> C6, A4 -> B5, C6 -> B5, D1 -> F3)
        game map (_.pgnMoves) must beSuccess.like {
          case ms ⇒ ms must_== ("d4 d5 c4 dxc4 e3 b5 a4 c6 axb5 cxb5 Qf3" split ' ')
        }
      }
    }
  }
}
