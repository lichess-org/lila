package lila.game

import chess.*
import chess.format.Uci
import chess.format.pgn.SanStr

import lila.db.ByteArray

private object PgnStorage:

  case object OldBin:

    def encode(sans: Vector[SanStr]) =
      ByteArray {
        monitor(_.game.pgn.encode("old")) {
          format.pgn.Binary.writeMoves(sans).get
        }
      }

    def decode(bytes: ByteArray, plies: Ply): Vector[SanStr] =
      monitor(_.game.pgn.decode("old")) {
        format.pgn.Binary.readMoves(bytes.value.toList, plies.value).get.toVector
      }

  case object Huffman:

    import org.lichess.compression.game.{ Encoder, Piece as JavaPiece, Role as JavaRole }
    import scala.jdk.CollectionConverters.*

    def encode(sans: Vector[SanStr]) =
      ByteArray {
        monitor(_.game.pgn.encode("huffman")) {
          Encoder.encode(SanStr raw sans.toArray)
        }
      }
    def decode(bytes: ByteArray, plies: Ply, id: GameId): Decoded =
      monitor(_.game.pgn.decode("huffman")) {
        val decoded =
          try {
            Encoder.decode(bytes.value, plies.value)
          } catch {
            case e: java.nio.BufferUnderflowException =>
              logger.error(s"Can't decode game $id PGN", e)
              throw e
          }
        val unmovedRooks = decoded.unmovedRooks.asScala.view.map(Square(_)).toSet
        Decoded(
          sans = SanStr from decoded.pgnMoves.toVector,
          pieces = decoded.pieces.asScala.view.map { (k, v) =>
            Square(k) -> chessPiece(v)
          }.toMap,
          positionHashes = PositionHash(decoded.positionHashes),
          unmovedRooks = UnmovedRooks(unmovedRooks),
          lastMove = Option(decoded.lastUci) flatMap Uci.apply,
          castles = Castles(
            whiteKingSide = unmovedRooks(Square.H1),
            whiteQueenSide = unmovedRooks(Square.A1),
            blackKingSide = unmovedRooks(Square.H8),
            blackQueenSide = unmovedRooks(Square.A8)
          ),
          halfMoveClock = HalfMoveClock(decoded.halfMoveClock)
        )
      }

    private def chessRole(role: JavaRole): Role =
      role match
        case JavaRole.PAWN   => Pawn
        case JavaRole.KNIGHT => Knight
        case JavaRole.BISHOP => Bishop
        case JavaRole.ROOK   => Rook
        case JavaRole.QUEEN  => Queen
        case JavaRole.KING   => King
    private def chessPiece(piece: JavaPiece): Piece =
      Piece(Color.fromWhite(piece.white), chessRole(piece.role))

  case class Decoded(
      sans: Vector[SanStr],
      pieces: PieceMap,
      positionHashes: PositionHash, // irrelevant after game ends
      unmovedRooks: UnmovedRooks,   // irrelevant after game ends
      lastMove: Option[Uci],
      castles: Castles,            // irrelevant after game ends
      halfMoveClock: HalfMoveClock // irrelevant after game ends
  )

  private def monitor[A](mon: lila.mon.TimerPath)(f: => A): A =
    lila.common.Chronometer.syncMon(mon)(f)
