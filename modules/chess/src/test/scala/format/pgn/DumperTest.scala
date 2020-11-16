package chess
package format.pgn

import format.Forsyth
import Pos._

class DumperTest extends ChessTest {

  "Check with pawn" should {
    "not be checkmate if pawn can be taken en passant" in {
      val game = Forsyth.<<<("8/3b4/6R1/1P2kp2/6pp/2N1P3/4KPPP/8 w - -").get match {
        case Forsyth.SituationPlus(sit, turns) =>
          Game(
            sit,
            turns = turns
          )
      }
      val move = game(Pos.F2, Pos.F4).toOption.get._2
      Dumper(move) must_== "f4+"
    }
  }

  val gioachineGreco = makeGame.playMoves(
    D2 -> D4,
    D7 -> D5,
    C2 -> C4,
    D5 -> C4,
    E2 -> E3,
    B7 -> B5,
    A2 -> A4,
    C7 -> C6,
    A4 -> B5,
    C6 -> B5,
    D1 -> F3
  )

  val peruvianImmortal = makeGame.playMoves(
    E2 -> E4,
    D7 -> D5,
    E4 -> D5,
    D8 -> D5,
    B1 -> C3,
    D5 -> A5,
    D2 -> D4,
    C7 -> C6,
    G1 -> F3,
    C8 -> G4,
    C1 -> F4,
    E7 -> E6,
    H2 -> H3,
    G4 -> F3,
    D1 -> F3,
    F8 -> B4,
    F1 -> E2,
    B8 -> D7,
    A2 -> A3,
    E8 -> C8,
    A3 -> B4,
    A5 -> A1,
    E1 -> D2,
    A1 -> H1,
    F3 -> C6,
    B7 -> C6,
    E2 -> A6
  )

  "standard game" should {
    "move list" in {
      "Gioachine Greco" in {
        gioachineGreco map (_.pgnMoves) must beSuccess.like {
          case ms => ms must_== "d4 d5 c4 dxc4 e3 b5 a4 c6 axb5 cxb5 Qf3".split(' ').toList
        }
      }
      "Peruvian Immortal" in {
        peruvianImmortal map (_.pgnMoves) must beSuccess.like {
          case ms =>
            ms must_== "e4 d5 exd5 Qxd5 Nc3 Qa5 d4 c6 Nf3 Bg4 Bf4 e6 h3 Bxf3 Qxf3 Bb4 Be2 Nd7 a3 O-O-O axb4 Qxa1+ Kd2 Qxh1 Qxc6+ bxc6 Ba6#"
              .split(' ')
              .toList
        }
      }
    }
  }

  "dump a promotion move" should {
    "without check" in {
      val game = Game("""

P    k




PP   PPP
KNBQ BNR
""")
      game.playMoves(A7 -> A8) map (_.pgnMoves) must beSuccess.like {
        case ms => ms must_== List("a8=Q")
      }
    }
    "with check" in {
      val game = Game("""
    k
P




PP   PPP
KNBQ BNR
""")
      game.playMoves(A7 -> A8) map (_.pgnMoves) must beSuccess.like {
        case ms => ms must_== List("a8=Q+")
      }
    }
    "with checkmate" in {
      val game = Game("""
    k
P  ppp




PP   PPP
KNBQ BNR
""")
      game.playMoves(A7 -> A8) map (_.pgnMoves) must beSuccess.like {
        case ms => ms must_== List("a8=Q#")
      }
    }
    "castle kingside" in {
      Game("""
PP   PPP
R   K  R
""").playMoves(E1 -> G1) map (_.pgnMoves) must beSuccess.like {
        case ms => ms must_== List("O-O")
      }
    }
    "castle queenside" in {
      Game("""
PP   PPP
R   K  R
""").playMoves(E1 -> C1) map (_.pgnMoves) must beSuccess.like {
        case ms => ms must_== List("O-O-O")
      }
    }
  }

