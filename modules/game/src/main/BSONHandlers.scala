package lila.game

import lila.db.{ BSON, ByteArray }
import org.joda.time.DateTime
import reactivemongo.bson._

import chess.variant.{ Variant, Crazyhouse }
import chess.{ CheckCount, Color, Clock, White, Black, Status, Mode, UnmovedRooks }

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
      val (white, black) = r.str("p").toList.flatMap(chess.Piece.fromChar).partition(_ is chess.White)
      Pockets(
        white = Pocket(white.map(_.role)),
        black = Pocket(black.map(_.role)))
    },
      promoted = r.str("t").toSet.flatMap(chess.Pos.piotr))

    def writes(w: BSON.Writer, o: Crazyhouse.Data) = BSONDocument(
      "p" -> {
        o.pockets.white.roles.map(_.forsythUpper).mkString +
          o.pockets.black.roles.map(_.forsyth).mkString
      },
      "t" -> o.promoted.map(_.piotr).mkString
    )
  }

  implicit val gameBSONHandler = new BSON[Game] {

    import Game.BSONFields._
    import PgnImport.pgnImportBSONHandler
    import Player.playerBSONHandler

    private val emptyPlayerBuilder = playerBSONHandler.read(BSONDocument())

    def reads(r: BSON.Reader): Game = {
      val nbTurns = r int turns
      val winC = r boolO winnerColor map Color.apply
      val (whiteId, blackId) = r str playerIds splitAt 4
      val uids = ~r.getO[List[String]](playerUids)
      val (whiteUid, blackUid) = (uids.headOption.filter(_.nonEmpty), uids.lift(1).filter(_.nonEmpty))
      def player(field: String, color: Color, id: Player.Id, uid: Player.UserId): Player = {
        val builder = r.getO[Player.Builder](field)(playerBSONHandler) | emptyPlayerBuilder
        val win = winC map (_ == color)
        builder(color)(id)(uid)(win)
      }
      val wPlayer = player(whitePlayer, White, whiteId, whiteUid)
      val bPlayer = player(blackPlayer, Black, blackId, blackUid)
      val createdAtValue = r date createdAt
      val realVariant = Variant(r intD variant) | chess.variant.Standard
      Game(
        id = r str id,
        whitePlayer = wPlayer,
        blackPlayer = bPlayer,
        binaryPieces = r bytes binaryPieces,
        binaryPgn = r bytesD binaryPgn,
        status = r.get[Status](status),
        turns = nbTurns,
        startedAtTurn = r intD startedAtTurn,
        clock = r.getO[Color => Clock](clock)(clockBSONReader(createdAtValue, wPlayer.berserk, bPlayer.berserk)) map (_(Color(0 == nbTurns % 2))),
        positionHashes = r.bytesD(positionHashes).value,
        checkCount = {
          val counts = r.intsD(checkCount)
          CheckCount(~counts.headOption, ~counts.lastOption)
        },
        castleLastMoveTime = r.get[CastleLastMoveTime](castleLastMoveTime)(CastleLastMoveTime.castleLastMoveTimeBSONHandler),
        unmovedRooks = r.getO[UnmovedRooks](unmovedRooks) | UnmovedRooks.default,
        daysPerTurn = r intO daysPerTurn,
        binaryMoveTimes = (r bytesO moveTimes) | ByteArray.empty,
        mode = Mode(r boolD rated),
        variant = realVariant,
        crazyData = (realVariant == Crazyhouse) option r.get[Crazyhouse.Data](crazyData),
        next = r strO next,
        bookmarks = r intD bookmarks,
        createdAt = createdAtValue,
        updatedAt = r dateO updatedAt,
        metadata = Metadata(
          source = r intO source flatMap Source.apply,
          pgnImport = r.getO[PgnImport](pgnImport)(PgnImport.pgnImportBSONHandler),
          tournamentId = r strO tournamentId,
          simulId = r strO simulId,
          tvAt = r dateO tvAt,
          analysed = r boolD analysed)
      )
    }

    def writes(w: BSON.Writer, o: Game) = BSONDocument(
      id -> o.id,
      playerIds -> (o.whitePlayer.id + o.blackPlayer.id),
      playerUids -> w.strListO(List(~o.whitePlayer.userId, ~o.blackPlayer.userId)),
      whitePlayer -> w.docO(playerBSONHandler write ((_: Color) => (_: Player.Id) => (_: Player.UserId) => (_: Player.Win) => o.whitePlayer)),
      blackPlayer -> w.docO(playerBSONHandler write ((_: Color) => (_: Player.Id) => (_: Player.UserId) => (_: Player.Win) => o.blackPlayer)),
      binaryPieces -> o.binaryPieces,
      binaryPgn -> w.byteArrayO(o.binaryPgn),
      status -> o.status,
      turns -> o.turns,
      startedAtTurn -> w.intO(o.startedAtTurn),
      clock -> (o.clock map { c => clockBSONWrite(o.createdAt, c) }),
      positionHashes -> w.bytesO(o.positionHashes),
      checkCount -> o.checkCount.nonEmpty.option(o.checkCount),
      castleLastMoveTime -> CastleLastMoveTime.castleLastMoveTimeBSONHandler.write(o.castleLastMoveTime),
      unmovedRooks -> o.unmovedRooks,
      daysPerTurn -> o.daysPerTurn,
      moveTimes -> (BinaryFormat.moveTime write o.moveTimes),
      rated -> w.boolO(o.mode.rated),
      variant -> o.variant.exotic.option(o.variant.id).map(w.int),
      crazyData -> o.crazyData,
      next -> o.next,
      bookmarks -> w.intO(o.bookmarks),
      createdAt -> w.date(o.createdAt),
      updatedAt -> o.updatedAt.map(w.date),
      source -> o.metadata.source.map(_.id),
      pgnImport -> o.metadata.pgnImport,
      tournamentId -> o.metadata.tournamentId,
      simulId -> o.metadata.simulId,
      tvAt -> o.metadata.tvAt.map(w.date),
      analysed -> w.boolO(o.metadata.analysed)
    )
  }

  private[game] def clockBSONReader(since: DateTime, whiteBerserk: Boolean, blackBerserk: Boolean) = new BSONReader[BSONBinary, Color => Clock] {
    def read(bin: BSONBinary) = BinaryFormat.clock(since).read(
      ByteArrayBSONHandler read bin, whiteBerserk, blackBerserk
    )
  }
  private[game] def clockBSONWrite(since: DateTime, clock: Clock) = ByteArrayBSONHandler write {
    BinaryFormat clock since write clock
  }
}
