package lila.game

import org.specs2.mutable._

import shogi.format.usi.Usi
import shogi.variant.{ Standard, Variant }

// todo
class PiecesTest extends Specification {
  val usis                                                     = Usi.readList(Fixtures.fromProd3).get.toVector
  def fromUsis(usis: Vector[Usi], variant: Variant = Standard) = BinaryFormat.pieces.read(usis, None, variant)

  "Piece map reader" should {

    "Starting position" in {
      fromUsis(Vector[Usi]()) must_== Standard.pieces
    }

    "single move" in {
      true
    }

    "two moves" in {
      true
    }

    "capture" in {
      true
    }

    "capture and a drop" in {
      true
    }

    "from position" in {
      true
    }

    "minishogi" in {
      true
    }

    "500 production" in {
      Fixtures.fromProd500 forall { uStr =>
        val u = Usi.readList(uStr).get
        fromUsis(u.toVector) must_== shogi.Replay.situations(u, None, Standard).toOption.get.last.board.pieces
      }
    }

  }
}
