package shogi

import Pos._

class ImpasseTest extends ShogiTest {

  "Pieces in promotion zone" should {
    "not be enough" in {
      "starting position" in {
        val g = fenToGame("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1")
        g must beSuccess.like { case game =>
          game.situation.impasse must beFalse
          game.situation.winner must beNone
        }
      }
      "position with less than 10 other pieces in promotion zone" in {
        val g = fenToGame("2SGS4/+B+RGKG2RB/9/9/7pp/8k/9/9/9 b g2s4n4l16p 1")
        g must beSuccess.like { case game =>
          game.situation.impasse must beFalse
          game.situation.winner must beNone
        }
      }
      "position without the king in promotion zone" in {
        val g = fenToGame("2SGS4/+B+RG1G2RB/3G5/9/7pp/8k/9/9/4K4 b - 1")
        g must beSuccess.like { case game =>
          game.situation.impasse must beFalse
          game.situation.winner must beNone
        }
      }
      "position without enough value" in {
        val g = fenToGame("9/1G2K2G1/PPPPPPPPP/9/9/7ss/7sk/9/9 w 2r2b2gs4n4l9p 2")
        g must beSuccess.like { case game =>
          game.situation.impasse must beFalse
          game.situation.winner must beNone
        }
      }
      "one move away" in {
        val g = fenToGame("2SGS4/+B1GKGLLRB/3G5/9/1+R5pp/8k/9/9/9 b - 1")
        g must beSuccess.like { case game =>
          game.playMove(B5, B8) must beSuccess.like { case game2 =>
            game2.situation.impasse must beFalse
            game2.situation.winner must beNone
          }
        }
      }
      "opponent prevents impasse" in {
        val g = fenToGame("2SGS4/+B1GKGLLRB/3G5/9/1+R5pp/8k/6b2/9/9 b - 1")
        g must beSuccess.like { case game =>
          game.playMoves(B5 -> B8, G3 -> H4) must beSuccess.like { case game2 =>
            game2.situation.impasse must beFalse
            game2.situation.winner must beNone
          }
        }
      }
      "26 points for gote" in {
        val g = fenToGame("9/9/9/9/9/9/3r1lllg/+P+P1+bkssgg/K+P4ssg w r 2")
        g must beSuccess.like { case game =>
          game.situation.impasse must beFalse
          game.situation.winner must beNone
        }
      }
      "27 points for sente" in {
        val g = fenToGame("G3+R3S/GG5SS/GLPBKBPLS/9/9/7+p+p/7+pk/7+p+p/9 b - 1")
        g must beSuccess.like { case game =>
          game.situation.impasse must beFalse
          game.situation.winner must beNone
        }
      }
    }
    "be enough" in {
      "all pieces on the board" in {
        val g = fenToGame("2SGS4/+B+RGKGLLRB/3G5/9/7pp/8k/9/9/9 b - 1")
        g must beSuccess.like { case game =>
          game.situation.impasse must beTrue
          game.situation.winner must beSome.like { case color =>
            color == Sente
          }
        }
      }
      "some from pocket" in {
        val g = fenToGame("G8/4K4/PPPPPPPPP/9/9/7ss/7sk/9/9 b 2R2B 1")
        g must beSuccess.like { case game =>
          game.situation.impasse must beTrue
          game.situation.winner must beSome.like { case color =>
            color == Sente
          }
        }
      }
      "after moves" in {
        val g = fenToGame("2SGS4/+B1GKGLLRB/3G5/9/1+R5pp/8k/9/9/9 b - 1")
        g must beSuccess.like { case game =>
          game.playMoves(B5 -> B8, H5 -> H4) must beSuccess.like { case game2 =>
            game2.situation.impasse must beTrue
            game2.situation.winner must beSome.like { case color =>
              color == Sente
            }
          }
        }
      }
      "27 points for gote" in {
        val g = fenToGame("9/9/9/9/9/9/3r1llll/+P+P1+bkssgg/K+P3ssgg w r 2")
        g must beSuccess.like { case game =>
          game.situation.impasse must beTrue
          game.situation.winner must beSome.like { case color =>
            color == Gote
          }
        }
      }
      "28 points for sente" in {
        val g = fenToGame("G3+R3S/GG2P2SS/GLPBKBPLS/9/9/7+p+p/7+pk/7+p+p/9 b - 1")
        g must beSuccess.like { case game =>
          game.situation.impasse must beTrue
          game.situation.winner must beSome.like { case color =>
            color == Sente
          }
        }
      }
    }
  }
}
