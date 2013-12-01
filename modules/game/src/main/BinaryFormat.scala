package lila.game

import scala.util.{ Try, Success, Failure }

import chess._

import lila.db.ByteArray

object BinaryFormat {

  object piece {

    def write(all: AllPieces): ByteArray = {
      val (alives, deads) = all
      def posInt(pos: Pos): Int = (alives get pos).fold(0)(pieceInt)
      def pieceInt(piece: Piece): Int =
        piece.color.fold(0, 8) + roleToInt(piece.role)
      val aliveBytes: Iterator[Int] = Pos.all grouped 2 map {
        case List(p1, p2) ⇒ (posInt(p1) << 4) + posInt(p2)
      }
      val deadBytes: Iterator[Int] = deads grouped 2 map {
        case List(d1, d2) ⇒ (pieceInt(d1) << 4) + pieceInt(d2)
        case List(d1)     ⇒ pieceInt(d1) << 4
      }
      val bytes = aliveBytes.toArray ++ deadBytes
      ByteArray(bytes.map(_.toByte))
    }

    def read(ba: ByteArray): AllPieces = {
      def splitInts(int: Int) = Array(int >> 4, int & 0x0F) 
      def intPiece(int: Int): Option[Piece] = 
        intToRole(int & 7) map { role => Piece(Color((int & 8) == 0), role) }
      val (aliveInts, deadInts) = ba.value map (_.toInt) flatMap splitInts splitAt 64
      val alivePieces = (Pos.all zip aliveInts map {
        case (pos, int) => intPiece(int) map (pos -> _)
      }).flatten.toMap
      alivePieces -> (deadInts map intPiece).toList.flatten
    }

    // cache standard start position
    val standard = write(Board.init(Variant.Standard).pieces -> Nil)

    private def intToRole(int: Int): Option[Role] = int match {
      case 6 ⇒ Some(Pawn)
      case 1 ⇒ Some(King)
      case 2 ⇒ Some(Queen)
      case 3 ⇒ Some(Rook)
      case 4 ⇒ Some(Knight)
      case 5 ⇒ Some(Bishop)
      case _ ⇒ None
    }
    private def roleToInt(role: Role): Int = role match {
      case Pawn   ⇒ 6
      case King   ⇒ 1
      case Queen  ⇒ 2
      case Rook   ⇒ 3
      case Knight ⇒ 4
      case Bishop ⇒ 5
    }
  }
}
