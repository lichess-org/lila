package lila.game

import chess.{ Centis, Clock, White }
import scalalib.Maths

import lila.db.ByteArray

class BinaryClockTest extends munit.FunSuite:

  val _0_ = "00000000"
  val since = nowInstant.minusHours(1)
  def writeBytes(c: Clock) = BinaryFormat.clock(since).write(c)
  def readBytes(bytes: ByteArray, berserk: Boolean = false): Clock =
    (BinaryFormat.clock(since).read(bytes, berserk, false))(White)
  def isomorphism(c: Clock): Clock = readBytes(writeBytes(c))

  def write(c: Clock): List[String] = writeBytes(c).showBytes.split(',').toList
  def read(bytes: List[String]) = readBytes(ByteArray.parseBytes(bytes))

  given Conversion[Int, Clock.LimitSeconds] = Clock.LimitSeconds(_)
  given Conversion[Int, Clock.IncrementSeconds] = Clock.IncrementSeconds(_)

  val clock = Clock(120, 2)
  val bits22 = List("00000010", "00000010")
  test("write"):
    assertEquals(write(clock), bits22 ::: List.fill(6)(_0_))
    assertEquals(
      write(clock.giveTime(White, Centis(3))),
      bits22 ::: List("10000000", "00000000", "00000011") ::: List.fill(3)(_0_)
    )
    assertEquals(
      write(clock.giveTime(White, Centis(-3))),
      bits22 ::: List("00000000", "00000000", "00000011") ::: List.fill(3)(_0_)
    )
    assertEquals(
      write(Clock(0, 3)),
      List("00000000", "00000011", "10000000", "00000001", "00101100", "10000000", "00000001", "00101100")
    )
  test("read with timer"):
    assertEquals(read(bits22 ::: List.fill(11)(_0_)), clock)
    assertEquals(
      read(bits22 ::: List("10000000", "00000000", "00000011") ::: List.fill(8)(_0_)),
      clock.giveTime(White, Centis(3))
    )
    assertEquals(
      read(bits22 ::: List("00000000", "00000000", "00000011") ::: List.fill(8)(_0_)),
      clock.giveTime(White, Centis(-3))
    )
  test("read without timer bytes"):
    assertEquals(read(bits22 ::: List.fill(7)(_0_)), clock)
    assertEquals(
      read(bits22 ::: List("10000000", "00000000", "00000011") ::: List.fill(4)(_0_)),
      clock.giveTime(White, Centis(3))
    )
    assertEquals(
      read(bits22 ::: List("00000000", "00000000", "00000011") ::: List.fill(4)(_0_)),
      clock.giveTime(White, Centis(-3))
    )

  test("isomorphism without berserk"):
    assertEquals(isomorphism(clock), clock)

    val c2 = clock.giveTime(White, Centis.ofSeconds(15))
    assertEquals(isomorphism(c2), c2)

    val c3 = clock.giveTime(chess.Black, Centis.ofSeconds(5))
    assertEquals(isomorphism(c3), c3)

    val c4 = clock.start
    assert(Maths.isCloseTo(isomorphism(c4).timer.get.value, c4.timer.get.value, 10d))

    Clock(120, 60).pipe { c =>
      assertEquals(isomorphism(c), c)
    }

  test("isomorphism with berserk"):
    val b1 = clock.goBerserk(White)
    assertEquals(readBytes(writeBytes(b1), true), b1)

    val b2 = clock.giveTime(White, Centis(15)).goBerserk(White)
    assertEquals(readBytes(writeBytes(b2), true), b2)

    val b3 = Clock(60, 2).goBerserk(White)
    assertEquals(readBytes(writeBytes(b3), true), b3)
