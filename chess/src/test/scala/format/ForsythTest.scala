package lila.chess
package format

import Pos._

class ForsythTest extends ChessTest {

  val f = Forsyth

  "the forsyth notation" should {
    "export" in {
      "game opening" in {
        val moves = List(E2 -> E4, C7 -> C5, G1 -> F3, G8 -> H6, A2 -> A3)
        "new game" in {
          f >> Game() must_== "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        }
        "one move" in {
          Game().playMoveList(moves take 1) must beSuccess.like {
            case g ⇒ f >> g must_== "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
          }
        }
        "2 moves" in {
          Game().playMoveList(moves take 2) must beSuccess.like {
            case g ⇒ f >> g must_== "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2"
          }
        }
        "3 moves" in {
          Game().playMoveList(moves take 3) must beSuccess.like {
            case g ⇒ f >> g must_== "rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2"
          }
        }
        "4 moves" in {
          Game().playMoveList(moves take 4) must beSuccess.like {
            case g ⇒ f >> g must_== "rnbqkb1r/pp1ppppp/7n/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"
          }
        }
        "5 moves" in {
          Game().playMoveList(moves take 5) must beSuccess.like {
            case g ⇒ f >> g must_== "rnbqkb1r/pp1ppppp/7n/2p5/4P3/P4N2/1PPP1PPP/RNBQKB1R b KQkq - 0 3"
          }
        }
      }
    }
    "import" in {
      val moves = List(E2 -> E4, C7 -> C5, G1 -> F3, G8 -> H6, A2 -> A3)
      "new game" in {
        f << "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1" must_== Game()
      }
      "one move" in {
        Game().playMoveList(moves take 1) must beSuccess.like {
          case g ⇒ (f << "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1").situation must_== g.situation
        }
      }
      "2 moves" in {
        Game().playMoveList(moves take 2) must beSuccess.like {
          case g ⇒ (f << "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2") must_== g.situation
        }
      }
      "3 moves" in {
        Game().playMoveList(moves take 3) must beSuccess.like {
          case g ⇒ (f << "rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2") must_== g.situation
        }
      }
      "4 moves" in {
        Game().playMoveList(moves take 4) must beSuccess.like {
          case g ⇒ (f << "rnbqkb1r/pp1ppppp/7n/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3") must_== g.situation
        }
      }
      "5 moves" in {
        Game().playMoveList(moves take 5) must beSuccess.like {
          case g ⇒ (f << "rnbqkb1r/pp1ppppp/7n/2p5/4P3/P4N2/1PPP1PPP/RNBQKB1R b KQkq - 0 3") must_== g.situation
        }
      }
    }
  }
}
