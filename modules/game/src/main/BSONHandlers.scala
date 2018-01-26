package lila.game

import org.joda.time.DateTime
import reactivemongo.bson._
import scala.collection.breakOut

import chess.variant.{ Variant, Crazyhouse }
import chess.{ CheckCount, Color, Clock, White, Black, Status, Mode, UnmovedRooks, History => ChessHistory, Game => ChessGame }

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

    import Game.{ BSONFields => F }
    import PgnImport.pgnImportBSONHandler
    import Player.playerBSONHandler

    private val emptyPlayerBuilder = playerBSONHandler.read(BSONDocument())

    def reads(r: BSON.Reader): Game = {

      val gameVariant = Variant(r intD F.variant) | chess.variant.Standard
      val plies = r int F.turns
      val turnColor = Color.fromPly(plies)

      val decoded = r.bytesO(F.huffmanPgn).map { PgnStorage.Huffman.decode(_, plies) } | {
        val clm = r.get[CastleLastMove](F.castleLastMove)
        PgnStorage.Decoded(
          pgnMoves = PgnStorage.OldBin.decode(r bytesD F.oldPgn, plies),
          pieces = BinaryFormat.piece.read(r bytes F.binaryPieces, gameVariant),
          positionHashes = r.getO[chess.PositionHash](F.positionHashes) | Array.empty,
          unmovedRooks = r.getO[UnmovedRooks](F.unmovedRooks) | UnmovedRooks.default,
          lastMove = clm.lastMove,
          castles = clm.castles,
          format = PgnStorage.OldBin
        )
      }

      val winC = r boolO F.winnerColor map Color.apply
      val uids = ~r.getO[List[String]](F.playerUids)
      val (whiteUid, blackUid) = (uids.headOption.filter(_.nonEmpty), uids.lift(1).filter(_.nonEmpty))
      def makePlayer(field: String, color: Color, id: Player.Id, uid: Player.UserId): Player = {
        val builder = r.getO[Player.Builder](field)(playerBSONHandler) | emptyPlayerBuilder
        val win = winC map (_ == color)
        builder(color)(id)(uid)(win)
      }
      val (whiteId, blackId) = r str F.playerIds splitAt 4
      val wPlayer = makePlayer(F.whitePlayer, White, whiteId, whiteUid)
      val bPlayer = makePlayer(F.blackPlayer, Black, blackId, blackUid)

      val createdAt = r date F.createdAt
      val status = r.get[Status](F.status)

      val chessGame = ChessGame(
        situation = chess.Situation(
          chess.Board(
            pieces = decoded.pieces,
            history = ChessHistory(
              lastMove = decoded.lastMove,
              castles = decoded.castles,
              positionHashes = decoded.positionHashes,
              unmovedRooks = decoded.unmovedRooks,
              checkCount = if (gameVariant.threeCheck) {
                val counts = r.intsD(F.checkCount)
                CheckCount(~counts.headOption, ~counts.lastOption)
              } else Game.emptyCheckCount
            ),
            variant = gameVariant,
            crazyData = gameVariant.crazyhouse option r.get[Crazyhouse.Data](F.crazyData)
          ),
          color = turnColor
        ),
        pgnMoves = decoded.pgnMoves,
        clock = r.getO[Color => Clock](F.clock) {
          clockBSONReader(createdAt, wPlayer.berserk, bPlayer.berserk)
        } map (_(turnColor)),
        turns = plies,
        startedAtTurn = r intD F.startedAtTurn
      )

      Game(
        id = r str F.id,
        whitePlayer = wPlayer,
        blackPlayer = bPlayer,
        chess = chessGame,
        pgnStorage = decoded.format,
        status = status,
        daysPerTurn = r intO F.daysPerTurn,
        binaryMoveTimes = r bytesO F.moveTimes,
        clockHistory = for {
          clk <- chessGame.clock
          bw <- r bytesO F.whiteClockHistory
          bb <- r bytesO F.blackClockHistory
          history <- BinaryFormat.clockHistory.read(clk.limit, bw, bb, (status == Status.Outoftime).option(turnColor))
        } yield history,
        mode = Mode(r boolD F.rated),
        next = r strO F.next,
        bookmarks = r intD F.bookmarks,
        createdAt = createdAt,
        movedAt = r.dateD(F.movedAt, createdAt),
        metadata = Metadata(
          source = r intO F.source flatMap Source.apply,
          pgnImport = r.getO[PgnImport](F.pgnImport)(PgnImport.pgnImportBSONHandler),
          tournamentId = r strO F.tournamentId,
          simulId = r strO F.simulId,
          tvAt = r dateO F.tvAt,
          analysed = r boolD F.analysed
        )
      )
    }

    def writes(w: BSON.Writer, o: Game) = BSONDocument(
      F.id -> o.id,
      F.playerIds -> (o.whitePlayer.id + o.blackPlayer.id),
      F.playerUids -> w.strListO(List(~o.whitePlayer.userId, ~o.blackPlayer.userId)),
      F.whitePlayer -> w.docO(playerBSONHandler write ((_: Color) => (_: Player.Id) => (_: Player.UserId) => (_: Player.Win) => o.whitePlayer)),
      F.blackPlayer -> w.docO(playerBSONHandler write ((_: Color) => (_: Player.Id) => (_: Player.UserId) => (_: Player.Win) => o.blackPlayer)),
      F.status -> o.status,
      F.turns -> o.chess.turns,
      F.startedAtTurn -> w.intO(o.chess.startedAtTurn),
      F.clock -> (o.chess.clock map { c => clockBSONWrite(o.createdAt, c) }),
      F.daysPerTurn -> o.daysPerTurn,
      F.moveTimes -> o.binaryMoveTimes,
      F.whiteClockHistory -> clockHistory(White, o.clockHistory, o.chess.clock, o.flagged),
      F.blackClockHistory -> clockHistory(Black, o.clockHistory, o.chess.clock, o.flagged),
      F.rated -> w.boolO(o.mode.rated),
      F.variant -> o.board.variant.exotic.option(w int o.board.variant.id),
      F.next -> o.next,
      F.bookmarks -> w.intO(o.bookmarks),
      F.createdAt -> w.date(o.createdAt),
      F.movedAt -> w.date(o.movedAt),
      F.source -> o.metadata.source.map(_.id),
      F.pgnImport -> o.metadata.pgnImport,
      F.tournamentId -> o.metadata.tournamentId,
      F.simulId -> o.metadata.simulId,
      F.tvAt -> o.metadata.tvAt.map(w.date),
      F.analysed -> w.boolO(o.metadata.analysed)
    ) ++ {
        o.pgnStorage match {
          case f @ PgnStorage.OldBin => $doc(
            F.oldPgn -> f.encode(o.pgnMoves),
            F.binaryPieces -> BinaryFormat.piece.write(o.board.pieces),
            F.positionHashes -> o.history.positionHashes,
            F.unmovedRooks -> o.history.unmovedRooks,
            F.castleLastMove -> CastleLastMove.castleLastMoveBSONHandler.write(CastleLastMove(
              castles = o.history.castles,
              lastMove = o.history.lastMove
            )),
            // since variants are always OldBin
            F.checkCount -> o.history.checkCount.nonEmpty.option(o.history.checkCount),
            F.crazyData -> o.board.crazyData
          )
          case f @ PgnStorage.Huffman => $doc(
            F.huffmanPgn -> f.encode(o.pgnMoves)
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
