package lila.chess
package format

import Pos._

class PgnReaderTest extends ChessTest {

  val checkmates = List(
    "e3 Nc6 d4 Nf6 c3 e5 dxe5 Nxe5 Bb5 a6 Ba4 b5 Bb3 d5 e4 dxe4 f4 Qxd1+ Kxd1 Nd3 Be3 Ng4 Bd4 Ngf2+ Bxf2 Nxf2+ Ke1 Nxh1 Bd5 Ra7 Bc6+ Kd8 Bxe4 Bd6 g3 Re8 Nd2 f5 Ne2 fxe4 Kf1 e3 Kg2 exd2 Rxh1 Bb7+ Kf2 Bc5+ Kf1 d1=Q#",
    "e3 Nf6 Ng3 Ne6 Nf3 d5 Nd4 Nxd4 exd4 e6 Re1 Ng4 Re2 f6 c4 dxc4 Be4 Rxd4 Bf3 Ne5 Ne4 Nxf3 gxf3 Bf7 Nxf6 gxf6 Kd1 e5 h4 Bg6 Bh2 Bf5 Rxe5 fxe5 Bxe5 Qxe5 Qf1 Qf4 d3 Rxd3+ Ke1 Qd2#",
    "c4 Nc6 e3 Nf6 h3 Ne4 d3 Nc5 a3 Ne5 d4 d6 dxe5 dxe5 b4 Qxd1+ Kxd1 Ne4 f3 Nf2+ Ke2 Nxh1 Nd2 Ng3+ Ke1 Bf5 Bd3 Bxd3 Rb1 Bxb1 Nxb1 Rd8 Bd2 e6 h4 Be7 Nh3 Bxh4 Nf2 Ke7 Bc3 f6 Nd2 h5 c5 g5 Nc4 Rhg8 Na5 Nh1 Ke2 Nxf2 Be1 Nd3 Nxb7 Bxe1 Nxd8 Rxd8 c6 a5 bxa5 Bxa5 a4 f5 Kd1 Nf4+ Kc2 Rd2+ Kc1 Nxg2 Kb1 Nxe3 Kc1 h4 Kb1 h3 Kc1 h2 Kb1 h1=Q#",
    "d4 d5 c4 e6 a3 Nf6 Nf3 dxc4 Nc3 c5 Qa4+ Nc6 dxc5 Bxc5 b4 cxb3 Qxb3 O-O Bg5 Be7 e3 b6 Bb5 Bb7 O-O Nd5 Nxd5 Bxg5 Nxg5 Qxg5 Bxc6 Bxc6 Nb4 Qxg2#",
    "d4 Nf6 c4 Nc6 Nc3 e5 Nd5 Nxd5 cxd5 Nxd4 e3 Nf5 e4 Nd4 h4 Qf6 Bg5 Qb6 b3 h6 Bc4 hxg5 h5 Bc5 Ne2 Qa5+ Kf1 d6 Nxd4 Bxd4 Rc1 Qxa2 Rc2 Qa5 Qc1 g4 h6 g3 f3 gxh6 Rxh6 Rxh6 Qxh6 Bf2 Qh8+ Kd7 Qf8 Qe1#",
    "e4 Nc6 d4 Nf6 d5 Ne5 Nf3 Nxf3+ gxf3 e6 Nd2 exd5 e5 Qe7 c4 Qxe5+ Be2 d4 Rg1 d3 b4 Qxa1 Bb2 Qxb2 Qc2 dxc2 Nb1 Qc1+ Bd1 d1=Q#",
    "e4 d5 exd5 Nf6 Nc3 Nxd5 Nxd5 Qxd5 Nf3 Bg4 c4 Bxf3 Qxf3 Qe6+ Be2 Nc6 O-O Nd4 Qxb7 Nxe2+ Kh1 Rd8 Re1 Qg4 Qc6+ Rd7 Qa8+ Rd8 Qc6+ Qd7 Qxd7+ Rxd7 Rxe2 e6 b3 Bb4 Bb2 f5 Bc3 Bxc3 dxc3 Rd3 Rxe6+ Kf7 Re3 Rd2 a4 Rhd8 b4 Rd1+ Re1 Rxe1+ Rxe1 Rd3 Re3 Rd1+ Re1 Rxe1#",
    "d4 d5 c4 b6 cxd5 Na6 e4 Nf6 Bb5+ Bd7 Bxa6 Ba4 Qxa4+ c6 Qxc6+ Nd7 e5 Rc8 Bxc8 Qxc8 Qxc8#",
    "Nc3 c6 Nf3 Na6 b4 Nxb4 Rb1 c5 a3 Nxc2+ Qxc2 b6 Nb5 Ba6 Qa4 Bxb5 Rxb5 Nf6 Bb2 Nd5 Qg4 Nc7 Bxg7 Bxg7 Qxg7 Rf8 Rb3 Ne6 Qxh7 Qb8 Re3 f6 Qg6+ Rf7 g3 Nf8 Qg8 e5 d4 d6 dxc5 Qc7 cxd6 Qc1#",
    "d4 Nf6 Nf3 Nc6 Nbd2 e6 e4 d6 c4 Qe7 Bd3 e5 d5 Nd4 Nxd4 exd4 O-O Bg4 f3 Bd7 Nb3 Qe5 Be2 c5 dxc6 bxc6 Qxd4 Qxd4+ Nxd4 Rb8 b3 Be7 Be3 Bd8 Rfd1 Bb6 Kf2 Ke7 Rd2 Ba5 Rd3 Bb6 Rad1 Rhd8 g4 h6 Bf4 g5 Bg3 h5 h3 h4 Bh2 Rb7 e5 dxe5 Bxe5 Ne8 Kg2 Bc7 Bxc7 Rxc7 Nf5+ Kf8 f4 gxf4 Nxh4 Ng7 Bf3 Ne6 Nf5 Nc5 Rd4 Ne6 Rd6 c5 h4 Ng7 Nxg7 Kxg7 g5 a5 Kf2 Kf8 Bc6 Ke7 Ba4 Bxa4 Rxd8 Bc6 h5 Ke6 h6 Be4 Rh8 Re7 Re1 Kf5 h7 Kg6 Rc8 Kxh7 Rxc5 a4 b4 Kg6 b5 f6 gxf6 Kxf6 b6 a3 Rc7 Rxc7 bxc7 Bb7 Re8 Kf5 c5 Ba6 Ra8 Bb7 Rf8+ Ke5 c6 Ba6 Ra8 Kd6 Rxa6 Kxc7 Kf3 Kb8 Kxf4 Kc8 Ke5 Kc7 Ke6 Kd8 Kd6 Ke8 Ra7 Kf8 c7 Kf7 c8=Q+ Kg6 Qg4+ Kf6 Ra8 Kf7 Qf5+ Kg7 Ra7+ Kg8 Qc8#",
    "e3 Nc6 Nf3 Nf6 Nc3 e6 a3 Bd6 d4 Ng4 h3 Nf6 Bb5 a6 Bxc6 dxc6 e4 Nd7 O-O h5 h4 c5 Bg5 f6 Be3 cxd4 Bxd4 c5 Be3 Qe7 g3 Ne5 Nxe5 Bxe5 Qe2 Bxc3 bxc3 Kf7 Rad1 b5 c4 Rb8 Rd2 Bb7 Rfd1 Bc6 Kg2 Bxe4+ Kf1 Bc6 Bf4 e5 Be3 Qe6 Bxc5 Qh3+ Ke1 Qh1+ Qf1 Qe4+ Re2 Qxc4 Bd6 Rbd8 Bb4 Bf3 Rxd8 Rxd8 Re3 Rd1#",
    "e4 e5 Bc4 Nf6 d3 d5 Bg5 Be6 exd5 Bxd5 Bxd5 Qxd5 f3 e4 Bxf6 gxf6 fxe4 Qa5+ c3 Nd7 b4 Qb6 a4 Ne5 Nf3 O-O-O a5 Qe6 Nd4 Qd7 Qh5 Bxb4 cxb4 Qxd4 Qf5+ Kb8 Kd2 Nc4+ Ke2 Qxd3+ Kf2 Qe3+ Kf1 Rd1#",
    "h4 d5 d4 Qd6 f4 Qb4+ c3 Qb6 b4 Qc6 Nf3 Bf5 Nfd2 Qg6 Rg1 Qg3#",
    "d4 d5 e3 e6 f3 Bd6 c3 c6 b4 b5 g4 g5 h3 Bg3+ Ke2 f6 Kd2 Qd6 Qe2 a5 a4 axb4 cxb4 Qxb4+ Kd1 bxa4 Qg2 Qe1+ Kc2 Bf2 Bd2 Bxe3 Qe2 Qh4 Qxe3 Nd7 Be1 Qh6 Bd3 a3 Nxa3 Nb6 Rb1 Nd7 Rb3 Ba6 Kb2 Ke7 Bb4+ Kf7 Be1 Qf8 h4 gxh4 Rxh4 Bb7 Rxb7 Qxa3+ Kc2 Qa2+ Kc3 Ra3+ Kb4 Qb2#",
    "d4 Nc6 Nc3 Nf6 e4 d6 Bb5 Bg4 f3 Be6 d5 Nxe4 dxc6 Nxc3 cxb7+ Nxb5 a8=Q Qxa8 Qd3 c6 Bd2 Bd5 c4 Nc7 cxd5 Nxd5 Rc1 e5 Ne2 Be7 O-O O-O h3 Rb8 b3 Bh4 Kh2 Kh8 g3 Bf6 h4 a5 Kh3 a4 Rc4 axb3 axb3 Qa6 Rb1 Ra8 b4 Qb5 Nc3 Nxc3 Qxc3 e4 Qc2 Qf5+ g4 Qxf3+ Kh2 Qf2+ Kh1 Qxh4+ Kg2 Qxg4+ Kf1 Qf3+ Ke1 Bh4#",
    "e4 Nc6 Nc3 Nf6 Bc4 e6 Nf3 Bc5 d4 Bd6 Qe2 Ng4 h3 Nf6 e5 Bb4 exf6 Qxf6 Be3 Qf5 Bd3 Qd5 a3 Qa5 O-O Bxc3 bxc3 Qxc3 Rac1 Qxa3 d5 exd5 Bc5+ Kd8 Bxa3 Re8 Qd2 h6 Bb2 f6 Bg6 Re4 Rce1 Rxe1 Rxe1 d6 Qxd5 a5 Qg8+ Kd7 Qe8#",
    "d3 Nc6 e3 Nf6 Be2 d6 Bf3 Be6 Bxc6+ bxc6 e4 Rb8 d4 Nxe4 f3 Nf6 Nh3 Bc4 Ng5 Nd5 a3 e6 f4 Be7 f5 Bxg5 h4 Bxh4+ Rxh4 Qxh4+ g3 Qxg3+ Kd2 Qe3#",
    "c3 Nc6 Qc2 Nf6 e3 d6 Bd3 Bg4 h3 Be6 Nf3 Qd7 g4 h5 g5 Nd5 g6 Bxh3 gxf7+ Kd8 Na3 Qg4 Be4 Qg2 Rg1 Nxe3 dxe3 Nb4 cxb4 Qxg1+ Nxg1 Bc8 Nb5 c6 Bxc6 bxc6 Qxc6 Rb8 Qc7#",
    "e3 d5 Qe2 e5 Nc3 Bc5 Nf3 Bg4 h3 Bxf3 gxf3 Nf6 f4 e4 Bg2 O-O d3 exd3 cxd3 c6 e4 d4 Na4 Bb4+ Bd2 Bxd2+ Qxd2 b6 e5 Nd5 Rc1 Qd7 b4 Na6 Rb1 b5 Bxd5 cxd5 Nb2 Rae8 Rg1 Qxh3 Nd1 f6 a4 Nc7 axb5 Nxb5 Ra1 fxe5 Nb2 exf4+ Kd1 Nc3+ Kc2 f3 Rge1 Rc8 Rxa7 Ne4+ Kb3 Nxd2+ Ka4 Qh6 Re5 Qb6 Rae7 Ra8+ Ra7 Rxa7#",
    "e4 e5 Nf3 Nc6 Nc3 Bb4 Nd5 Nf6 Nxb4 Nxb4 c3 Nc6 Nxe5 Nxe5 d4 Ng6 Bg5 h6 Bxf6 Qxf6 e5 Qe6 Bd3 d6 Qe2 Nf4 Qe4 dxe5 Bb5+ c6 d5 Nxd5 Bc4 O-O O-O b5 Bb3 Bb7 Bc2 Nf4 Qh7#")

