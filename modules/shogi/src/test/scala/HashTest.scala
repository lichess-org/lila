package shogi

import Pos._
import format.Uci
import variant.Standard

class HashTest extends ShogiTest {

  "Hasher" should {

    val hash = new Hash(3)

    "be consistent" in {
      val fen           = "lnsgkgsnl/1r5b1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL b - 3"
      val situation     = ((format.Forsyth << fen) get) withVariant Standard
      val move          = situation.move(Pos.B2, Pos.H8, false).toOption.get
      val hashAfterMove = hash(move.situationAfter)

      val fenAfter       = "lnsgkgsnl/1r5B1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/7R1/LNSGKGSNL w B 4"
      val situationAfter = (format.Forsyth << fenAfter) get
      val hashAfter      = hash(situationAfter)

      hashAfterMove mustEqual hashAfter
    }

    "hash hands" in {
      val gotePawn =
        ((format.Forsyth << "lnsgkgsnl/1r5b1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL b p 3") get) withVariant Standard
      val gotePawnHash = hash(gotePawn)

      val sentePawn =
        (format.Forsyth << "lnsgkgsnl/1r5b1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL b P 3") get
      val sentePawnHash = hash(sentePawn)

      sentePawnHash mustNotEqual gotePawnHash
    }

    "many rooks in hands" in {
      val goteRooks =
        ((format.Forsyth << "lnsgkgsnl/1r5b1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL b 18r 3") get) withVariant Standard
      val goteRooksHash = hash(goteRooks)

      val senteRooks =
        (format.Forsyth << "lnsgkgsnl/1r5b1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL b 18R 3") get
      val senteRoksHash = hash(senteRooks)

      senteRoksHash mustNotEqual goteRooksHash
    }

    "18+ pieces wraps back (%19)" in {
      val goteRooks =
        ((format.Forsyth << "lnsgkgsnl/1r5b1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL b - 3") get) withVariant Standard
      val goteRooksHash = hash(goteRooks)

      val senteRooks =
        (format.Forsyth << "lnsgkgsnl/1r5b1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL b 19R 3") get
      val senteRoksHash = hash(senteRooks)

      senteRoksHash mustEqual goteRooksHash
    }

  }

}
