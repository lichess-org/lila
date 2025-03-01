package lila.game

import chess.*
import chess.Square.*

import lila.db.ByteArray

class BinaryUnmovedRooksTest extends munit.FunSuite:

  val _0_ = "00000000"
  val _1_ = "11111111"
  def write(all: UnmovedRooks): List[String] =
    (BinaryFormat.unmovedRooks.write(all)).showBytes.split(',').toList
  def read(bytes: List[String]): UnmovedRooks =
    BinaryFormat.unmovedRooks.read(ByteArray.parseBytes(bytes))

  test("write"):
    assertEquals(write(UnmovedRooks(Set(A1, H1, A8, H8))), List("10000001", "10000001"))
    assertEquals(write(UnmovedRooks(Set.empty)), List(_0_, _0_))
    assertEquals(write(UnmovedRooks.default), List(_1_, _1_))
    assertEquals(write(UnmovedRooks(Set(A1, B1, C1))), List("11100000", _0_))
    assertEquals(write(UnmovedRooks(Set(A8, B8, C8))), List(_0_, "11100000"))
  test("read"):
    assertEquals(read(List("10000001", "10000001")), UnmovedRooks(Set(A1, H1, A8, H8)))
    assertEquals(read(List(_0_, _0_)), UnmovedRooks(Set.empty))
    assertEquals(read(List(_1_, _1_)), UnmovedRooks.default)
    assertEquals(read(List("11100000", _0_)), UnmovedRooks(Set(A1, B1, C1)))
    assertEquals(read(List(_0_, "11100000")), UnmovedRooks(Set(A8, B8, C8)))
