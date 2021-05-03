package chess

import chess.variant.Chess960

class PerftTest extends ChessTest {

  def perft(game: Game, depth: Int): Int = {
    if (depth > 0)
      game.situation.moves.values.flatten.foldLeft(0)((p, move) =>
        if (move.piece.role == Pawn && (move.dest.y == 1 || move.dest.y == 8))
          p + perft(game.apply(move.withPromotion(Some(Queen)).get), depth - 1)
            + perft(game.apply(move.withPromotion(Some(Rook)).get), depth - 1)
            + perft(game.apply(move.withPromotion(Some(Bishop)).get), depth - 1)
            + perft(game.apply(move.withPromotion(Some(Knight)).get), depth - 1)
        else
          p + perft(game.apply(move), depth - 1)
      )
    else 1
  }

  "calculate standard chess perfts" should {
    // source: https://marcelk.net/rookie/nostalgia/v3/perft-random.epd
    "gentest-1364" in {
      val game = Game(Some(Chess960), Some("8/4k3/2r1Br2/Pp4Q1/3N4/1p5K/1b6/8 b - -"))
      perft(game, 3) must be equalTo 18802
    }
    "gentest-2309" in {
      val game = Game(Some(Chess960), Some("7k/8/1p2p2n/Pp2P2p/7P/Q1p1P3/3NK3/4N2B b - -"))
      perft(game, 3) must be equalTo 4760
    }
    "gentest-2698" in {
      val game = Game(Some(Chess960), Some("8/8/4R3/1k6/8/5n2/Kb6/8 w - -"))
      perft(game, 3) must be equalTo 5444
    }
    "gentest-3283" in {
      val game =
        Game(Some(Chess960), Some("r1bqkb1r/p1pp1ppp/1p3n2/6B1/Pn1p2P1/7B/RPP1PP1P/1N1QK1NR w Kkq -"))
      perft(game, 3) must be equalTo 26302
    }
    "gentest-3523" in {
      val game = Game(Some(Chess960), Some("1n1K4/3bB3/2k5/2p5/8/8/3p4/8 w - -"))
      perft(game, 3) must be equalTo 933
    }
    "gentest-4253" in {
      val game = Game(Some(Chess960), Some("2Bk4/2N4N/1Q6/P2pb1P1/1R5p/2r1p2P/4K1R1/8 b - -"))
      perft(game, 3) must be equalTo 17116
    }
    "gentest-4309" in {
      val game = Game(Some(Chess960), Some("1nb1qbn1/4rk1r/ppNp3p/5ppP/P5P1/1P3P2/2PPP1RN/1R1QKB2 b - -"))
      perft(game, 3) must be equalTo 20587
    }
    "gentest-5008" in {
      val game = Game(Some(Chess960), Some("5nk1/7p/r1p4b/p5PP/K4p2/3R4/5B2/8 w - -"))
      perft(game, 3) must be equalTo 8214
    }
    "gentest-5569" in {
      val game = Game(Some(Chess960), Some("r3k1n1/p2p2p1/1p4N1/2r2pq1/2b2PPp/b1p5/P3PB1P/RQ1K1B1R b q -"))
      perft(game, 3) must be equalTo 54944
    }
    "gentest-6195" in {
      val game = Game(Some(Chess960), Some("Bnbqk2r/2pppp2/pp4p1/7p/P2b2Q1/2P1P3/1P1P1PPP/RNB2KNR w k -"))
      perft(game, 3) must be equalTo 41002
    }
    "gentest-6689" in {
      val game = Game(Some(Chess960), Some("8/k5b1/7N/2b5/8/1K3N2/8/4R3 w - -"))
      perft(game, 3) must be equalTo 17996
    }
  }

  "calculate chess960 perfts" should {
    // source: http://www.talkchess.com/forum/viewtopic.php?t=55274
    "x-fen 00" in {
      val game = Game(Some(Chess960), Some("r1k1r2q/p1ppp1pp/8/8/8/8/P1PPP1PP/R1K1R2Q w KQkq - 0 1"))
      perft(game, 3) must be equalTo 12333
    }
    "x-fen 01" in {
      val game = Game(Some(Chess960), Some("r1k2r1q/p1ppp1pp/8/8/8/8/P1PPP1PP/R1K2R1Q w KQkq - 0 1"))
      perft(game, 3) must be equalTo 20218
    }
    "x-fen 02" in {
      val game = Game(Some(Chess960), Some("8/8/8/4B2b/6nN/8/5P2/2R1K2k w Q - 0 1"))
      perft(game, 4) must be equalTo 118388
    }
    "x-fen 03" in {
      val game = Game(Some(Chess960), Some("2r5/8/8/8/8/8/6PP/k2KR3 w K - 0 1"))
      perft(game, 4) must be equalTo 57700
    }
    "x-fen 04" in {
      val game = Game(Some(Chess960), Some("4r3/3k4/8/8/8/8/6PP/qR1K1R2 w KQ - 0 1"))
      perft(game, 3) must be equalTo 12858
    }
  }

  "calculate tricky perfts" should {
    // source: https://chessprogramming.wikispaces.com/Perft+Results
    "kiwipete" in {
      val game =
        Game(Some(Chess960), Some("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"))
      perft(game, 3) must be equalTo 97862
    }
    "position 4 mirrored" in {
      val game =
        Game(Some(Chess960), Some("r2q1rk1/pP1p2pp/Q4n2/bbp1p3/Np6/1B3NBn/pPPP1PPP/R3K2R b KQ - 0 1"))
      perft(game, 3) must be equalTo 9467
    }
    // https://github.com/ornicar/lila/issues/4625
    "h-side rook blocks a-side castling" in {
      val game =
        Game(Some(Chess960), Some("4rrk1/pbbp2p1/1ppnp3/3n1pqp/3N1PQP/1PPNP3/PBBP2P1/4RRK1 w Ff - 0 1"))
      perft(game, 3) must be equalTo 71908
    }
  }
}
