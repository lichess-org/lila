package lila.game

import org.joda.time.DateTime
import reactivemongo.bson._
import scala.collection.breakOut

import chess.variant.{ Variant, Crazyhouse }
import chess.{ CheckCount, Color, Clock, White, Black, Status, Mode, UnmovedRooks }

import lila.db.BSON
import lila.db.dsl._

object BSONHandlers {

  import lila.db.ByteArray.ByteArrayBSONHandler

  private[game] implicit val checkCountWriter = new BSONWriter[CheckCount, BSONArray] {
    def write(cc: CheckCount) = BSONArray(cc.white, cc.black)
  }

  implicit val StatusBSONHandler = new BSONHandler[BSONInteger, Status] {
    def read(bsonInt: BSONInteger): Status = Status(bsonInt.value) err s"No such status: ${bsonInt.value}"
    def write(x: Status) = BSONInteger(x.id)
  }

  private[game] implicit val unmovedRooksHandler = new BSONHandler[BSONBinary, UnmovedRooks] {
    def read(bin: BSONBinary): UnmovedRooks = BinaryFormat.unmovedRooks.read {
      ByteArrayBSONHandler.read(bin)
    }
    def write(x: UnmovedRooks): BSONBinary = ByteArrayBSONHandler.write {
      BinaryFormat.unmovedRooks.write(x)
    }
  }

  private[game] implicit val crazyhouseDataBSONHandler = new BSON[Crazyhouse.Data] {

    import Crazyhouse._

    def reads(r: BSON.Reader) = Crazyhouse.Data(
      pockets = {
        val (white, black) = {
          r.str("p").flatMap(chess.Piece.fromChar)(breakOut): List[chess.Piece]
        }.partition(_ is chess.White)
        Pockets(
          white = Pocket(white.map(_.role)),
          black = Pocket(black.map(_.role))
        )
      },
      promoted = r.str("t").flatMap(chess.Pos.piotr)(breakOut)
    )

    def writes(w: BSON.Writer, o: Crazyhouse.Data) = BSONDocument(
      "p" -> {
        o.pockets.white.roles.map(_.forsythUpper).mkString +
          o.pockets.black.roles.map(_.forsyth).mkString
      },
      "t" -> o.promoted.map(_.piotr).mkString
    )
  }

