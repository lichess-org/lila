package lila.game

import chess._
import chess.Pos._
import org.specs2.mutable.*

import lila.db.ByteArray
import chess.variant.Standard

class BinaryPieceTest extends Specification {

  val noop = "00000000"
  def write(all: PieceMap): List[String] =
    (BinaryFormat.piece write all).showBytes.split(',').toList
  def read(bytes: List[String]): PieceMap =
    BinaryFormat.piece.read(ByteArray.parseBytes(bytes), Standard)

  "binary pieces" >> {
    "write" >> {
      "empty board" >> {
        write(Map.empty) === List.fill(32)(noop)
      }
      "A1 white king" >> {
        write(Map(A1 -> White.king)) === {
          "00010000" :: List.fill(31)(noop)
        }
      }
      "A1 black knight" >> {
        write(Map(A1 -> Black.knight)) === {
          "11000000" :: List.fill(31)(noop)
        }
      }
      "B1 black pawn" >> {
        write(Map(B1 -> Black.pawn)) === {
          "00001110" :: List.fill(31)(noop)
        }
      }
      "A1 black knight, B1 white bishop" >> {
        write(Map(A1 -> Black.knight, B1 -> White.bishop)) === {
          "11000101" :: List.fill(31)(noop)
        }
      }
      "A1 black knight, B1 white bishop, C1 white queen" >> {
        write(Map(A1 -> Black.knight, B1 -> White.bishop, C1 -> White.queen)) === {
          "11000101" :: "00100000" :: List.fill(30)(noop)
        }
      }
      "H8 black knight" >> {
        write(Map(H8 -> Black.knight)) === {
          List.fill(31)(noop) :+ "00001100"
        }
      }
      "G8 black knight, H8 white bishop" >> {
        write(Map(G8 -> Black.knight, H8 -> White.bishop)) === {
          List.fill(31)(noop) :+ "11000101"
        }
      }
    }
    "read" >> {
      "empty board" >> {
        read(List.fill(32)(noop)) === Map.empty
        "A1 white king" >> {
          read("00010000" :: List.fill(31)(noop)) === Map(A1 -> White.king)
        }
        "B1 black pawn" >> {
          read("00001110" :: List.fill(31)(noop)) === Map(B1 -> Black.pawn)
        }
      }
    }
  }
}
