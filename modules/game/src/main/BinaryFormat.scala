package lila.game

import org.joda.time.DateTime
import scala.collection.breakOut
import scala.collection.Searching._
import scala.collection.breakOut
import scala.util.Try

import chess._
import chess.variant.Variant

import lila.db.ByteArray

import org.lichess.clockencoder.{ Encoder => ClockEncoder }

object BinaryFormat {

  object pgn {

    def write(moves: PgnMoves): ByteArray = ByteArray {
      format.pgn.Binary.writeMoves(moves).get.toArray
    }

    def read(ba: ByteArray): PgnMoves =
      format.pgn.Binary.readMoves(ba.value.toList).get

    def read(ba: ByteArray, nb: Int): PgnMoves =
      format.pgn.Binary.readMoves(ba.value.toList, nb).get
  }

  object clockHistory {
    private val logger = lila.log("clockHistory")

    def writeSide(start: Centis, times: Vector[Centis], flagged: Boolean) = {
      val timesToWrite = if (flagged) times.dropRight(1) else times
      ByteArray(ClockEncoder.encode(timesToWrite.map(_.centis)(breakOut), start.centis))
    }

    def readSide(start: Centis, ba: ByteArray, flagged: Boolean) = {
      val decoded: Vector[Centis] = ClockEncoder.decode(ba.value, start.centis).map(Centis.apply)(breakOut)
      if (flagged) decoded :+ Centis(0) else decoded
    }

    def read(start: Centis, bw: ByteArray, bb: ByteArray, flagged: Option[Color], gameId: String) = Try {
      ClockHistory(
        readSide(start, bw, flagged has White),
        readSide(start, bb, flagged has Black)
      )
    }.fold(
      e => { logger.warn(s"Exception decoding history on game $gameId", e); none },
      some
    )
  }

  object moveTime {

    private type MT = Int // centiseconds
    private val size = 16
    private val buckets = List(10, 50, 100, 150, 200, 300, 400, 500, 600, 800, 1000, 1500, 2000, 3000, 4000, 6000)
    private val encodeCutoffs = buckets zip buckets.tail map {
      case (i1, i2) => (i1 + i2) / 2
    } toVector

    private val decodeMap: Map[Int, MT] = buckets.zipWithIndex.map(x => x._2 -> x._1)(breakOut)

    def write(mts: Vector[Centis]): ByteArray = ByteArray {
      def enc(mt: Centis) = encodeCutoffs.search(mt.centis).insertionPoint
      (mts.grouped(2).map {
        case Vector(a, b) => (enc(a) << 4) + enc(b)
        case Vector(a) => enc(a) << 4
      }).map(_.toByte).toArray
    }

    def read(ba: ByteArray, turns: Int): Vector[Centis] = {
      def dec(x: Int) = decodeMap get x getOrElse decodeMap(size - 1)
      ba.value map toInt flatMap { k =>
        Array(dec(k >> 4), dec(k & 15))
      }
    }.take(turns).map(Centis.apply)(breakOut)
  }

