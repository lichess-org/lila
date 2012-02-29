package lila.chess
package format

import Pos._

class PgnDumpTest extends ChessTest {

  val gioachineGreco = Game().playMoves(D2 -> D4, D7 -> D5, C2 -> C4, D5 -> C4, E2 -> E3, B7 -> B5, A2 -> A4, C7 -> C6, A4 -> B5, C6 -> B5, D1 -> F3)

  val peruvianImmortal = Game().playMoves(E2 -> E4, D7 -> D5, E4 -> D5, D8 -> D5, B1 -> C3, D5 -> A5, D2 -> D4, C7 -> C6, G1 -> F3, C8 -> G4, C1 -> F4, E7 -> E6, H2 -> H3, G4 -> F3, D1 -> F3, F8 -> B4, F1 -> E2, B8 -> D7, A2 -> A3, E8 -> C8, A3 -> B4, A5 -> A1, E1 -> D2, A1 -> H1, F3 -> C6, B7 -> C6, E2 -> A6)

  "dump a game to pgn" should {
    "move list" in {
      "Gioachine Greco" in {
        gioachineGreco map (_.pgnMoves) must beSuccess.like {
          case ms ⇒ ms must_== ("d4 d5 c4 dxc4 e3 b5 a4 c6 axb5 cxb5 Qf3" split ' ').toSeq
        }
      }
      "Peruvian Immortal" in {
        peruvianImmortal map (_.pgnMoves) must beSuccess.like {
          case ms ⇒ ms must_== ("e4 d5 exd5 Qxd5 Nc3 Qa5 d4 c6 Nf3 Bg4 Bf4 e6 h3 Bxf3 Qxf3 Bb4 Be2 Nd7 a3 O-O-O axb4 Qxa1+ Kd2 Qxh1 Qxc6+ bxc6 Ba6#" split ' ').toSeq
        }
      }
    }
  }
}
