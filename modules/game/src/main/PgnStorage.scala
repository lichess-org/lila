package lila.game

import chess._
import chess.format.Uci

import lila.db.ByteArray

sealed trait PgnStorage

private object PgnStorage {

  case object OldBin extends PgnStorage {

    def encode(pgnMoves: PgnMoves) =
      ByteArray {
        monitor(_.game.pgn.encode("old")) {
          format.pgn.Binary.writeMoves(pgnMoves).get
        }
      }

    def decode(bytes: ByteArray, plies: Int): PgnMoves =
      monitor(_.game.pgn.decode("old")) {
        format.pgn.Binary.readMoves(bytes.value.toList, plies).get.toVector
      }
  }

  case object Huffman extends PgnStorage {

    import org.lichess.compression.game.{
      Encoder,
      Square => JavaSquare,
      Piece => JavaPiece,
      Role => JavaRole
    }
    import scala.jdk.CollectionConverters._

    def encode(pgnMoves: PgnMoves) =
      ByteArray {
        monitor(_.game.pgn.encode("huffman")) {
          Encoder.encode(pgnMoves.toArray)
        }
      }
    def decode(bytes: ByteArray, plies: Int): Decoded =
      monitor(_.game.pgn.decode("huffman")) {
        val decoded      = Encoder.decode(bytes.value, plies)
        val unmovedRooks = decoded.unmovedRooks.asScala.view.flatMap(chessPos).to(Set)
        Decoded(
          pgnMoves = decoded.pgnMoves.toVector,
          pieces = decoded.pieces.asScala.view.flatMap {
            case (k, v) => chessPos(k).map(_ -> chessPiece(v))
          }.toMap,
          positionHashes = decoded.positionHashes,
          unmovedRooks = UnmovedRooks(unmovedRooks),
          lastMove = Option(decoded.lastUci) flatMap Uci.apply,
          castles = Castles(
            whiteKingSide = unmovedRooks(Pos.H1),
            whiteQueenSide = unmovedRooks(Pos.A1),
            blackKingSide = unmovedRooks(Pos.H8),
            blackQueenSide = unmovedRooks(Pos.A8)
          ),
          halfMoveClock = decoded.halfMoveClock
        )
      }

    private def chessPos(sq: Integer): Option[Pos] =
      Pos.posAt(JavaSquare.file(sq) + 1, JavaSquare.rank(sq) + 1)
    private def chessRole(role: JavaRole): Role =
      role match {
        case JavaRole.PAWN   => Pawn
        case JavaRole.KNIGHT => Knight
        case JavaRole.BISHOP => Bishop
        case JavaRole.ROOK   => Rook
        case JavaRole.QUEEN  => Queen
        case JavaRole.KING   => King
      }
    private def chessPiece(piece: JavaPiece): Piece = Piece(Color(piece.white), chessRole(piece.role))
  }

  case class Decoded(
      pgnMoves: PgnMoves,
      pieces: PieceMap,
      positionHashes: PositionHash, // irrelevant after game ends
      unmovedRooks: UnmovedRooks,   // irrelevant after game ends
      lastMove: Option[Uci],
      castles: Castles,  // irrelevant after game ends
      halfMoveClock: Int // irrelevant after game ends
  )

  private def monitor[A](mon: lila.mon.TimerPath)(f: => A): A =
    lila.common.Chronometer.syncMon(mon)(f)
}
