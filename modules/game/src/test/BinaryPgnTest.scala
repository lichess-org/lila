package lila.game

import org.lichess.compression.game.{ Encoder => GameEncoder }

import org.specs2.mutable._

class BinaryPgnTest extends Specification {

  "game compression" should {
    "compress and decompress" in {
      val pgnMoves = "Nf3 Nf6 d4 Ng8 b3 e6 Bb2 g6 c4 h5 Na3 c6 Bc1 Qf6 c5 Qe5 g3 Qf4 h3 Bd6 Ng5 Qxf2+ Kd2 Kf8 Qe1 b6 b4 Qf5 Ke3 Ke8 Rh2 Rh6 h4 Rh8 Kd2 Qf3 Rh3 Qxf1 d5 Qxe2+ Kxe2".split(" ")
      val encoded = GameEncoder.encode(pgnMoves)
      val decoded = GameEncoder.decode(encoded)
      pgnMoves must_== decoded.toArray
    }
  }

}
