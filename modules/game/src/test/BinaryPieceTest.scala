package lila.game

import chess.*
import chess.Square.*
import chess.variant.Standard

import lila.db.ByteArray

class BinaryPieceTest extends munit.FunSuite:

  val noop = "00000000"
  def write(all: PieceMap): List[String] =
    (BinaryFormat.piece.write(all)).showBytes.split(',').toList
  def read(bytes: List[String]): PieceMap =
    BinaryFormat.piece.read(ByteArray.parseBytes(bytes), Standard)

  test("write empty board"):
    assertEquals(write(Map.empty), List.fill(32)(noop))
  test("write A1 white king"):
    assertEquals(write(Map(A1 -> White.king)), "00010000" :: List.fill(31)(noop))
  test("write A1 black knight"):
    assertEquals(write(Map(A1 -> Black.knight)), "11000000" :: List.fill(31)(noop))
  test("write B1 black pawn"):
    assertEquals(write(Map(B1 -> Black.pawn)), "00001110" :: List.fill(31)(noop))
  test("write A1 black knight, B1 white bishop"):
    assertEquals(write(Map(A1 -> Black.knight, B1 -> White.bishop)), "11000101" :: List.fill(31)(noop))
  test("write A1 black knight, B1 white bishop, C1 white queen"):
    assertEquals(
      write(Map(A1 -> Black.knight, B1 -> White.bishop, C1 -> White.queen)),
      "11000101" :: "00100000" :: List.fill(30)(noop)
    )
  test("write H8 black knight"):
    assertEquals(write(Map(H8 -> Black.knight)), List.fill(31)(noop) :+ "00001100")
  test("write G8 black knight, H8 white bishop"):
    assertEquals(write(Map(G8 -> Black.knight, H8 -> White.bishop)), List.fill(31)(noop) :+ "11000101")
  test("read empty board"):
    assertEquals(read(List.fill(32)(noop)), Map.empty)
  test("read A1 white king"):
    assertEquals(read("00010000" :: List.fill(31)(noop)), Map(A1 -> White.king))
  test("read B1 black pawn"):
    assertEquals(read("00001110" :: List.fill(31)(noop)), Map(B1 -> Black.pawn))
