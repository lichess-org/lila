package lila.game

import org.joda.time.DateTime
import scala.util.Try

import shogi.format.forsyth.Sfen
import shogi.format.usi.Usi
import shogi.variant.Variant
import shogi.{ Centis, Clock, ClockPlayer, Color, Gote, Piece, PieceMap, Sente, Situation, Timestamp }
import org.lishogi.compression.clock.{ Encoder => ClockEncoder }

import lila.db.ByteArray

object BinaryFormat {
  object usi {
    def write(usis: Usis, variant: Variant): ByteArray =
      ByteArray {
        shogi.format.usi.Binary.encode(usis, variant)
      }

    def read(ba: ByteArray, variant: Variant): Usis =
      shogi.format.usi.Binary.decode(ba.value.toList, variant, Game.maxPlies(variant))

  }

  object clockHistory {
    private val logger = lila.log("clockHistory")

    def writeSide(start: Centis, times: Vector[Centis], flagged: Boolean) = {
      val timesToWrite = if (flagged) times.dropRight(1) else times
      ByteArray(ClockEncoder.encode(timesToWrite.view.map(_.centis).to(Array), start.centis))
    }

    def readSide(start: Centis, ba: ByteArray, flagged: Boolean) = {
      val decoded: Vector[Centis] =
        ClockEncoder.decode(ba.value, start.centis).view.map(Centis.apply).to(Vector)
      if (flagged) decoded :+ Centis(0) else decoded
    }

    def read(start: Centis, bs: ByteArray, bg: ByteArray, pe: PeriodEntries, flagged: Option[Color]) =
      Try {
        ClockHistory(
          readSide(start, bs, flagged has Sente),
          readSide(start, bg, flagged has Gote),
          pe
        )
      }.fold(
        e => { logger.warn(s"Exception decoding history", e); none },
        some
      )
  }

  object moveTime {

    private type MT = Int // centiseconds
    private val size = 16
    private val buckets =
      List(10, 50, 100, 150, 200, 300, 400, 500, 600, 800, 1000, 1500, 2000, 3000, 4000, 6000)
    private val encodeCutoffs = buckets zip buckets.tail map { case (i1, i2) =>
      (i1 + i2) / 2
    } toVector

    private val decodeMap: Map[Int, MT] = buckets.view.zipWithIndex.map(x => x._2 -> x._1).toMap

    def write(mts: Vector[Centis]): ByteArray = {
      def enc(mt: Centis) = encodeCutoffs.search(mt.centis).insertionPoint
      (mts
        .grouped(2)
        .map {
          case Vector(a, b) => (enc(a) << 4) + enc(b)
          case Vector(a)    => enc(a) << 4
          case v            => sys error s"moveTime.write unexpected $v"
        })
        .map(_.toByte)
        .toArray
    }

    def read(ba: ByteArray, plies: Int): Vector[Centis] = {
      def dec(x: Int) = decodeMap.getOrElse(x, decodeMap(size - 1))
      ba.value map toInt flatMap { k =>
        Array(dec(k >> 4), dec(k & 15))
      }
    }.view.take(plies).map(Centis.apply).toVector
  }

  case class clock(start: Timestamp) {

    def legacyElapsed(clock: Clock, color: Color) =
      clock.limit - clock.players(color).remaining

    def computeRemaining(config: Clock.Config, legacyElapsed: Centis) =
      config.limit - legacyElapsed

    def write(clock: Clock): ByteArray = {
      Array(writeClockLimit(clock.limitSeconds), clock.incrementSeconds.toByte) ++
        writeSignedInt24(legacyElapsed(clock, Sente).centis) ++
        writeSignedInt24(legacyElapsed(clock, Gote).centis) ++
        clock.timer.fold(Array.empty[Byte])(writeTimer) ++ Array(
          clock.byoyomiSeconds.toByte,
          clock.periodsTotal.toByte
        )
    }

    def read(
        ba: ByteArray,
        periodEntries: PeriodEntries,
        senteBerserk: Boolean,
        goteBerserk: Boolean
    ): Color => Clock =
      color => {
        val ia   = ba.value map toInt
        val size = ia.sizeIs

        // ba.size might be greater than 12 with 5 bytes timers
        // ba.size might be 8 if there was no timer.
        // #TODO remove 5 byte timer case! But fix the DB first!
        val timer = {
          if (size >= 12) readTimer(readInt(ia(8), ia(9), ia(10), ia(11)))
          else None
        }

        val byo = {
          if (size == 14) ia(12)
          else if (size == 10) ia(8)
          else 0
        }

        val per = {
          if (size == 14) ia(13)
          else if (size == 10) ia(9)
          else 1
        }

        ia match {
          case Array(b1, b2, b3, b4, b5, b6, b7, b8, _*) => {
            val config      = Clock.Config(readClockLimit(b1), b2, byo, per)
            val legacySente = Centis(readSignedInt24(b3, b4, b5))
            val legacyGote  = Centis(readSignedInt24(b6, b7, b8))
            Clock(
              config = config,
              color = color,
              players = Color.Map(
                ClockPlayer
                  .withConfig(config)
                  .copy(berserk = senteBerserk)
                  .setRemaining(computeRemaining(config, legacySente))
                  .setPeriods(periodEntries(Sente).size atLeast config.initPeriod),
                ClockPlayer
                  .withConfig(config)
                  .copy(berserk = goteBerserk)
                  .setRemaining(computeRemaining(config, legacyGote))
                  .setPeriods(periodEntries(Gote).size atLeast config.initPeriod)
              ),
              timer = timer
            )
          }
          case _ => sys error s"BinaryFormat.clock.read invalid bytes: ${ba.showBytes}"
        }
      }

    private def writeTimer(timer: Timestamp) = {
      val centis = (timer - start).centis
      /*
       * A zero timer is resolved by `readTimer` as the absence of a timer.
       * As a result, a clock that is started with a timer = 0
       * resolves as a clock that is not started.
       * This can happen when the clock was started at the same time as the game
       * For instance in simuls
       */
      val nonZero = centis atLeast 1
      writeInt(nonZero)
    }

    private def readTimer(l: Int) =
      if (l != 0) Some(start + Centis(l)) else None

    private def writeClockLimit(limit: Int): Byte = {
      // The database expects a byte for a limit, and this is limit / 60.
      // For 0.5+0, this does not give a round number, so there needs to be
      // an alternative way to describe 0.5.
      // The max limit where limit % 60 == 0, returns 180 for limit / 60
      // So, for the limits where limit % 30 == 0, we can use the space
      // from 181-255, where 181 represents 0.25 and 182 represents 0.50...
      (if (limit % 60 == 0) limit / 60 else limit / 15 + 180).toByte
    }

    private def readClockLimit(i: Int) = {
      if (i < 181) i * 60 else (i - 180) * 15
    }
  }

