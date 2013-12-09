package lila.game

import scala.concurrent.duration._

import chess._
import chess.Pos._
import org.specs2.mutable._
import org.specs2.specification._

import lila.db.ByteArray

class BinaryPieceTest extends Specification {

  val noop = "00000000"
  def write(all: AllPieces): List[String] =
    (BinaryFormat.piece write all).showBytes.split(',').toList
  def read(bytes: List[String]): AllPieces =
    BinaryFormat.piece read ByteArray.parseBytes(bytes)

  "binary pieces" should {
    "write" should {
      "empty board" in {
        write(Map.empty -> Nil) must_== List.fill(32)(noop)
      }
      "A1 white king" in {
        write(Map(A1 -> White.king) -> Nil) must_== {
          "00010000" :: List.fill(31)(noop)
        }
      }
      "A1 black knight" in {
        write(Map(A1 -> Black.knight) -> Nil) must_== {
          "11000000" :: List.fill(31)(noop)
        }
      }
      "B1 black pawn" in {
        write(Map(B1 -> Black.pawn) -> Nil) must_== {
          "00001110" :: List.fill(31)(noop)
        }
      }
      "A1 black knight, B1 white bishop" in {
        write(Map(A1 -> Black.knight, B1 -> White.bishop) -> Nil) must_== {
          "11000101" :: List.fill(31)(noop)
        }
      }
      "A1 black knight, B1 white bishop, C1 white queen" in {
        write(Map(A1 -> Black.knight, B1 -> White.bishop, C1 -> White.queen) -> Nil) must_== {
          "11000101" :: "00100000" :: List.fill(30)(noop)
        }
      }
      "H8 black knight" in {
        write(Map(H8 -> Black.knight) -> Nil) must_== {
          List.fill(31)(noop) :+ "00001100"
        }
      }
      "G8 black knight, H8 white bishop" in {
        write(Map(G8 -> Black.knight, H8 -> White.bishop) -> Nil) must_== {
          List.fill(31)(noop) :+ "11000101"
        }
      }
      "dead black knight" in {
        write(Map.empty, List(Black.knight)) must_== {
          List.fill(32)(noop) :+ "11000000"
        }
      }
      "B1 black pawn, dead black knight, dead white queen, dead black pawn" in {
        write(Map(B1 -> Black.pawn) -> List(Black.knight, White.queen, Black.pawn)) must_== {
          ("00001110" :: List.fill(31)(noop)) :+ "11000010" :+ "11100000"
        }
      }
    }
    "read" should {
      "empty board" in {
        read(List.fill(32)(noop)) must_== {
          Map.empty -> Nil
        }
        "A1 white king" in {
          read("00010000" :: List.fill(31)(noop)) must_== {
            Map(A1 -> White.king) -> Nil
          }
        }
        "B1 black pawn" in {
          read("00001110" :: List.fill(31)(noop)) must_== {
            Map(B1 -> Black.pawn) -> Nil
          }
        }
        "dead black knight" in {
          read(List.fill(32)(noop) :+ "11000000") must_== {
            Map.empty -> List(Black.knight)
          }
        }
        "B1 black pawn, dead black knight, dead white queen, dead black pawn" in {
          read(("00001110" :: List.fill(31)(noop)) :+ "11000010" :+ "11100000") must_== {
            Map(B1 -> Black.pawn) -> List(Black.knight, White.queen, Black.pawn)
          }
        }
      }
    }
  }
}
