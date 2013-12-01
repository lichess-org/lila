package lila.game

import chess._

import lila.db.ByteArray

object BinaryFormat {

  object piece {

    def encode(all: AllPieces): ByteArray = {
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

    def decode(str: ByteArray): AllPieces =
      ???

    def intToRole(int: Int) = int match {
      case 6 ⇒ Pawn
      case 1 ⇒ King
      case 2 ⇒ Queen
      case 3 ⇒ Rook
      case 4 ⇒ Knight
      case 5 ⇒ Bishop
      case x ⇒ sys error s"Invalid role int $x"
    }
    def roleToInt(role: Role) = role match {
      case Pawn   ⇒ 6
      case King   ⇒ 1
      case Queen  ⇒ 2
      case Rook   ⇒ 3
      case Knight ⇒ 4
      case Bishop ⇒ 5
    }
  }
}
