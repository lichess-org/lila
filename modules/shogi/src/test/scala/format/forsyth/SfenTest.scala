package shogi
package format
package forsyth

import Pos._
import variant.{ Minishogi, Standard }

import cats.syntax.option._

class SfenTest extends ShogiTest {

  "the forsyth notation" should {
    "export" in {
      "game opening" in {
        val moves = List((SQ7G, SQ7F, false), (SQ3C, SQ3D, false), (SQ8H, SQ2B, false), (SQ3A, SQ2B, false))
        "new game" in {
          makeGame.toSfen must_== Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1")
        }
        "new game board only" in {
          Sfen(Sfen.boardToString(makeSituation.board, Standard)) must_== Sfen(
            "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL"
          )
        }
        "one move" in {
          makeGame.playMoveList(moves take 1) must beValid.like { case g =>
            g.toSfen must_== Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL w - 2")
          }
        }
        "2 moves" in {
          makeGame.playMoveList(moves take 2) must beValid.like { case g =>
            g.toSfen must_== Sfen("lnsgkgsnl/1r5b1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL b - 3")
          }
        }
        "3 moves" in {
          makeGame.playMoveList(moves take 3) must beValid.like { case g =>
            g.toSfen must_== Sfen("lnsgkgsnl/1r5B1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/7R1/LNSGKGSNL w B 4")
          }
        }
        "4 moves" in {
          makeGame.playMoveList(moves take 4) must beValid.like { case g =>
            g.toSfen must_== Sfen("lnsgkg1nl/1r5s1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/7R1/LNSGKGSNL b Bb 5")
          }
        }
        "5 drop" in {
          makeGame.playMoveList(moves take 4) must beValid.like { case g =>
            g.playDrop(Bishop, SQ5E) must beValid.like { case g2 =>
              g2.toSfen must_== Sfen("lnsgkg1nl/1r5s1/pppppp1pp/6p2/4B4/2P6/PP1PPPPPP/7R1/LNSGKGSNL w b 6")
            }
          }
        }
      }

    }
    "import" in {
      val moves = List((SQ7G, SQ7F, false), (SQ3C, SQ3D, false), (SQ8H, SQ2B, false), (SQ3A, SQ2B, false))
      def compare(ms: List[(Pos, Pos, Boolean)], sfen: Sfen) =
        makeGame.playMoveList(ms) must beValid.like { case g =>
          sfen must_== g.toSfen
        }
      "new game" in {
        compare(
          Nil,
          Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1")
        )
      }
      "one move" in {
        compare(
          moves take 1,
          Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL w - 2")
        )
      }
      "2 moves" in {
        compare(
          moves take 2,
          Sfen("lnsgkgsnl/1r5b1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL b - 3")
        )
      }
      "3 moves" in {
        compare(
          moves take 3,
          Sfen("lnsgkgsnl/1r5B1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/7R1/LNSGKGSNL w B 4")
        )
      }
      "4 moves" in {
        compare(
          moves take 4,
          Sfen("lnsgkg1nl/1r5s1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/7R1/LNSGKGSNL b Bb 5")
        )
      }
      "invalid" in {
        Sfen("hahaha").toSituation(Standard) must beNone
      }
    }
    "promoted pieces in sfen" in {
      Sfen("+l+n+sgkg+s+n+l/1r5+b1/+p+p+p+p+p+p+p+p+p/9/9/9/PPPP+PPPPP/1B5R1/LNSGKGSNL b - 1").toSituation(
        Standard
      ) must beSome
        .like { case s =>
          val ps = s.board.pieces.values.toList
          ps.count(_.role == Tokin) must_== 10
          ps.count(_.role == PromotedLance) must_== 2
          ps.count(_.role == PromotedKnight) must_== 2
          ps.count(_.role == PromotedSilver) must_== 2
          ps.count(_.role == Horse) must_== 1
        }
    }
  }
  "export to situation plus" should {
    "with plies" in {
      "starting" in {
        Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1").toSituationPlus(
          Standard
        ) must beSome.like { case s =>
          s.situation.color must_== Sente
          s.plies must_== 0
          s.moveNumber must_== 1
        }
      }
      "sente to play" in {
        Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 11").toSituationPlus(
          Standard
        ) must beSome.like { case s =>
          s.situation.color must_== Sente
          s.plies must_== 10
          s.moveNumber must_== 11
        }
      }
      "gote to play" in {
        Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 2").toSituationPlus(
          Standard
        ) must beSome.like { case s =>
          s.situation.color must_== Gote
          s.plies must_== 1
          s.moveNumber must_== 2
        }
      }
      "gote to play starting at 1" in {
        Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 1").toSituationPlus(
          Standard
        ) must beSome.like { case s =>
          s.situation.color must_== Gote
          s.plies must_== 1
          s.moveNumber must_== 1
        }
      }
    }
  }
  "make game" should {
    "sente starts - 1" in {
      val sfen = Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1")
      val game = Game(None, sfen.some)
      game.plies == 0
      game.startedAtPly == 0
      game.startedAtMove == 1
      game.toSfen must_== sfen
    }
    "sente starts - 2" in {
      val sfen = Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 2")
      val game = Game(None, sfen.some)
      game.plies == 2
      game.startedAtPly == 2
      game.startedAtMove == 2
      game.toSfen must_== sfen
    }
    "gote starts - 1" in {
      val sfen = Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 1")
      val game = Game(None, sfen.some)
      game.plies == 1
      game.startedAtPly == 1
      game.startedAtMove == 1
      game.toSfen must_== sfen
    }
    "gote starts - 2" in {
      val sfen = Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 2")
      val game = Game(None, sfen.some)
      game.plies == 1
      game.startedAtPly == 1
      game.startedAtMove == 2
      game.toSfen must_== sfen
    }
  }
  "pieces in hand" should {
    "read" in {
      "makeHands" in {
        Sfen.makeHandsFromString("", Standard) must beSome.like { case hs =>
          hs must_== Hands.empty
        }
        Sfen.makeHandsFromString("-", Standard) must beSome.like { case hs =>
          hs must_== Hands.empty
        }
        Sfen.makeHandsFromString("10p25P", Standard) must beSome.like { case hs =>
          hs must_== Hands(Hand(Map(Pawn -> 25)), Hand(Map(Pawn -> 10)))
        }
      }
      "empty hand" in {
        Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1").toSituationPlus(
          Standard
        ) must beSome.like { case s =>
          s.situation.hands must_== Hands.empty
        }
      }
      "simple hand" in {
        Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b PNr 1").toSituationPlus(
          Standard
        ) must beSome.like { case s =>
          s.situation.hands must_== Hands(Hand(Map(Knight -> 1, Pawn -> 1)), Hand(Map(Rook -> 1)))
        }
      }
      "hand with numbers" in {
        Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b 15P3Nr2LB7R3s230gG12pl 1")
          .toSituationPlus(Standard) must beSome
          .like { case s =>
            s.situation.hands must_== Hands(
              Hand(Map(Rook -> 7, Bishop -> 1, Gold -> 1, Knight -> 3, Lance -> 2, Pawn -> 15)),
              Hand(Map(Rook -> 1, Gold -> 81, Silver -> 3, Lance -> 1, Pawn -> 12))
            )
          }
      }
      "hand repeating" in {
        Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b ppBG15ppppP 1").toSituationPlus(
          Standard
        ) must beSome.like { case s =>
          s.situation.hands must_== Hands(Hand(Map(Bishop -> 1, Gold -> 1, Pawn -> 1)), Hand(Map(Pawn -> 20)))
        }
      }
      "invalid roles" in {
        Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b T13k10t 1").toSituationPlus(
          Standard
        ) must beNone
      }
      "open number" in {
        Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b 120 1").toSituationPlus(
          Standard
        ) must beSome.like { case s =>
          s.situation.hands must_== Hands.empty
        }
      }
      "ignore wrong input" in {
        Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b 3P2ljk 1").toSituationPlus(
          Standard
        ) must beNone
      }
    }
  }
  "minishogi" in {
    "default" in {
      Sfen("rbsgk/4p/5/PG3/K1SBR").toSituation(Minishogi) must beSome.like { case s =>
        s.toSfen.truncate must_== Sfen("rbsgk/4p/5/PG3/K1SBR b -")
      }
    }
    "too many ranks" in {
      Sfen("rbsgk/4p/5/PG3/K1SBR/PG3").toSituation(Minishogi) must beNone
    }
    "too many files" in {
      Sfen("rrbsgk/4p/5/PG3/K1SBR/PG3").toSituation(Minishogi) must beNone
    }
  }
}
