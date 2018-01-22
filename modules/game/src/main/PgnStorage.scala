package lila.game

import chess.{ variant => _, ToOptionOpsFromOption => _, _ }
import chess.variant.Variant
import lila.db.ByteArray

sealed trait PgnStorage

object PgnStorage {

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

    import org.lichess.compression.game.Encoder
    import scala.collection.JavaConversions._

    def encode(pgnMoves: PgnMoves) = ByteArray {
      monitor(lila.mon.game.pgn.huffman.encode) {
        Encoder.encode(pgnMoves.toArray)
      }
    }
    def decode(bytes: ByteArray, plies: Int): Decoded = monitor(lila.mon.game.pgn.huffman.decode) {
      Encoder.decode(bytes.value, plies) match {
        case (pgnMoves, pieces, unmovedRooks, positionHashes) => Decoded(
          pgnMoves = pgnMoves.toVector,
          pieces = mapAsScalaMap(pieces).toMap,
          positionHashes = positionHashes,
          unmovedRooks = UnmovedRooks(asScalaSet(unmovedRooks).toSet),
          format = Huffman
        )
      }
    }
  }

  case class Decoded(
      pgnMoves: PgnMoves,
      pieces: PieceMap,
      positionHashes: PositionHash, // irrelevant after game ends
      unmovedRooks: UnmovedRooks, // irrelevant after game ends
      format: PgnStorage
  )

  private val betaTesters = Set("thibault", "revoof", "isaacly")
  private def shouldUseHuffman(variant: Variant, playerUserIds: List[lila.user.User.ID]) = variant.standard && {
    try {
      lila.game.Env.current.pgnEncodingSetting.get() match {
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
  private[game] def apply(variant: Variant, playerUserIds: List[lila.user.User.ID]): PgnStorage =
    if (shouldUseHuffman(variant, playerUserIds)) Huffman else OldBin

  private def monitor[A](mon: lila.mon.game.pgn.Protocol)(f: => A): A = {
    mon.count()
    lila.mon.measureRec(mon.time)(f)
  }
}
