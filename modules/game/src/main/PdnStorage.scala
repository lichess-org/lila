package lidraughts.game

import draughts.format.Uci
import draughts.variant.Variant
import draughts.{ variant => _, ToOptionOpsFromOption => _, _ }
import lidraughts.db.ByteArray

sealed trait PdnStorage

private object PdnStorage {

  case object OldBin extends PdnStorage {

    def encode(pdnmoves: PdnMoves) = ByteArray {
      monitor(lidraughts.mon.game.pdn.oldBin.encode) {
        format.pdn.Binary.writeMoves(pdnmoves).get
      }
    }

    def decode(bytes: ByteArray, plies: Int): PdnMoves = monitor(lidraughts.mon.game.pdn.oldBin.decode) {
      val v = format.pdn.Binary.readMoves(bytes.value.toList, plies).get.toVector
      v
    }
  }

  case object Huffman extends PdnStorage {

    import org.lichess.compression.game.{ Encoder, Square => JavaSquare, Piece => JavaPiece, Role => JavaRole }
    import scala.collection.JavaConversions._

    def encode(pdnmoves: PdnMoves) = ByteArray {
      monitor(lidraughts.mon.game.pdn.huffman.encode) {
        Encoder.encode(pdnmoves.toArray)
      }
    }
    def decode(bytes: ByteArray, plies: Int): Decoded = monitor(lidraughts.mon.game.pdn.huffman.decode) {
      val decoded = Encoder.decode(bytes.value, plies)
      val unmovedRooks = asScalaSet(decoded.unmovedRooks.flatMap(chessPos)).toSet
      Decoded(
        pdnMoves = decoded.pgnMoves.toVector,
        pieces = mapAsScalaMap(decoded.pieces).flatMap {
          case (k, v) => chessPos(k).map(_ -> chessPiece(v))
        }.toMap,
        positionHashes = decoded.positionHashes,
        lastMove = Option(decoded.lastUci) flatMap Uci.apply,
        format = Huffman
      )
    }

    private def chessPos(sq: Integer): Option[Pos] = draughts.variant.Standard.boardSize.pos.posAt(JavaSquare.file(sq) + 1, JavaSquare.rank(sq) + 1)
    private def chessRole(role: JavaRole): Role = role match {
      case JavaRole.PAWN => Man
      case JavaRole.KING => King
      case JavaRole.BISHOP => King
      case JavaRole.KNIGHT => King
      case JavaRole.QUEEN => King
      case JavaRole.ROOK => King
    }
    private def chessPiece(piece: JavaPiece): Piece = Piece(Color(piece.white), chessRole(piece.role))
  }

  case class Decoded(
      pdnMoves: PdnMoves,
      pieces: PieceMap,
      positionHashes: PositionHash, // irrelevant after game ends
      lastMove: Option[Uci],
      format: PdnStorage
  )

  private val betaTesters = Set("")
  private def shouldUseHuffman(variant: Variant, playerUserIds: List[lidraughts.user.User.ID]) = variant.standard && {
    try {
      lidraughts.game.Env.current.pdnEncodingSetting.get() match {
        case "all" => true
        case "beta" if playerUserIds.exists(betaTesters.contains) => true
        case _ => false
      }
    } catch {
      case e: Throwable =>
        println(e)
        false // breaks in tests. The shouldUseHuffman function is temporary anyway
    }
  }
  private[game] def apply(variant: Variant, playerUserIds: List[lidraughts.user.User.ID]): PdnStorage =
    if (shouldUseHuffman(variant, playerUserIds)) Huffman else OldBin

  private def monitor[A](mon: lidraughts.mon.game.pdn.Protocol)(f: => A): A = {
    mon.count()
    lidraughts.mon.measureRec(mon.time)(f)
  }
}
