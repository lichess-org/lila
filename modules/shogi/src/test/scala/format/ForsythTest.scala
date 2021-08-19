package shogi
package format

import Forsyth.SituationPlus
import Pos._
import variant._

class ForsythTest extends ShogiTest {

  val f = Forsyth

  "the forsyth notation" should {
    "export" in {
      "game opening" in {
        val moves = List(C3 -> C4, G7 -> G6, B2 -> H8, G9 -> H8)
        "new game" in {
          f >> makeGame must_== "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1"
        }
        "new game board only" in {
          f exportBoard makeBoard must_== "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL"
        }
        "one move" in {
          makeGame.playMoveList(moves take 1) must beSuccess.like { case g =>
            f >> g must_== "lnsgkgsnl/1r5b1/ppppppppp/9/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL w - 2"
          }
        }
        "2 moves" in {
          makeGame.playMoveList(moves take 2) must beSuccess.like { case g =>
            f >> g must_== "lnsgkgsnl/1r5b1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL b - 3"
          }
        }
        "3 moves" in {
          makeGame.playMoveList(moves take 3) must beSuccess.like { case g =>
            f >> g must_== "lnsgkgsnl/1r5B1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/7R1/LNSGKGSNL w B 4"
          }
        }
        "4 moves" in {
          makeGame.playMoveList(moves take 4) must beSuccess.like { case g =>
            f >> g must_== "lnsgkg1nl/1r5s1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/7R1/LNSGKGSNL b Bb 5"
          }
        }
        "5 drop" in {
          makeGame.playMoveList(moves take 4) must beSuccess.like { case g =>
            g.playDrop(Bishop, E5) must beSuccess.like { case g2 =>
              f >> g2 must_== "lnsgkg1nl/1r5s1/pppppp1pp/6p2/4B4/2P6/PP1PPPPPP/7R1/LNSGKGSNL w b 6"
            }
          }
        }
      }

    }
    "import" in {
      val moves = List(C3 -> C4, G7 -> G6, B2 -> H8, G9 -> H8)
      def compare(ms: List[(Pos, Pos)], fen: String) =
        makeGame.playMoveList(ms) must beSuccess.like { case g =>
          (f << fen) must beSome.like { case situation =>
            situation.board.visual must_== g.situation.board.visual
          }
        }
      "new game" in {
        compare(
          Nil,
          "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1"
        )
      }
      "one move" in {
        compare(
          moves take 1,
          "lnsgkgsnl/1r5b1/ppppppppp/9/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL w - 2"
        )
      }
      "2 moves" in {
        compare(
          moves take 2,
          "lnsgkgsnl/1r5b1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL b - 3"
        )
      }
      "3 moves" in {
        compare(
          moves take 3,
          "lnsgkgsnl/1r5B1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/7R1/LNSGKGSNL w B 4"
        )
      }
      "4 moves" in {
        compare(
          moves take 4,
          "lnsgkg1nl/1r5s1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/7R1/LNSGKGSNL b Bb 5"
        )
      }
      "invalid" in {
        f << "hahaha" must beNone
      }
    }
  }
  "export to situation plus" should {
    "with turns" in {
      "starting" in {
        f <<< "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1" must beSome.like { case s =>
          s.turns must_== 0
        }
      }
      "sente to play" in {
        f <<< "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 11" must beSome.like { case s =>
          s.turns must_== 10
        }
      }
      "gote to play" in {
        f <<< "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 2" must beSome.like { case s =>
          s.turns must_== 1
        }
      }
    }
  }
  "pieces in hand" should {
    "read" in {
      "readHands" in {
        val h = f.readHands("-")
        h must_== Hands.init
        val h2 = f.readHands("10p25P")
        val sente: HandMap =
          Map(Rook -> 0, Bishop -> 0, Gold -> 0, Silver -> 0, Knight -> 0, Lance -> 0, Pawn -> 25)
        val gote: HandMap =
          Map(Rook -> 0, Bishop -> 0, Gold -> 0, Silver -> 0, Knight -> 0, Lance -> 0, Pawn -> 10)
        h2 must_== Hands(Hand(sente), Hand(gote))
      }
      "empty hand" in {
        f <<< "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1" must beSome.like { case s =>
          s.situation.board.crazyData must beSome.like { case d =>
            d must_== Hands.init
          }
        }
      }
      "simple hand" in {
        f <<< "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b PNr 1" must beSome.like { case s =>
          s.situation.board.crazyData must beSome.like {
            case d => {
              val sente: HandMap =
                Map(Rook -> 0, Bishop -> 0, Gold -> 0, Silver -> 0, Knight -> 1, Lance -> 0, Pawn -> 1)
              val gote: HandMap =
                Map(Rook -> 1, Bishop -> 0, Gold -> 0, Silver -> 0, Knight -> 0, Lance -> 0, Pawn -> 0)
              d must_== Hands(Hand(sente), Hand(gote))
            }
          }
        }
      }
      "hand with numbers" in {
        f <<< "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b 15P3Nr2LB7R3s230gG12pl 1" must beSome
          .like { case s =>
            s.situation.board.crazyData must beSome.like {
              case d => {
                val sente: HandMap =
                  Map(Rook -> 7, Bishop -> 1, Gold -> 1, Silver -> 0, Knight -> 3, Lance -> 2, Pawn -> 15)
                val gote: HandMap =
                  Map(Rook -> 1, Bishop -> 0, Gold -> 81, Silver -> 3, Knight -> 0, Lance -> 1, Pawn -> 12)
                d must_== Hands(Hand(sente), Hand(gote))
              }
            }
          }
      }
      "hand repeating" in {
        f <<< "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b ppBG15ppppP 1" must beSome.like {
          case s =>
            s.situation.board.crazyData must beSome.like {
              case d => {
                val sente: HandMap =
                  Map(Rook -> 0, Bishop -> 1, Gold -> 1, Silver -> 0, Knight -> 0, Lance -> 0, Pawn -> 1)
                val gote: HandMap =
                  Map(Rook -> 0, Bishop -> 0, Gold -> 0, Silver -> 0, Knight -> 0, Lance -> 0, Pawn -> 20)
                d must_== Hands(Hand(sente), Hand(gote))
              }
            }
        }
      }
      "invalid roles" in {
        f <<< "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b T13k10t 1" must beSome.like {
          case s =>
            s.situation.board.crazyData must beSome.like {
              case d => {
                d must_== Hands.init
              }
            }
        }
      }
      "open number" in {
        f <<< "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b 120 1" must beSome.like { case s =>
          s.situation.board.crazyData must beSome.like {
            case d => {
              d must_== Hands.init
            }
          }
        }
      }
      "read till correct, ignore wrong input" in {
        f <<< "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b 3P2ljk 1" must beSome.like {
          case s =>
            s.situation.board.crazyData must beSome.like {
              case d => {
                val sente: HandMap =
                  Map(Rook -> 0, Bishop -> 0, Gold -> 0, Silver -> 0, Knight -> 0, Lance -> 0, Pawn -> 3)
                val gote: HandMap =
                  Map(Rook -> 0, Bishop -> 0, Gold -> 0, Silver -> 0, Knight -> 0, Lance -> 2, Pawn -> 0)
                d must_== Hands(Hand(sente), Hand(gote))
              }
            }
        }
      }
    }
  }
}
