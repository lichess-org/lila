package lila.game

import shogi.format.Uci
import shogi.{ variant => _, ToOptionOpsFromOption => _, _ }
import shogi.variant.{ Standard }
import lila.db.ByteArray

sealed trait PgnStorage

private object PgnStorage {

  case object OldBin extends PgnStorage {

    def encode(pgnMoves: PgnMoves) = {
      ByteArray {
        monitor(_.game.pgn.encode("old")) {
          format.pgn.Binary.writeMoves(pgnMoves).get
        }
      }
    }

    def decode(bytes: ByteArray, plies: Int): PgnMoves = {
      monitor(_.game.pgn.decode("old")) {
        format.pgn.Binary.readMoves(bytes.value.toList, plies).get.toVector
      }
    }
  }

  // case object Huffman extends PgnStorage {
  //
  //   import org.lishogi.compression.game.{
  //     Encoder,
  //     Square => JavaSquare,
  //     Piece => JavaPiece,
  //     Role => JavaRole
  //   }
  //   import scala.jdk.CollectionConverters._
  // // PgnMoves - Vector[String]
  //   def encode(pgnMoves: PgnMoves) = {
  //     ByteArray {
  //       monitor(_.game.pgn.encode("huffman")) {
  //         Encoder.encode(pgnMoves.toArray)
  //       }
  //     }
  //   }
  //   def decode(bytes: ByteArray, plies: Int): Decoded =
  //     monitor(_.game.pgn.decode("huffman")) {
  //       val decoded      = Encoder.decode(bytes.value, plies)
  //       Decoded(
  //         pgnMoves = decoded.pgnMoves.toVector,
  //         pieces = Standard.pieces,
  //         checkCount = List(0, 0),
  //         positionHashes = decoded.positionHashes,
  //         lastMove = Option(decoded.lastUci) flatMap Uci.apply,
  //         halfMoveClock = decoded.halfMoveClock
  //       )
  //     }
  //
  //   private def shogiPos(sq: Integer): Option[Pos] =
  //     Pos.posAt(JavaSquare.file(sq) + 1, JavaSquare.rank(sq) + 1)
  //   private def shogiRole(role: JavaRole): Role =
  //     role match {
  //       case JavaRole.PAWN   => Pawn
  //       case JavaRole.KNIGHT => Knight
  //       case JavaRole.BISHOP => Bishop
  //       case JavaRole.ROOK   => Rook
  //       case JavaRole.KING   => King
  //       case _ => Lance
  //     }
  //   private def shogiPiece(piece: JavaPiece): Piece = Piece(Color(piece.sente), shogiRole(piece.role))
  // }

  case class Decoded(
      pgnMoves: PgnMoves,
      pieces: PieceMap,
      positionHashes: PositionHash, // irrelevant after game ends
      checkCount: List[Int],
      lastMove: Option[Uci],
      hands: Option[Hands]
  )

  private def monitor[A](mon: lila.mon.TimerPath)(f: => A): A =
    lila.common.Chronometer.syncMon(mon)(f)
}
