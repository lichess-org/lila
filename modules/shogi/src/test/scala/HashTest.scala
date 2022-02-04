package shogi

import variant.Standard
import format.forsyth.Sfen
import format.usi.Usi

class HashTest extends ShogiTest {

  "Hasher" should {

    val hash = new Hash(3)

    "be consistent" in {
      val sfen          = Sfen("lnsgkgsnl/1r5b1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL b - 3")
      val situation     = sfen.toSituation(Standard).get
      val sitAfter      = situation(Usi.Move(Pos.SQ8H, Pos.SQ2B, false)).toOption.get
      val hashAfterMove = hash(sitAfter)

      val sfenAfter      = Sfen("lnsgkgsnl/1r5B1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/7R1/LNSGKGSNL w B 4")
      val situationAfter = sfenAfter.toSituation(Standard).get
      val hashAfter      = hash(situationAfter)

      hashAfterMove mustEqual hashAfter
    }

    "hash hands" in {
      val gotePawn =
        Sfen("lnsgkgsnl/1r5b1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL b p 3").toSituation(Standard).get
      val gotePawnHash = hash(gotePawn)

      val sentePawn =
        Sfen("lnsgkgsnl/1r5b1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL b P 3").toSituation(Standard).get
      val sentePawnHash = hash(sentePawn)

      sentePawnHash mustNotEqual gotePawnHash
    }

    "many rooks in hands" in {
      val goteRooks =
        Sfen("lnsgkgsnl/1r5b1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL b 18r 3").toSituation(Standard).get
      val goteRooksHash = hash(goteRooks)

      val senteRooks =
        Sfen("lnsgkgsnl/1r5b1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL b 18R 3").toSituation(Standard).get
      val senteRoksHash = hash(senteRooks)

      senteRoksHash mustNotEqual goteRooksHash
    }

    "18+ pieces wraps back (%19)" in {
      val goteRooks =
        Sfen("lnsgkgsnl/1r5b1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL b - 3").toSituation(Standard).get
      val goteRooksHash = hash(goteRooks)

      val senteRooks =
        Sfen("lnsgkgsnl/1r5b1/pppppp1pp/6p2/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL b 19R 3").toSituation(Standard).get
      val senteRoksHash = hash(senteRooks)

      senteRoksHash mustEqual goteRooksHash
    }

  }

}