  case class clock(since: DateTime) {

    def write(clock: Clock): ByteArray = {
      ByteArray {
        Array(writeClockLimit(clock.limitSeconds), writeInt8(clock.incrementSeconds)) ++
          writeSignedInt24(clock.whiteTime.centis) ++
          writeSignedInt24(clock.blackTime.centis) ++
          writeTimer(clock.timerOption.fold(0l)(_.value / 10l)) map { _.toByte }
      }
    }

    def read(ba: ByteArray, whiteBerserk: Boolean, blackBerserk: Boolean): Color => Clock = color => ba.value map toInt match {
      case Array(b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12) =>
        readTimer(b9, b10, b11, b12) match {
          case 0 => PausedClock(
            config = Clock.Config(readClockLimit(b1), b2),
            color = color,
            whiteTime = Centis(readSignedInt24(b3, b4, b5)),
            blackTime = Centis(readSignedInt24(b6, b7, b8)),
            whiteBerserk = whiteBerserk,
            blackBerserk = blackBerserk
          )
          case timer => RunningClock(
            config = Clock.Config(readClockLimit(b1), b2),
            color = color,
            whiteTime = Centis(readSignedInt24(b3, b4, b5)),
            blackTime = Centis(readSignedInt24(b6, b7, b8)),
            whiteBerserk = whiteBerserk,
            blackBerserk = blackBerserk,
            timer = Timestamp(timer * 10l)
          )
        }
      // compatibility with 5 bytes timers
      // #TODO remove me! But fix the DB first!
      case Array(b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12, _) =>
        PausedClock(
          config = Clock.Config(readClockLimit(b1), b2),
          color = color,
          whiteTime = Centis(readSignedInt24(b3, b4, b5)),
          blackTime = Centis(readSignedInt24(b6, b7, b8)),
          whiteBerserk = whiteBerserk,
          blackBerserk = blackBerserk
        )
      case x => sys error s"BinaryFormat.clock.read invalid bytes: ${ba.showBytes}"
    }

    private def decay = (since.getMillis / 10) - 10

    private def writeTimer(long: Long) = {
      val i = math.max(0, long - decay).toInt
      Array(i >> 24, (i >> 16) & 255, (i >> 8) & 255, i & 255)
    }
    private def readTimer(b1: Int, b2: Int, b3: Int, b4: Int) = {
      val l = (b1 << 24) + (b2 << 16) + (b3 << 8) + b4
      if (l == 0) 0 else l + decay
    }

    private def writeClockLimit(limit: Int) = {
      // The database expects a byte for a limit, and this is limit / 60.
      // For 0.5+0, this does not give a round number, so there needs to be
      // an alternative way to describe 0.5.
      // The max limit where limit % 60 == 0, returns 180 for limit / 60
      // So, for the limits where limit % 30 == 0, we can use the space
      // from 181-255, where 181 represents 0.5, 182 represents 0.75 and
      // 185 represents 1.5.
      if (limit % 60 == 0) limit / 60 else (limit - 15) / 15 + 181
    }

    private def readClockLimit(b: Int) = {
      if (b < 181) b * 60 else (b - 181) * 15 + 15
    }
  }

  object castleLastMoveTime {

    def write(clmt: CastleLastMoveTime): ByteArray = {

      val castleInt = clmt.castles.toList.zipWithIndex.foldLeft(0) {
        case (acc, (false, _)) => acc
        case (acc, (true, p)) => acc + (1 << (3 - p))
      }

      def posInt(pos: Pos): Int = ((pos.x - 1) << 3) + pos.y - 1
      val lastMoveInt = clmt.lastMove.fold(0) {
        case (f, t) => (posInt(f) << 6) + posInt(t)
      }
      val time = clmt.lastMoveTime getOrElse 0

      val ints = Array(
        (castleInt << 4) + (lastMoveInt >> 8),
        (lastMoveInt & 255)
      ) ++ writeInt24(time) ++ clmt.check.map(posInt)

      ByteArray(ints.map(_.toByte))
    }

    def read(ba: ByteArray): CastleLastMoveTime = {
      ba.value map toInt match {
        case Array(b1, b2, b3, b4, b5) => doRead(b1, b2, b3, b4, b5, None)
        case Array(b1, b2, b3, b4, b5, b6) => doRead(b1, b2, b3, b4, b5, b6.some)
        case x => sys error s"BinaryFormat.clmt.read invalid bytes: ${ba.showBytes}"
      }
    }

    private def posAt(x: Int, y: Int) = Pos.posAt(x + 1, y + 1)

    private def doRead(b1: Int, b2: Int, b3: Int, b4: Int, b5: Int, b6: Option[Int]) =
      CastleLastMoveTime(
        castles = Castles(b1 > 127, (b1 & 64) != 0, (b1 & 32) != 0, (b1 & 16) != 0),
        lastMove = for {
          from ← posAt((b1 & 15) >> 1, ((b1 & 1) << 2) + (b2 >> 6))
          to ← posAt((b2 & 63) >> 3, b2 & 7)
          if from != Pos.A1 || to != Pos.A1
        } yield from -> to,
        lastMoveTime = readInt24(b3, b4, b5).some filter (0 !=),
        check = b6 flatMap { x => posAt(x >> 3, x & 7) }
      )
  }