  implicit val gameBSONHandler: BSON[Game] = new BSON[Game] {

    import Game.BSONFields._
    import PgnImport.pgnImportBSONHandler
    import Player.playerBSONHandler

    private val emptyPlayerBuilder = playerBSONHandler.read(BSONDocument())

    def reads(r: BSON.Reader): Game = {
      val winC = r boolO winnerColor map Color.apply
      val (whiteId, blackId) = r str playerIds splitAt 4
      val uids = ~r.getO[List[String]](playerUids)
      val (whiteUid, blackUid) = (uids.headOption.filter(_.nonEmpty), uids.lift(1).filter(_.nonEmpty))
      def player(field: String, color: Color, id: Player.Id, uid: Player.UserId): Player = {
        val builder = r.getO[Player.Builder](field)(playerBSONHandler) | emptyPlayerBuilder
        val win = winC map (_ == color)
        builder(color)(id)(uid)(win)
      }
      val gameVariant = Variant(r intD variant) | chess.variant.Standard
      val plies = r int turns

      val decoded = r.bytesO(huffmanPgn).map { PgnStorage.Huffman.decode(_, plies) } getOrElse PgnStorage.Decoded(
        pgnMoves = PgnStorage.OldBin.decode(r bytesD oldPgn, plies),
        pieces = BinaryFormat.piece.read(r bytes binaryPieces, gameVariant),
        positionHashes = r.getO[chess.PositionHash](positionHashes) | Array.empty,
        unmovedRooks = r.getO[UnmovedRooks](unmovedRooks) | UnmovedRooks.default,
        format = PgnStorage.OldBin
      )

      val g = Game(
        id = r str id,
        whitePlayer = player(whitePlayer, White, whiteId, whiteUid),
        blackPlayer = player(blackPlayer, Black, blackId, blackUid),
        pgnMoves = decoded.pgnMoves,
        pieces = decoded.pieces,
        positionHashes = decoded.positionHashes,
        unmovedRooks = decoded.unmovedRooks,
        pgnStorage = decoded.format,
        status = r.get[Status](status),
        turns = plies,
        startedAtTurn = r intD startedAtTurn,
        checkCount = {
          val counts = r.intsD(checkCount)
          CheckCount(~counts.headOption, ~counts.lastOption)
        },
        castleLastMoveTime = r.get[CastleLastMoveTime](castleLastMoveTime)(CastleLastMoveTime.castleLastMoveTimeBSONHandler),
        daysPerTurn = r intO daysPerTurn,
        binaryMoveTimes = r bytesO moveTimes,
        mode = Mode(r boolD rated),
        variant = gameVariant,
        next = r strO next,
        bookmarks = r intD bookmarks,
        createdAt = r date createdAt,
        movedAt = r.dateD(movedAt, r date createdAt),
        metadata = Metadata(
          source = r intO source flatMap Source.apply,
          pgnImport = r.getO[PgnImport](pgnImport)(PgnImport.pgnImportBSONHandler),
          tournamentId = r strO tournamentId,
          simulId = r strO simulId,
          tvAt = r dateO tvAt,
          analysed = r boolD analysed
        )
      )

      val gameClock = r.getO[Color => Clock](clock)(clockBSONReader(g.createdAt, g.whitePlayer.berserk, g.blackPlayer.berserk)) map (_(g.turnColor))

      g.copy(
        clock = gameClock,
        crazyData = (g.variant == Crazyhouse) option r.get[Crazyhouse.Data](crazyData),
        clockHistory = for {
          clk <- gameClock
          bw <- r bytesO whiteClockHistory
          bb <- r bytesO blackClockHistory
          history <- BinaryFormat.clockHistory.read(clk.limit, bw, bb, g.flagged, g.id)
        } yield history
      )
    }

    def writes(w: BSON.Writer, o: Game) = BSONDocument(
      id -> o.id,
      playerIds -> (o.whitePlayer.id + o.blackPlayer.id),
      playerUids -> w.strListO(List(~o.whitePlayer.userId, ~o.blackPlayer.userId)),
      whitePlayer -> w.docO(playerBSONHandler write ((_: Color) => (_: Player.Id) => (_: Player.UserId) => (_: Player.Win) => o.whitePlayer)),
      blackPlayer -> w.docO(playerBSONHandler write ((_: Color) => (_: Player.Id) => (_: Player.UserId) => (_: Player.Win) => o.blackPlayer)),
      status -> o.status,
      turns -> o.turns,
      startedAtTurn -> w.intO(o.startedAtTurn),
      clock -> (o.clock map { c => clockBSONWrite(o.createdAt, c) }),
      checkCount -> o.checkCount.nonEmpty.option(o.checkCount),
      castleLastMoveTime -> CastleLastMoveTime.castleLastMoveTimeBSONHandler.write(o.castleLastMoveTime),
      daysPerTurn -> o.daysPerTurn,
      moveTimes -> o.binaryMoveTimes,
      whiteClockHistory -> clockHistory(White, o.clockHistory, o.clock, o.flagged),
      blackClockHistory -> clockHistory(Black, o.clockHistory, o.clock, o.flagged),
      rated -> w.boolO(o.mode.rated),
      variant -> o.variant.exotic.option(o.variant.id).map(w.int),
      crazyData -> o.crazyData,
      next -> o.next,
      bookmarks -> w.intO(o.bookmarks),
      createdAt -> w.date(o.createdAt),
      movedAt -> w.date(o.movedAt),
      source -> o.metadata.source.map(_.id),
      pgnImport -> o.metadata.pgnImport,
      tournamentId -> o.metadata.tournamentId,
      simulId -> o.metadata.simulId,
      tvAt -> o.metadata.tvAt.map(w.date),
      analysed -> w.boolO(o.metadata.analysed)
    ) ++ {
        o.pgnStorage match {
          case f @ PgnStorage.OldBin => $doc(
            oldPgn -> f.encode(o.pgnMoves),
            binaryPieces -> BinaryFormat.piece.write(o.pieces),
            positionHashes -> o.positionHashes,
            unmovedRooks -> o.unmovedRooks
          )
          case f @ PgnStorage.Huffman => $doc(
            huffmanPgn -> f.encode(o.pgnMoves)
          )
        }
      }
  }

  private def clockHistory(color: Color, clockHistory: Option[ClockHistory], clock: Option[Clock], flagged: Option[Color]) =
    for {
      clk <- clock
      history <- clockHistory
      times = history(color)
    } yield BinaryFormat.clockHistory.writeSide(clk.limit, times, flagged has color)

  private[game] def clockBSONReader(since: DateTime, whiteBerserk: Boolean, blackBerserk: Boolean) = new BSONReader[BSONBinary, Color => Clock] {
    def read(bin: BSONBinary) = BinaryFormat.clock(since).read(
      ByteArrayBSONHandler read bin, whiteBerserk, blackBerserk
    )
  }

  private[game] def clockBSONWrite(since: DateTime, clock: Clock) = ByteArrayBSONHandler write {
    BinaryFormat clock since write clock
  }
}