  val checkmate = "d4 Nf6 Nf3 Nc6 Nbd2 e6 e4 d6 c4 Qe7 Bd3 e5 d5 Nd4 Nxd4 exd4 O-O Bg4 f3 Bd7 Nb3 Qe5 Be2 c5 dxc6 bxc6 Qxd4 Qxd4+ Nxd4 Rb8 b3 Be7 Be3 Bd8 Rfd1 Bb6 Kf2 Ke7 Rd2 Ba5 Rd3 Bb6 Rad1 Rhd8 g4 h6 Bf4 g5 Bg3 h5 h3 h4 Bh2 Rb7 e5 dxe5 Bxe5 Ne8 Kg2 Bc7 Bxc7 Rxc7 Nf5+ Kf8 f4 gxf4 Nxh4 Ng7 Bf3 Ne6 Nf5 Nc5 Rd4 Ne6 Rd6 c5 h4 Ng7 Nxg7 Kxg7 g5 a5 Kf2 Kf8 Bc6 Ke7 Ba4 Bxa4 Rxd8 Bc6 h5 Ke6 h6 Be4 Rh8 Re7 Re1 Kf5 h7 Kg6 Rc8 Kxh7 Rxc5 a4 b4 Kg6 b5 f6 gxf6 Kxf6 b6 a3 Rc7 Rxc7 bxc7 Bb7 Re8 Kf5 c5 Ba6 Ra8 Bb7 Rf8+ Ke5 c6 Ba6 Ra8 Kd6 Rxa6 Kxc7 Kf3 Kb8 Kxf4 Kc8 Ke5 Kc7 Ke6 Kd8 Kd6 Ke8 Ra7 Kf8 c7 Kf7 c8=Q+ Kg6 Qg4+ Kf6 Ra8 Kf7 Qf5+ Kg7 Ra7+ Kg8 Qc8#"

  "Read pgn" should {
    "one complete game" in {
      PgnReader(checkmate) must beSuccess.like {
        case moves ⇒ moves must have size (checkmate.split(' ').size)
      }
    }
  }
}