  object piece {

    private val groupedPos = Pos.all grouped 2 collect {
      case List(p1, p2) => (p1, p2)
    } toArray

    def write(pieces: PieceMap): ByteArray = {
      def posInt(pos: Pos): Int = (pieces get pos).fold(0) { piece =>
        piece.color.fold(0, 8) + roleToInt(piece.role)
      }
      ByteArray(groupedPos map {
        case (p1, p2) => ((posInt(p1) << 4) + posInt(p2)).toByte
      })
    }

    def read(ba: ByteArray, variant: Variant): PieceMap = {
      def splitInts(b: Byte) = {
        val int = b.toInt
        Array(int >> 4, int & 0x0F)
      }
      def intPiece(int: Int): Option[Piece] =
        intToRole(int & 7, variant) map { role => Piece(Color((int & 8) == 0), role) }
      val pieceInts = ba.value flatMap splitInts
      (Pos.all zip pieceInts).flatMap {
        case (pos, int) => intPiece(int) map (pos -> _)
      }(breakOut)
    }

    // cache standard start position
    val standard = write(Board.init(chess.variant.Standard).pieces)

    private def intToRole(int: Int, variant: Variant): Option[Role] = int match {
      case 6 => Some(Pawn)
      case 1 => Some(King)
      case 2 => Some(Queen)
      case 3 => Some(Rook)
      case 4 => Some(Knight)
      case 5 => Some(Bishop)
      // Legacy from when we used to have an 'Antiking' piece
      case 7 if variant.antichess => Some(King)
      case _ => None
    }
    private def roleToInt(role: Role): Int = role match {
      case Pawn => 6
      case King => 1
      case Queen => 2
      case Rook => 3
      case Knight => 4
      case Bishop => 5
    }
  }

  object unmovedRooks {

    val emptyByteArray = ByteArray(Array(0, 0))

    def write(o: UnmovedRooks): ByteArray = {
      if (o.pos.isEmpty) emptyByteArray
      else {
        var white = 0
        var black = 0
        o.pos.foreach { pos =>
          if (pos.y == 1) white = white | (1 << (8 - pos.x))
          else black = black | (1 << (8 - pos.x))
        }
        ByteArray(Array(white.toByte, black.toByte))
      }
    }

    private def bitAt(n: Int, k: Int) = (n >> k) & 1

    private val arrIndexes = 0 to 1
    private val bitIndexes = 0 to 7
    private val whiteStd = Set(Pos.A1, Pos.H1)
    private val blackStd = Set(Pos.A8, Pos.H8)

    def read(ba: ByteArray) = UnmovedRooks {
      var set = Set.empty[Pos]
      arrIndexes.foreach { i =>
        val int = ba.value(i).toInt
        if (int != 0) {
          if (int == -127) set = if (i == 0) whiteStd else set ++ blackStd
          else bitIndexes.foreach { j =>
            if (bitAt(int, j) == 1) set = set + Pos.posAt(8 - j, 1 + 7 * i).get
          }
        }
      }
      set
    }
  }

  @inline private def toInt(b: Byte): Int = b & 0xff

  def writeInt8(int: Int) = math.min(255, int)

  private val int24Max = math.pow(2, 24).toInt
  def writeInt24(int: Int) = {
    val i = math.min(int24Max, int)
    Array(i >> 16, (i >> 8) & 255, i & 255)
  }
  def readInt24(b1: Int, b2: Int, b3: Int) = (b1 << 16) + (b2 << 8) + b3

  private val int23Max = math.pow(2, 23).toInt
  def writeSignedInt24(int: Int) = {
    val i = math.abs(math.min(int23Max, int))
    val j = if (int < 0) i + int23Max else i
    Array(j >> 16, (j >> 8) & 255, j & 255)
  }
  def readSignedInt24(b1: Int, b2: Int, b3: Int) = {
    val i = (b1 << 16) + (b2 << 8) + b3
    if (i > int23Max) int23Max - i else i
  }
}
