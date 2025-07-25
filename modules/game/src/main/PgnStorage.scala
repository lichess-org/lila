package lila.game

import chess.*
import chess.format.Uci
import chess.format.pgn.SanStr

import lila.db.ByteArray

private object PgnStorage:

  case object OldBin:

    def encode(sans: Vector[SanStr]) =
      ByteArray:
        monitor(_.game.pgn.encode("old")):
          format.pgn.Binary.writeMoves(sans).get

    def decode(bytes: ByteArray, plies: Ply): Vector[SanStr] =
      monitor(_.game.pgn.decode("old")):
        format.pgn.Binary.readMoves(bytes.value.toList, plies.value).get.toVector

  case object Huffman:

    import org.lichess.compression.game.{ Encoder, Board as JavaBoard }

    def encode(sans: Vector[SanStr]) =
      ByteArray:
        monitor(_.game.pgn.encode("huffman")):
          Encoder.encode(SanStr.raw(sans.toArray))

    def decode(bytes: ByteArray, plies: Ply, id: GameId): Decoded =
      monitor(_.game.pgn.decode("huffman")):
        val decoded =
          try Encoder.decode(bytes.value, plies.value)
          catch
            case e: java.nio.BufferUnderflowException =>
              logger.error(s"Can't decode game $id PGN", e)
              throw e
        Decoded(
          sans = SanStr.from(decoded.pgnMoves.toVector),
          board = chessBoard(decoded.board),
          positionHashes = PositionHash(decoded.positionHashes),
          unmovedRooks = UnmovedRooks(decoded.board.castlingRights),
          lastMove = Option(decoded.lastUci).flatMap(Uci.apply),
          castles = Castles(decoded.board.castlingRights),
          halfMoveClock = HalfMoveClock(decoded.halfMoveClock)
        )

    private def chessBoard(b: JavaBoard): Board =
      Board(
        occupied = Bitboard(b.occupied),
        ByColor(
          white = Bitboard(b.white),
          black = Bitboard(b.black)
        ),
        ByRole(
          pawn = Bitboard(b.pawns),
          knight = Bitboard(b.knights),
          bishop = Bitboard(b.bishops),
          rook = Bitboard(b.rooks),
          queen = Bitboard(b.queens),
          king = Bitboard(b.kings)
        )
      )

  case class Decoded(
      sans: Vector[SanStr],
      board: Board,
      positionHashes: PositionHash, // irrelevant after game ends
      unmovedRooks: UnmovedRooks, // irrelevant after game ends
      lastMove: Option[Uci],
      castles: Castles, // irrelevant after game ends
      halfMoveClock: HalfMoveClock // irrelevant after game ends
  )

  private def monitor[A](mon: lila.mon.TimerPath)(f: => A): A =
    lila.common.Chronometer.syncMon(mon)(f)
