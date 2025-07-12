package lila.game

import chess.*
import chess.format.Uci
import chess.format.pgn.SanStr
import chess.variant.Variant
import org.lichess.compression.clock.Encoder as ClockEncoder

import scala.util.Try

import lila.core.game.ClockHistory
import lila.db.ByteArray

object BinaryFormat:

  object pgn:

    def write(moves: Vector[SanStr]): ByteArray = ByteArray:
      format.pgn.Binary.writeMoves(moves).get

    def read(ba: ByteArray): Vector[SanStr] =
      format.pgn.Binary.readMoves(ba.value.toList).get.toVector

    def read(ba: ByteArray, nb: Int): Vector[SanStr] =
      format.pgn.Binary.readMoves(ba.value.toList, nb).get.toVector

  object clockHistory:
    private val logger = lila.log("clockHistory")

    def writeSide(start: Centis, times: Vector[Centis], flagged: Boolean) =
      val timesToWrite = if flagged then times.dropRight(1) else times
      ByteArray(ClockEncoder.encode(Centis.raw(timesToWrite).to(Array), start.centis))

    def readSide(start: Centis, ba: ByteArray, flagged: Boolean) =
      val decoded: Vector[Centis] =
        Centis.from(ClockEncoder.decode(ba.value, start.centis).to(Vector))
      if flagged then decoded :+ Centis(0) else decoded

    def read(start: Centis, bw: ByteArray, bb: ByteArray, flagged: Option[Color]) =
      Try {
        ClockHistory(
          readSide(start, bw, flagged.has(White)),
          readSide(start, bb, flagged.has(Black))
        )
      }.fold(
        e =>
          logger.warn(s"Exception decoding history", e); none
        ,
        some
      )

  object moveTime:

    private type MT = Int // centiseconds
    private val size    = 16
    private val buckets =
      List(10, 50, 100, 150, 200, 300, 400, 500, 600, 800, 1000, 1500, 2000, 3000, 4000, 6000)
    private val encodeCutoffs = buckets
      .zip(buckets.tail)
      .map((i1, i2) => (i1 + i2) / 2)
      .toVector

    private val decodeMap: Map[Int, MT] = buckets.mapWithIndex((x, i) => i -> x).toMap

    def write(mts: Vector[Centis]): Array[Byte] =
      def enc(mt: Centis) = encodeCutoffs.search(mt.centis).insertionPoint
      mts
        .grouped(2)
        .map:
          case Vector(a, b) => (enc(a) << 4) + enc(b)
          case Vector(a)    => enc(a) << 4
          case v            => sys.error(s"moveTime.write unexpected $v")
        .map(_.toByte)
        .toArray

    def read(ba: Array[Byte], turns: Ply): Vector[Centis] = Centis.from:
      def dec(x: Int) = decodeMap.getOrElse(x, decodeMap(size - 1))
      ba.map(toInt)
        .flatMap(k => Array(dec(k >> 4), dec(k & 15)))
        .view
        .take(turns.value)
        .toVector

  final class clock(start: Timestamp):

    def legacyElapsed(clock: Clock, color: Color) =
      clock.limit - clock.players(color).remaining

    def computeRemaining(config: Clock.Config, legacyElapsed: Centis) =
      config.limit - legacyElapsed

    def write(clock: Clock): ByteArray = ByteArray:
      Array(writeClockLimit(clock.limitSeconds.value), clock.incrementSeconds.value.toByte) ++
        writeSignedInt24(legacyElapsed(clock, White).centis) ++
        writeSignedInt24(legacyElapsed(clock, Black).centis) ++
        clock.timer.fold(Array.empty[Byte])(writeTimer)

    def read(ba: ByteArray, whiteBerserk: Boolean, blackBerserk: Boolean): Color => Clock =
      color =>
        val ia = ba.value.map(toInt)

        // ba.size might be greater than 12 with 5 bytes timers
        // ba.size might be 8 if there was no timer.
        // #TODO remove 5 byte timer case! But fix the DB first!
        val timer = (ia.lengthIs == 12).so(readTimer(readInt(ia(8), ia(9), ia(10), ia(11))))

        ia match
          case Array(b1, b2, b3, b4, b5, b6, b7, b8, _*) =>
            val config      = Clock.Config(clock.readClockLimit(b1), Clock.IncrementSeconds(b2))
            val legacyWhite = Centis(readSignedInt24(b3, b4, b5))
            val legacyBlack = Centis(readSignedInt24(b6, b7, b8))
            val players     = ByColor((whiteBerserk, legacyWhite), (blackBerserk, legacyBlack))
              .map: (berserk, legacy) =>
                ClockPlayer
                  .withConfig(config)
                  .copy(berserk = berserk)
                  .setRemaining(computeRemaining(config, legacy))
            Clock(
              config = config,
              color = color,
              players = players,
              timer = timer
            )
          case _ => sys.error(s"BinaryFormat.clock.read invalid bytes: ${ba.showBytes}")

    private def writeTimer(timer: Timestamp) =
      val centis = (timer - start).centis
      /*
       * A zero timer is resolved by `readTimer` as the absence of a timer.
       * As a result, a clock that is started with a timer = 0
       * resolves as a clock that is not started.
       * This can happen when the clock was started at the same time as the game
       * For instance in simuls
       */
      val nonZero = centis.atLeast(1)
      writeInt(nonZero)

    private def readTimer(l: Int) =
      Option.when(l != 0)(start + Centis(l))

    private def writeClockLimit(limit: Int): Byte =
      // The database expects a byte for a limit, and this is limit / 60.
      // For 0.5+0, this does not give a round number, so there needs to be
      // an alternative way to describe 0.5.
      // The max limit where limit % 60 == 0, returns 180 for limit / 60
      // So, for the limits where limit % 30 == 0, we can use the space
      // from 181-255, where 181 represents 0.25 and 182 represents 0.50...
      (if limit % 60 == 0 then limit / 60 else limit / 15 + 180).toByte

  object clock:
    def apply(start: Instant) = new clock(Timestamp(start.toMillis))

    def readConfig(ba: ByteArray): Option[Clock.Config] =
      ba.value match
        case Array(b1, b2, _*) => Clock.Config(readClockLimit(b1), Clock.IncrementSeconds(b2)).some
        case _                 => None

    def readClockLimit(i: Int) = Clock.LimitSeconds(if i < 181 then i * 60 else (i - 180) * 15)

  object castleLastMove:

    def write(clmt: CastleLastMove): ByteArray = ByteArray:

      val castleInt = clmt.castles.toSeq.zipWithIndex.foldLeft(0):
        case (acc, (false, _)) => acc
        case (acc, (true, p))  => acc + (1 << (3 - p))

      def posInt(pos: Square): Int = (pos.file.value << 3) + pos.rank.value
      val lastMoveInt              = clmt.lastMove.map(_.origDest).fold(0) { (o, d) =>
        (posInt(o) << 6) + posInt(d)
      }
      Array(((castleInt << 4) + (lastMoveInt >> 8)).toByte, lastMoveInt.toByte)

    def read(ba: ByteArray): CastleLastMove =
      val ints = ba.value.map(toInt)
      doRead(ints(0), ints(1))

    private def doRead(b1: Int, b2: Int) =
      CastleLastMove(
        castles = Castles(b1 > 127, (b1 & 64) != 0, (b1 & 32) != 0, (b1 & 16) != 0),
        lastMove = for
          orig <- Square.at((b1 & 15) >> 1, ((b1 & 1) << 2) + (b2 >> 6))
          dest <- Square.at((b2 & 63) >> 3, b2 & 7)
          if orig != Square.A1 || dest != Square.A1
        yield Uci.Move(orig, dest)
      )

  object piece:

    private val groupedPos = Square.all
      .grouped(2)
      .collect:
        case List(p1, p2) => (p1, p2)
      .toArray

    def write(pieces: PieceMap): ByteArray =
      def posInt(pos: Square): Int =
        (pieces
          .get(pos))
          .fold(0): piece =>
            piece.color.fold(0, 8) + roleToInt(piece.role)
      ByteArray:
        groupedPos.map: (p1, p2) =>
          ((posInt(p1) << 4) + posInt(p2)).toByte

    def read(ba: ByteArray, variant: Variant): PieceMap =
      def splitInts(b: Byte) =
        val int = b.toInt
        Array(int >> 4, int & 0x0f)
      def intPiece(int: Int): Option[Piece] =
        intToRole(int & 7, variant).map: role =>
          Piece(Color.fromWhite((int & 8) == 0), role)
      val pieceInts = ba.value.flatMap(splitInts)
      (Square.all
        .zip(pieceInts))
        .view
        .flatMap: (pos, int) =>
          intPiece(int).map(pos -> _)
        .to(Map)

    private def intToRole(int: Int, variant: Variant): Option[Role] =
      int match
        case 6 => Some(Pawn)
        case 1 => Some(King)
        case 2 => Some(Queen)
        case 3 => Some(Rook)
        case 4 => Some(Knight)
        case 5 => Some(Bishop)
        // Legacy from when we used to have an 'Antiking' piece
        case 7 if variant.antichess => Some(King)
        case _                      => None
    private def roleToInt(role: Role): Int =
      role match
        case Pawn   => 6
        case King   => 1
        case Queen  => 2
        case Rook   => 3
        case Knight => 4
        case Bishop => 5

  object unmovedRooks:

    val emptyByteArray = ByteArray(Array(0, 0))

    def write(o: UnmovedRooks): ByteArray =
      if o.isEmpty then emptyByteArray
      else
        ByteArray:
          var white = 0
          var black = 0
          o.toList.foreach: pos =>
            if pos.rank == Rank.First then white = white | (1 << (7 - pos.file.value))
            else black = black | (1 << (7 - pos.file.value))
          Array(white.toByte, black.toByte)

    private def bitAt(n: Int, k: Int) = (n >> k) & 1

    private val arrIndexes = 0 to 1
    private val bitIndexes = 0 to 7
    private val whiteStd   = Set(Square.A1, Square.H1)
    private val blackStd   = Set(Square.A8, Square.H8)

    def read(ba: ByteArray) =
      var set = Set.empty[Square]
      arrIndexes.foreach: i =>
        val int = ba.value(i).toInt
        if int != 0 then
          if int == -127 then set = if i == 0 then whiteStd else set ++ blackStd
          else
            bitIndexes.foreach: j =>
              if bitAt(int, j) == 1 then set = set + Square.at(7 - j, 7 * i).get
      UnmovedRooks(set)

  inline private def toInt(inline b: Byte): Int = b & 0xff

  def writeInt24(int: Int) =
    val i = if int < (1 << 24) then int else 0
    Array((i >>> 16).toByte, (i >>> 8).toByte, i.toByte)

  private val int23Max           = 1 << 23
  def writeSignedInt24(int: Int) =
    val i = if int < 0 then int23Max - int else math.min(int, int23Max)
    writeInt24(i)

  def readInt24(b1: Int, b2: Int, b3: Int) = (b1 << 16) | (b2 << 8) | b3

  def readSignedInt24(b1: Int, b2: Int, b3: Int) =
    val i = readInt24(b1, b2, b3)
    if i > int23Max then int23Max - i else i

  def writeInt(i: Int) =
    Array(
      (i >>> 24).toByte,
      (i >>> 16).toByte,
      (i >>> 8).toByte,
      i.toByte
    )

  def readInt(b1: Int, b2: Int, b3: Int, b4: Int) =
    (b1 << 24) | (b2 << 16) | (b3 << 8) | b4
