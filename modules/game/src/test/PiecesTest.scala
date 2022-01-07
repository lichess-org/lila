package lila.game

import shogi._
import shogi.Pos._
import shogi.format.usi.Usi
import org.specs2.mutable._

import lila.db.ByteArray
import shogi.variant.Standard

// todo
class BinaryPieceTest extends Specification {
  val usis   = Usi.readList(Fixtures.fromProd3).get.toVector
  def runOne = BinaryFormat.pieces.read(Standard)(usis, None)

  "Piece map reader" should {

    "Starting position" in {
      true
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

  }
}
