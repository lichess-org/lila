package lila.game

import shogi._
import shogi.Pos._
import org.specs2.mutable._

import lila.db.ByteArray
import shogi.variant.Standard

class BinaryPieceTest extends Specification {

  val noop = "00000000"
  def write(all: PieceMap): List[String] =
    (BinaryFormat.piece write all).showBytes.split(',').toList
  def read(bytes: List[String]): PieceMap =
    BinaryFormat.piece.read(ByteArray.parseBytes(bytes), Standard)

  "binary pieces" should {
    "write" should {
      "empty board" in {
        write(Map.empty) must_== List.fill(81)(noop)
      }
      "A1 sente king" in {
        write(Map(A1 -> Sente.king)) must_== {
          "00000001" :: List.fill(80)(noop)
        }
      }
      "A1 gote knight" in {
        write(Map(A1 -> Gote.knight)) must_== {
          "00010100" :: List.fill(80)(noop)
        }
      }
      "B1 gote pawn" in {
        write(Map(B1 -> Gote.pawn)) must_== {
          noop :: "00011110" :: List.fill(79)(noop)
        }
      }
      "A1 gote knight, B1 sente bishop" in {
        write(Map(A1 -> Gote.knight, B1 -> Sente.bishop)) must_== {
          "00010100" :: "00000110" :: List.fill(79)(noop)
        }
      }
      "i9 gote knight" in {
        write(Map(I9 -> Gote.knight)) must_== {
          List.fill(80)(noop) :+ "00010100"
        }
      }
      "H9 gote knight, I9 sente bishop" in {
        write(Map(H9 -> Gote.knight, I9 -> Sente.bishop)) must_== {
          List.fill(79)(noop) :+ "00010100" :+ "00000110"
        }
      }
    }
    "read" should {
      "empty board" in {
        read(List.fill(81)(noop)) must_== Map.empty
        "A1 sente king" in {
          read("00000001" :: List.fill(80)(noop)) must_== Map(A1 -> Sente.king)
        }
        "B1 gote pawn" in {
          read(noop :: "00011110" :: List.fill(79)(noop)) must_== Map(B1 -> Gote.pawn)
        }
      }
    }
  }
}
