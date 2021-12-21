package shogi

class PerftTest extends ShogiTest {

  def findPossibleDrops(role: Role, sit: Situation): List[Drop] =
    sit.drops
      .getOrElse {
        sit.board.variant.allSquares.filterNot(sit.board.pieces contains _)
      }
      .flatMap { p =>
        // pawn drop checkmate is not really legal
        if (role == Pawn && Situation(sit.board.place(Piece(sit.color, Pawn), p).get, !sit.color).checkMate)
          None
        else sit.drop(role, p).toOption
      }

  def dropMoves(game: Game): List[Drop] =
    game.situation.board.handData.fold(List[Drop]()) { hands: Hands =>
      hands(game.situation.color).handMap.foldLeft(List[Drop]()) { case (acc, cur) =>
        if (cur._2 > 0)
          acc ::: findPossibleDrops(cur._1, game.situation)
        else acc
      }
    }

  def perft(game: Game, depth: Int): Int = {
    if (depth > 0) {
      val moves: List[Move] = game.situation.moves.values.flatten.toList
      val drops: List[Drop] = dropMoves(game)
      val movesOrDrops: List[Either[Move, Drop]] =
        moves.map { m: Move => Left(m) } ::: drops.map { d => Right(d) }
      movesOrDrops.foldLeft(0) { (p, md) =>
        md match {
          case Left(m)  => p + perft(game.apply(m), depth - 1)
          case Right(d) => p + perft(game.applyDrop(d), depth - 1)
        }
      }
    } else 1
  }

  "starting position" should {
    "1 depth" in {
      val game =
        Game(
          Option(shogi.variant.Standard),
          Option("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1")
        )
      perft(game, 1) must be equalTo 30
    }
    "2 depth" in {
      val game =
        Game(
          Option(shogi.variant.Standard),
          Option("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1")
        )
      perft(game, 2) must be equalTo 900
    }
    "3 depth" in {
      val game =
        Game(
          Option(shogi.variant.Standard),
          Option("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1")
        )
      perft(game, 3) must be equalTo 25470
    }
    //"4 depth" in {
    //  val game =
    //    Game(
    //      Option(shogi.variant.Standard),
    //      Option("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1")
    //    )
    //  perft(game, 4) must be equalTo 719731
    //}
  }

  "calculate minishogi perfts" should {
    val game = Game(Option(shogi.variant.Minishogi), Option("rbsgk/4p/5/P4/KGSBR b - 1"))
    "1 depth" in {
      perft(game, 1) must be equalTo 14
    }
    "2 depth" in {
      perft(game, 2) must be equalTo 181
    }
    "3 depth" in {
      perft(game, 3) must be equalTo 2512
    }
    "4 depth" in {
      perft(game, 4) must be equalTo 35401
    }
    //"5 depth" in {
    //  perft(game, 5) must be equalTo 533203
    //}
  }

  val random: List[List[String]] = List(
    List("l2kg2+R1/4n3+L/p1gpps3/4np3/6P1N/PP+rP2pS1/1pG2P3/4P1G2/LN3KB1+p b SPbslpppp", "107", "20080"),
    List("l+Rl2+R3/3k1s2+L/p1p1p4/2Ppnp1S1/4n1Pbp/PP2G4/1G3P+n2/Kp2P4/L8 w GSPPPPbgsnp", "240", "39392"),
    List("l+Rl2g2+R/3k1s2+L/p1p1p4/2Ppnp1S1/4n1Pbp/PP2G4/1G3P+n2/Kp2P4/L8 b SPPPPbgsnp", "110", "24582"),
    List("l2kgb1+R1/4n3+L/p1gpps3/4np3/6P1N/PPGP2pS1/1p3P3/4P1G2/LN3KB1+p b RSPslpppp", "158", "21443"),
    List("l8/1r1gk3+L/p1Npp2S1/2Psnpg2/5+r2N/PP1P2pS1/1pG1BP3/4P1G2/LN3KB1+p b SPPlppp", "109", "10902"),
    List("lr6l/3g1kg2/p2pp2s+P/2Ps1ppp1/8L/P1nP2PR1/1P3PS2/1SGK5/LN3G3 b BNNPPPbpp", "7", "755"),
    List("lr7/3g1kg2/p2pp2s+L/2Ps1ppp1/9/PP1P1BPS1/5P1PS/1G6+r/LN3KB2 b GNNLPPPnpp", "184", "18331"),
    List("l+Rl2g2+R/2p+Sks2+L/p3p4/2Ppnp1S1/4n1Pbp/PP2G4/1G3P+n2/Kp2P4/L8 w GSPPPPbnp", "2", "326"),
    List("l2s5/3kn2R+L/p1gpp+B3/4np3/2+r3P1N/PP1P2pS1/1pG2P3/4P1G2/LN3KB1+p b GLPsspppp", "165", "16018"),
    List("lr7/3g1kg2/p2pp2s+L/2Psnp1p1/5+rS2/PP1P5/4BPS2/1G2P1G2/LN3KB1L w NNPPPpppp", "72", "6928"),
    List("lr7/3g1kg2/p2pp2s+L/2Ps1ppp1/9/P2P1BPS1/1P3P3/1G3B3/LN2KG1r1 w NNNLPPPsppp", "123", "16661"),
    List("l+Rl2+R3/3k1s2+L/p1p1p4/2Ppnp1S1/4n1Pbp/PP2G4/KG3P+n2/1p2P4/Ls7 w GSPPPPbgnp", "188", "30364"),
    List("lr6l/3g1kg2/p2pp2s+P/2Ps1ppp1/7nL/P2P2PR1/1P3PN2/1SGK2S2/LN3G3 w BNPPPbpp", "109", "14585"),
    List("l1S6/r3k3+L/p1gppl3/4np+B2/2+r3P1N/PP1P2pS1/1pG2P3/4P1G2/LN3KB1+p b GSPsnpppp", "163", "21883"),
    List("lr7/3g1kg2/p2pp2sl/2Ps1ppp1/8L/P2P2PR1/1P3PS2/1G7/LN2KG3 b BNNNPPPbsppp", "145", "23120"),
    List("l8/1r1g1k3/p1Npp1gs+L/2Psnp1p1/5+rS2/PP1P2pS1/1pG1BP3/4P1G1p/LN3KB1L b NPPpp", "84", "4399"),
    List("lr7/3g1kg2/p2pp2s+L/2Ps1p1p1/5+rp2/PP1P1B1S1/5PP1S/1G2P1G2/LN3KB1L b NNPnpppp", "86", "8763"),
    List("l2kgs1+R1/4n+B2+L/p1gpp4/4np3/6P1N/PP+rP2pS1/1pG2P3/4P1G2/LN3KB1+p w SSPlpppp", "95", "10625"),
    List("l8/1r1gk3+L/p1Npp4/2Psnp+r2/8N/PP1P2pS1/1pG1BP3/4P1G2/LN3KB1+p b GSPPslppp", "156", "22669"),
    List("lr7/3g1kg2/p2pp2s+L/2Ps1ppp1/9/PP1P1BPS1/5P1P1/1G3B1g1/LN3KN1+r b SNNLPPPpp", "181", "12037"),
    List("7lk/9/8S/9/9/9/9/7L1/8K b P", "85", "639")
  )

  "random positions" should {
    "forall" in {
      forall(random) { line =>
        line match {
          case sfen :: d1 :: d2 :: _ => {
            val game =
              Game(Option(shogi.variant.Standard), Option(sfen))
            perft(game, 1) must be equalTo d1.toInt
            //perft(game, 2) must be equalTo d2.toInt
          }
        }
      }
    }
  }
}
