package chess

import Pos._
import format.Uci
import variant.{ Antichess, Atomic, Crazyhouse, Standard, ThreeCheck }

class HashTest extends ChessTest {
  def hexToBytes(str: String) =
    str.grouped(2).map(cc => Integer.parseInt(cc, 16).toByte).toArray

  "Polyglot hasher" should {

    val hash = new Hash(8)

    // Reference values available at:
    // http://hardy.uhasselt.be/Toga/book_format.html

    "match on the starting position" in {
      val fen  = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
      val game = fenToGame(fen, Standard).toOption err "huh"
      hash(game.situation) mustEqual hexToBytes("463b96181691fc9c")
    }

    "match after 1. e4" in {
      val fen  = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
      val game = fenToGame(fen, Standard).toOption err "huh"
      hash(game.situation) mustEqual hexToBytes("823c9b50fd114196")
    }

    "match after 1. e4 d5" in {
      val fen  = "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
      val game = fenToGame(fen, Standard).toOption err "huh"
      hash(game.situation) mustEqual hexToBytes("0756b94461c50fb0")
    }

    "match after 1. e4 d5 2. e5" in {
      val fen  = "rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR b KQkq - 0 2"
      val game = fenToGame(fen, Standard).toOption err "huh"
      hash(game.situation) mustEqual hexToBytes("662fafb965db29d4")
    }

    "match after 1. e4 d5 2. e5 f5" in {
      // note that en-passant matters
      val fen  = "rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3"
      val game = fenToGame(fen, Standard).toOption err "huh"
      hash(game.situation) mustEqual hexToBytes("22a48b5a8e47ff78")
    }

    "match after 1. e4 d5 2. e5 f5 3. Ke2" in {
      // 3. Ke2 forfeits castling rights
      val fen  = "rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPPKPPP/RNBQ1BNR b kq - 1 3"
      val game = fenToGame(fen, Standard).toOption err "huh"
      hash(game.situation) mustEqual hexToBytes("652a607ca3f242c1")
    }

    "match after 1. e4 d5 2. e5 f5 3. Ke2 Kf7" in {
      val fen  = "rnbq1bnr/ppp1pkpp/8/3pPp2/8/8/PPPPKPPP/RNBQ1BNR w - - 2 4"
      val game = fenToGame(fen, Standard).toOption err "huh"
      hash(game.situation) mustEqual hexToBytes("00fdd303c946bdd9")
    }

    "match after 1. a4 b5 2. h4 b4 3. c4" in {
      // again, note en-passant matters
      val fen  = "rnbqkbnr/p1pppppp/8/8/PpP4P/8/1P1PPPP1/RNBQKBNR b KQkq c3 0 3"
      val game = fenToGame(fen, Standard).toOption err "huh"
      hash(game.situation) mustEqual hexToBytes("3c8123ea7b067637")
    }

    "match after 1. a4 b5 2. h4 b4 3. c4 bxc3 4. Ra3" in {
      // 4. Ra3 partially forfeits castling rights
      val fen  = "rnbqkbnr/p1pppppp/8/8/P6P/R1p5/1P1PPPP1/1NBQKBNR b Kkq - 1 4"
      val game = fenToGame(fen, Standard).toOption err "huh"
      hash(game.situation) mustEqual hexToBytes("5c3f9b829b279560")
    }
  }

