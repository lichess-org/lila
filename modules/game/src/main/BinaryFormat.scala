package lila.game

import scala.util.{ Try, Success, Failure }

import chess._

import lila.db.ByteArray

object BinaryFormat {

  object castleLastMoveTime {

    def write(clmt: CastleLastMoveTime): ByteArray = {

      val castleInt = clmt.castles.toList.zipWithIndex.foldLeft(0) {
        case (acc, (false, _)) ⇒ acc
        case (acc, (true, p))  ⇒ acc + (1 << (3 - p))
      }

      def posInt(pos: Pos): Int = ((pos.x - 1) << 3) + pos.y - 1
      val lastMoveInt = clmt.lastMove.fold(0) {
        case (f, t) ⇒ (posInt(f) << 6) + posInt(t)
      }
      val time = clmt.lastMoveTime getOrElse 0

      val bytes = ((castleInt << 4) + (lastMoveInt >> 8)) ::
        (lastMoveInt & 255) ::
        (time >> 16) ::
        ((time >> 8) & 255) ::
        (time & 255) ::
        Nil

      ByteArray(bytes.map(_.toByte).toArray)
    }

    def read(ba: ByteArray): CastleLastMoveTime = {
      def posAt(x: Int, y: Int) = Pos.posAt(x + 1, y + 1)
      ba.value map toInt match {
        case Array(b1, b2, b3, b4, b5) ⇒ CastleLastMoveTime(
          castles = Castles(b1 > 127, (b1 & 64) != 0, (b1 & 32) != 0, (b1 & 16) != 0),
          lastMove = for {
            from ← posAt((b1 & 15) >> 1, ((b1 & 1) << 2) + (b2 >> 6))
            to ← posAt((b2 & 63) >> 3, b2 & 7)
            if from != to
          } yield from -> to,
          lastMoveTime = Some((b3 << 16) + (b4 << 8) + b5) filter (0 !=)
        )
      }
    }
  }

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
        intToRole(int & 7) map { role ⇒ Piece(Color((int & 8) == 0), role) }
      val (aliveInts, deadInts) = ba.value map toInt flatMap splitInts splitAt 64
      val alivePieces = (Pos.all zip aliveInts map {
        case (pos, int) ⇒ intPiece(int) map (pos -> _)
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

  @inline private def toInt(b: Byte): Int = b & 0xff
}