  object clock {
    def apply(start: DateTime) = new clock(Timestamp(start.getMillis))
  }

  object periodEntries {
    private val logger = lila.log("periodEntries")

    def writeSide(v: Vector[Int]): ByteArray = {
      def intToShort(i: Int): Array[Byte] = Array((i >> 8).toByte, i.toByte)
      (v.flatMap(intToShort _)).toArray
    }
    def readSide(ba: ByteArray): Vector[Int] = {
      def backToInt(b: Array[Byte]): Int =
        b map toInt match {
          case Array(b1, b2) => (b1 << 8) + b2
          case _             => 0
        }
      val pairs = ba.value.grouped(2)
      (pairs map (backToInt _)).toVector
    }
    def read(bs: ByteArray, bg: ByteArray) =
      Try {
        PeriodEntries(readSide(bs), readSide(bg))
      }.fold(
        e => { logger.warn(s"Exception decoding period entries", e); none },
        some
      )
  }

  object pieces {
    def read(
        usis: Usis,
        initialSfen: Option[Sfen],
        variant: Variant
    ): PieceMap = {
      val init = initialSfen.flatMap { sfen =>
        sfen.toSituation(variant)
      } | Situation(variant)
      val mm    = collection.mutable.Map(init.board.pieces.toSeq: _*)
      var color = init.color
      usis.foreach { case usi =>
        usi match {
          case Usi.Move(orig, dest, prom, None) => {
            mm.remove(orig) map { piece =>
              val mp = piece.updateRole(variant.promote).filter(_ => prom).getOrElse(piece)
              mm update (dest, mp)
              color = !color
            }
          }
          case Usi.Move(orig, dest, prom, Some(ms)) => {
            mm.remove(orig) map { piece =>
              val mp = piece.updateRole(variant.promote).filter(_ => prom).getOrElse(piece)
              mm update (dest, mp)
              mm remove ms
              color = !color
            }
          }
          case Usi.Drop(role, pos) => {
            mm update (pos, Piece(color, role))
            color = !color
          }
        }
      }
      mm.toMap
    }
  }

  @inline private def toInt(b: Byte): Int = b & 0xff

  def writeInt24(int: Int) = {
    val i = if (int < (1 << 24)) int else 0
    Array((i >>> 16).toByte, (i >>> 8).toByte, i.toByte)
  }

  private val int23Max = 1 << 23
  def writeSignedInt24(int: Int) = {
    val i = if (int < 0) int23Max - int else math.min(int, int23Max)
    writeInt24(i)
  }

  def readInt24(b1: Int, b2: Int, b3: Int) = (b1 << 16) | (b2 << 8) | b3

  def readSignedInt24(b1: Int, b2: Int, b3: Int) = {
    val i = readInt24(b1, b2, b3)
    if (i > int23Max) int23Max - i else i
  }

  def writeInt(i: Int) =
    Array(
      (i >>> 24).toByte,
      (i >>> 16).toByte,
      (i >>> 8).toByte,
      i.toByte
    )

  def readInt(b1: Int, b2: Int, b3: Int, b4: Int) = {
    (b1 << 24) | (b2 << 16) | (b3 << 8) | b4
  }
}
