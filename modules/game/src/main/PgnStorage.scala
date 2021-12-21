package lila.game

import shogi._
import shogi.format.Usi
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

  case class Decoded(
      pgnMoves: PgnMoves,
      pieces: PieceMap,
      positionHashes: PositionHash, // irrelevant after game ends
      checkCount: List[Int],
      lastMove: Option[Usi],
      hands: Option[Hands]
  )

  private def monitor[A](mon: lila.mon.TimerPath)(f: => A): A =
    lila.common.Chronometer.syncMon(mon)(f)
}
