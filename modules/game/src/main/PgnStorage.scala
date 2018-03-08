package lila.game

import chess.format.Uci
import chess.variant.Variant
import chess.{ variant => _, ToOptionOpsFromOption => _, _ }
import lila.db.ByteArray

sealed trait PgnStorage

private object PgnStorage {

  case object OldBin extends PgnStorage {

    def encode(pgnMoves: PgnMoves) = ByteArray {
      monitor(lila.mon.game.pgn.oldBin.encode) {
        format.pgn.Binary.writeMoves(pgnMoves).get
      }
    }

    def decode(bytes: ByteArray, plies: Int): PgnMoves = monitor(lila.mon.game.pgn.oldBin.decode) {
      format.pgn.Binary.readMoves(bytes.value.toList, plies).get.toVector
    }
  }

  case object Huffman extends PgnStorage {

    import org.lichess.compression.game.{ Encoder, Square => JavaSquare, Piece => JavaPiece, Role => JavaRole }
    import scala.collection.JavaConversions._

    def encode(pgnMoves: PgnMoves) = ByteArray {
      monitor(lila.mon.game.pgn.huffman.encode) {
        Encoder.encode(pgnMoves.toArray)
      }
    }
    def decode(bytes: ByteArray, plies: Int): Decoded = monitor(lila.mon.game.pgn.huffman.decode) {
      val decoded = Encoder.decode(bytes.value, plies)
      val unmovedRooks = asScalaSet(decoded.unmovedRooks.flatMap(chessPos)).toSet
      Decoded(
        pgnMoves = decoded.pgnMoves.toVector,
        pieces = mapAsScalaMap(decoded.pieces).flatMap {
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
        )
      )
    }

    private def chessPos(sq: Integer): Option[Pos] = Pos.posAt(JavaSquare.file(sq) + 1, JavaSquare.rank(sq) + 1)
    private def chessRole(role: JavaRole): Role = role match {
      case JavaRole.PAWN => Pawn
      case JavaRole.KNIGHT => Knight
      case JavaRole.BISHOP => Bishop
      case JavaRole.ROOK => Rook
      case JavaRole.QUEEN => Queen
      case JavaRole.KING => King
    }
    private def chessPiece(piece: JavaPiece): Piece = Piece(Color(piece.white), chessRole(piece.role))
  }

  case class Decoded(
      pgnMoves: PgnMoves,
      pieces: PieceMap,
      positionHashes: PositionHash, // irrelevant after game ends
      unmovedRooks: UnmovedRooks, // irrelevant after game ends
      lastMove: Option[Uci],
      castles: Castles // irrelevant after game ends
  )

  private def shouldUseHuffman(variant: Variant, playerUserIds: List[lila.user.User.ID]) = variant.standard && {
    try {
      lila.game.Env.current.pgnEncodingSetting.get() match {
        case "all" => true
        case "none" => false
        case regex if playerUserIds.exists(_ matches regex) => true
        case _ => false
      }
    } catch {
      case e: Throwable =>
        println(e)
        false // breaks in tests. The shouldUseHuffman function is temporary anyway
    }
  }
  private[game] def apply(variant: Variant, playerUserIds: List[lila.user.User.ID]): PgnStorage =
    if (shouldUseHuffman(variant, playerUserIds)) Huffman else OldBin

  private def monitor[A](mon: lila.mon.game.pgn.Protocol)(f: => A): A = {
    mon.count()
    lila.mon.measureRec(mon.time)(f)
  }
}
