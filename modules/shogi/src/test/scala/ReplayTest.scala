package shogi

class ReplayTest extends ShogiTest {

  "from prod" in {
    "replay" in {
      val fen = """lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1"""
      val moves =
        """Pc4 Pg6 Ph4 Pf6 Sf2 Rf8 Kd2 Kd8 Kc2 Kc8 Pe4 Kb8 Gf1e2 Sc8 Sd2 Pa6 Pa4 Sg8 Sd2e3 Bg7 Pg4 Sf7 Sd4 Se6 Pf4 Pf5 Pe5 Pd6 Pe6 Pe6 S*e7 Pd5 Sf8= Gf8 Sc3 Pf4 Pi4 Be5 Sg3 S*f3 R*f5 Se2= Sf4 G*f6 Re5 Sd1= Kd1 Ge5 Rf2 P*f3 Rf3 Ge4 Rf1 R*f3 Rf3 Gf3 Bc1 R*h2 S*d2 Ge2 Kc2 Gd2 Sd2 S*d1 Kc3 Se2+ G*c2 Ad2 Gd2 Rh1+ S*d1 G*e1 Sc2 S*d1 Kb2 Sc2+ Gc2 Di1 B*b4 Gf8e8 Be1 De1 Bd2 De4 S*c3 N*b5 P*e3 Df4 Sb4 B*e5 Nc3 Nc3+ Bc3 Bc3+ Sc3 L*b6 Pb4 B*g7 B*d6 Lb4 P*b3 Lb3+ Kb3 Bc3+ Kc3 N*c6 Bf4 S*e5 Be5 Pe5 L*b3 Ng7 N*d6 Gd7 Nc8+ Gc8 R*f8 S*b4 Kd2 Pd4 S*c5 Pd3+ Kd3 P*d5 Sc6 Pc6 Lb4 N*d4 Gd2 B*e7 Lb7+ Kb7 P*b6 Kc7 G*b7 Gb7 Pb7+ Kd6 N*e4 Ke6 G*f5 Nf5 S*f7"""
          .split(' ')
          .toList

      Replay.gameMoveWhileValid(moves, fen, variant.FromPosition) must beLike {
        case (_, games, None) =>
          games.size must_== 143
        case (init, games, Some(err)) =>
          println(err)
          println(init)
          games.size must_== 143
      }
    }
  }
}
