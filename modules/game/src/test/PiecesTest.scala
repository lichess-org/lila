package lila.game

import org.specs2.mutable._

import shogi.format.forsyth.Sfen
import shogi.format.usi.Usi
import shogi.variant.{ Chushogi, Standard, Variant }

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

    "chushogi" in {
      val ms = List(
        "7i7h",
        "7d7e",
        "7j7i6h",
        "6d6e",
        "5i5h",
        "6c5e",
        "5h5g",
        "5e5g",
        "6h8g",
        "5g4h5g",
        "3i3h",
        "5g3h",
        "8g7g8g",
        "3h2h3h",
        "8g7e",
        "4e4f",
        "7e7d8d",
        "3h3j",
        "7k7i",
        "3j2j1j",
        "7i7g",
        "1j1k1l",
        "7g7e",
        "12d12e",
        "7e7c+",
        "5d5e",
        "7c8b9a",
        "6e6f",
        "9a10a11b"
      )
      val u = Usi.readList(ms).get
      fromUsis(u.toVector, Chushogi) must_== Sfen(
        "lf2gekgscfl/a+Ob2xot1b1a/mvrhd2dhrvm/1pppN3pppp/p2i3p4/6p1i3/12/3I1P6/PPPPP1P1P1PP/MVRHD1QDH3/A1B1T1XT1B2/LFCSGKEGSCFn w - 30"
      ).toSituation(Chushogi).get.board.pieces
    }

    "500 production" in {
      Fixtures.fromProd500 forall { uStr =>
        val u = Usi.readList(uStr).get
        fromUsis(u.toVector) must_== shogi.Replay.situations(u, None, Standard).toOption.get.last.board.pieces
      }
    }

  }
}
