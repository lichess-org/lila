package lila.game

import chess.{ Pos, Piece, AllPieces }

object BinaryFormat {

  object piece {

    def decode(str: List[Byte]): AllPieces =
      ???

    def encode(all: AllPieces): List[Byte] =
      ???
  }
}
