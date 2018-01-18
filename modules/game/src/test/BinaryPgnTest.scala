package lila.game

import org.lichess.compression.game.{ Encoder => GameEncoder }

import org.specs2.mutable._

class BinaryPgnTest extends Specification {

  "game compression" should {

    "compress and decompress" in {
      val pgnMoves = "Nf3 Nf6 d4 Ng8 b3 e6 Bb2 g6 c4 h5 Na3 c6 Bc1 Qf6 c5 Qe5 g3 Qf4 h3 Bd6 Ng5 Qxf2+ Kd2 Kf8 Qe1 b6 b4 Qf5 Ke3 Ke8 Rh2 Rh6 h4 Rh8 Kd2 Qf3 Rh3 Qxf1 d5 Qxe2+ Kxe2".split(" ")
      val encoded = GameEncoder.encode(pgnMoves)
      encoded.size.pp("bytes")
      val decoded = GameEncoder.decode(encoded)
      pgnMoves must_== decoded
    }

    "with checkmate" in {
      val pgnMoves = "d4 g6 c4 Bg7 Nc3 Nf6 e4 O-O Nf3 Ne8 Be2 d6 O-O c6 Ne1 Nd7 Be3 e5 d5 c5 f3 b6 a3 a6 b4 f5 bxc5 Nxc5 Bxc5 bxc5 Rb1 fxe4 fxe4 Rxf1+ Bxf1 Nf6 Bd3 Bg4 Nf3 h5 Qa4 Bd7 Qb3 Ng4 Qb6 Qxb6 Rxb6 Ne3 Rxd6 Bg4 Kf2 Bxf3 gxf3 Nxc4 Bxc4 Kh7 Rd7 Rb8 Ke3 Rb2 Kd3 Rxh2 d6 Rh3 Ke2 Rh2+ Ke3 Rh3 Kf2 g5 Re7 g4 d7 g3+ Kg2 h4 Kxh3 g2 Kxg2 h3+ Kxh3 Kg6 d8=Q Bf6 Qd7 Bxe7 Qxe7 a5 Qe6+ Kg7 Bd5 Kf8 Bc6 Kg7 Bd5 Kf8 Qf7#".split(" ")
      val encoded = GameEncoder.encode(pgnMoves)
      encoded.size.pp("bytes")
      val decoded = GameEncoder.decode(encoded)
      pgnMoves must_== decoded
    }

    "en passant and promotion" in {
      val pgnMoves = "e4 d5 e5 f5 exf6 h6 fxg7 Nf6 gxf8=Q+ Kd7 Qh5 Nc6 Qhf7".split(" ")
      val encoded = GameEncoder.encode(pgnMoves)
      encoded.size.pp("bytes")
      val decoded = GameEncoder.decode(encoded)
      pgnMoves must_== decoded
    }
  }
}