  "ambiguous moves" should {
    "ambiguous file only" in {
      val game = Game("""
k





P   K  P
R      R
""")
      game.playMoves(H1 -> B1) map (_.pgnMoves) must beSuccess.like {
        case ms => ms must_== List("Rhb1")
      }
    }
    "ambiguous rank only" in {
      val game = Game("""
k


 N


    K  P
 N
""")
      game.playMoves(B5 -> C3) map (_.pgnMoves) must beSuccess.like {
        case ms => ms must_== List("N5c3")
      }
    }
    "ambiguous file and rank" in {
      val game = Game("""


  QQ
  Q


    K
k
""")
      game.playMoves(C6 -> D5) map (_.pgnMoves) must beSuccess.like {
        case ms => ms must_== List("Qc6d5")
      }
    }
    "unambiguous file" in {
      val game = Game("""
k





P      P
R   K  R
""")
      game.playMoves(H1 -> F1) map (_.pgnMoves) must beSuccess.like {
        case ms => ms must_== List("Rf1")
      }
    }
    "unambiguous rank" in {
      val game = Game("""
k

   KRq

    R



""")
      game.playMoves(E4 -> E5) map (_.pgnMoves) must beSuccess.like {
        case ms => ms must_== List("Re5")
      }
    }
  }

  "chess960" should {
    "castle queenside as white" in {
      Game(makeBoard("""
PPPPPPPP
NRK RQBB
""", variant.Chess960)).playMoves(C1 -> B1) map (_.pgnMoves) must beSuccess.like {
        case ms => ms must_== List("O-O-O")
      }
    }
    "castle kingside as white" in {
      Game(makeBoard("""
PP PPPPP
NRK R  B
""", variant.Chess960)).playMoves(C1 -> E1) map (_.pgnMoves) must beSuccess.like {
        case ms => ms must_== List("O-O")
      }
    }
    "castle queenside as black" in {
      Game(makeBoard("""
nrk rqbb
pppppppp




PPPPPPPP
NRK RQBB
""", variant.Chess960)).withPlayer(Black).playMoves(C8 -> B8) map (_.pgnMoves) must beSuccess.like {
        case ms => ms must_== List("O-O-O")
      }
    }
    "castle kingside as black" in {
      Game(makeBoard("""
nrk r  b
pppppppp




PPPPPPPP
NRK RQBB
""", variant.Chess960)).withPlayer(Black).playMoves(C8 -> E8) map (_.pgnMoves) must beSuccess.like {
        case ms => ms must_== List("O-O")
      }
    }
    "opening with castles" in {
      Game(makeBoard("""
nrknrqbb
pppppppp




PPPPPPPP
NRKNRQBB
""", variant.Chess960)).playMoves(
        F2 -> F4,
        D8 -> C6,
        D1 -> C3,
        G7 -> G6,
        C3 -> B5,
        C8 -> B8,
        C1 -> B1
      ) map (_.pgnMoves) must beSuccess.like {
        case ms => ms must_== "f4 Nc6 Nc3 g6 Nb5 O-O-O O-O-O".split(' ').toList
      }
    }
    "tricky rook disambiguation" in {
      val fen           = """r5k1/1b5p/N3p1p1/Q4p2/4r3/2P1q3/1PK2RP1/5R2 w - - 1 38"""
      val sit           = chess.format.Forsyth.<<(fen).get
      val game1         = Game(sit.board, sit.color)
      val (game2, move) = game1(Pos.F2, Pos.F3).toOption.get
      Dumper(game1.situation, move, game2.situation) must_== "Rf3"
    }
  }
  "move comment" should {
    "simple" in {
      Move("e4", List("Some comment")).toString must_== "e4 { Some comment }"
    }
    "one line break" in {
      Move("e4", List("""Some
comment""")).toString must_== """e4 { Some
comment }"""
    }
    "two line breaks" in {
      Move("e4", List("""Some

comment""")).toString must_== """e4 { Some
comment }"""
    }
    "three line breaks" in {
      Move("e4", List("""Some


comment""")).toString must_== """e4 { Some
comment }"""
    }
  }
}