  "Hasher" should {

    val hash = new Hash(3)

    "account for checks in three-check" in {
      // 2 ... Bb4+
      val gameA = Game(Board init ThreeCheck)
        .playMoves(
          E2 -> E4,
          E7 -> E6,
          D2 -> D4,
          F8 -> B4
        )
        .toOption
        .get

      // repeat
      val gameB = gameA
        .playMoves(
          C1 -> D2,
          B4 -> F8,
          D2 -> C1,
          F8 -> B4
        )
        .toOption
        .get

      hash(gameA.situation) mustNotEqual hash(gameB.situation)
    }

    "account for pockets in crazyhouse" in {
      val gameA = Game(Crazyhouse)
        .playMoves(
          E2 -> E4,
          D7 -> D5,
          E4 -> D5
        )
        .toOption
        .get

      val intermediate = Game(Crazyhouse)
        .playMoves(
          E2 -> E4,
          D7 -> D5,
          E4 -> D5,
          D8 -> D7
        )
        .toOption
        .get

      // we reach the same position, but now the pawn is in blacks pocket
      val gameB = intermediate(Uci.Drop(Pawn, D6)).toOption.get._1
        .playMoves(
          D7 -> D6,
          D1 -> E2,
          D6 -> D8,
          E2 -> D1
        )
        .toOption
        .get

      hash(gameA.situation) mustNotEqual hash(gameB.situation)
    }

    "be consistent in crazyhouse" in {
      // from https://lichess.org/j4r7XHTB/black
      val fen           = "r2qkb1r/ppp1pppp/2n2n2/3p2B1/3P2b1/4PN2/PPP1BPPP/RN1QK2R/ b KQkq - 9 5"
      val situation     = ((format.Forsyth << fen) get) withVariant Crazyhouse
      val move          = situation.move(Pos.G4, Pos.F3, None).toOption.get
      val hashAfterMove = hash(move.situationAfter)

      // 5 ... Bxf3
      val fenAfter       = "r2qkb1r/ppp1pppp/2n2n2/3p2B1/3P4/4Pb2/PPP1BPPP/RN1QK2R/n w KQkq - 10 6"
      val situationAfter = (format.Forsyth << fenAfter) get
      val hashAfter      = hash(situationAfter)

      hashAfterMove mustEqual hashAfter
    }

    "be consistent when king is captured in antichess" in {
      val fen           = "rnbqkb1r/ppp1pppp/3p1n2/1B6/8/4P3/PPPP1PPP/RNBQK1NR w KQkq - 2 3"
      val situation     = ((format.Forsyth << fen) get) withVariant Antichess
      val move          = situation.move(Pos.B5, Pos.E8, None).toOption.get
      val hashAfterMove = hash(move.situationAfter)

      // 3. BxK
      val fenAfter       = "rnbqBb1r/ppp1pppp/3p1n2/8/8/4P3/PPPP1PPP/RNBQK1NR b KQkq - 0 3"
      val situationAfter = ((format.Forsyth << fenAfter) get) withVariant Antichess
      val hashAfter      = hash(situationAfter)

      hashAfterMove mustEqual hashAfter
    }

    "be consistent when rook is exploded in atomic" in {
      val fen           = "rnbqkb1r/ppppp1pp/5p1n/6N1/8/8/PPPPPPPP/RNBQKB1R w KQkq - 2 3"
      val situation     = format.Forsyth.<<@(Atomic, fen).get
      val move          = situation.move(Pos.G5, Pos.H7, None).toOption.get
      val hashAfterMove = hash(move.situationAfter)

      // 3. Nxh7
      val fenAfter       = "rnbqkb2/ppppp1p1/5p2/8/8/8/PPPPPPPP/RNBQKB1R b KQkq - 0 3"
      val situationAfter = format.Forsyth.<<@(Atomic, fenAfter).get
      val hashAfter      = hash(situationAfter)

      hashAfterMove mustEqual hashAfter
    }
    "prod 5 Three-Check games accumulate hash" in {
      val gameMoves = format.pgn.Fixtures.prod5threecheck.map {
        _.split(' ').toList
      }
      def runOne(moves: List[String]) =
        Replay.gameMoveWhileValid(moves, format.Forsyth.initial, chess.variant.ThreeCheck)
      def hex(buf: Array[Byte]): String = buf.map("%02x" format _).mkString
      val g                             = gameMoves.map(runOne)
      g.exists(_._3.nonEmpty) must beFalse
      val m16 = java.security.MessageDigest getInstance "MD5"
      val h   = new Hash(16)
      g.foreach(_._2.foreach(x => m16.update(h(x._1.situation))))
      hex(m16.digest) must beEqualTo("21281304d25ccf9c1dfd640775800087")
    }
  }

}
