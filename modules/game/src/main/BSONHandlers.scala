package lidraughts.game

import org.joda.time.DateTime
import reactivemongo.bson._
import scala.collection.breakOut

import draughts.variant.Variant
import draughts.{ KingMoves, Color, Clock, White, Black, Status, Mode, DraughtsHistory, DraughtsGame }
import draughts.format.Uci

import lidraughts.db.BSON
import lidraughts.db.dsl._

object BSONHandlers {

  import lidraughts.db.ByteArray.ByteArrayBSONHandler

  private[game] implicit val kingMovesWriter = new BSONWriter[KingMoves, BSONArray] {
    def write(km: KingMoves) = BSONArray(km.white, km.black)
  }

  implicit val StatusBSONHandler = new BSONHandler[BSONInteger, Status] {
    def read(bsonInt: BSONInteger): Status = Status(bsonInt.value) err s"No such status: ${bsonInt.value}"
    def write(x: Status) = BSONInteger(x.id)
  }

  implicit val gameBSONHandler: BSON[Game] = new BSON[Game] {

    import Game.{ BSONFields => F }
    import PdnImport.pdnImportBSONHandler
    import Player.playerBSONHandler

    private val emptyPlayerBuilder = playerBSONHandler.read(BSONDocument())

    def reads(r: BSON.Reader): Game = {

      val gameVariant = Variant(r intD F.variant) | draughts.variant.Standard
      val startedAtTurn = r intD F.startedAtTurn
      var plies = r int F.turns atMost Game.maxPlies // unlimited can cause StackOverflowError
      val playedPlies = plies - startedAtTurn
      val turnColor = Color.fromPly(plies)

      val decoded = r.bytesO(F.huffmanPdn).map { PdnStorage.Huffman.decode(_, playedPlies) } | {
        val lm = r strD F.historyLastMove
        PdnStorage.Decoded(
          pdnMoves = PdnStorage.OldBin.decode(r bytesD F.oldPdn, playedPlies),
          pieces = BinaryFormat.piece.read(r bytes F.binaryPieces, gameVariant),
          positionHashes = r.getO[draughts.PositionHash](F.positionHashes) | Array.empty,
          lastMove = if (lm.nonEmpty) Uci.apply(lm) else None,
          format = PdnStorage.OldBin
        )
      }

      val decodedBoard = draughts.Board(
        pieces = decoded.pieces,
        history = DraughtsHistory(
          lastMove = decoded.lastMove,
          positionHashes = decoded.positionHashes,
          kingMoves = if (gameVariant.frisian) {
            val counts = r.intsD(F.kingMoves)
            KingMoves(~counts.headOption, ~counts.lastOption)
          } else Game.emptyKingMoves,
          variant = gameVariant
        ),
        variant = gameVariant
      )

      val initialSituation = draughts.Situation(
        board = decodedBoard,
        color = turnColor
      )

      val lastMove: String = decoded.pdnMoves.lastOption.getOrElse("")

      val decodedSituation = draughts.Pos.posAt(lastMove.substring(lastMove.lastIndexOf('x') + 1)) match {
        case Some(pos) =>
          if (initialSituation.ghosts != 0) {
            plies = plies - 1
            draughts.Situation(board = decodedBoard, color = !turnColor)
          } else initialSituation
        case _ => initialSituation
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

      val draughtsGame = DraughtsGame(
        situation = decodedSituation,
        pdnMoves = decoded.pdnMoves,
        clock = r.getO[Color => Clock](F.clock) {
          clockBSONReader(createdAt, wPlayer.berserk, bPlayer.berserk)
        } map (_(decodedSituation.color)),
        turns = plies,
        startedAtTurn = startedAtTurn
      )

      Game(
        id = r str F.id,
        whitePlayer = wPlayer,
        blackPlayer = bPlayer,
        draughts = draughtsGame,
        pdnStorage = decoded.format,
        status = status,
        daysPerTurn = r intO F.daysPerTurn,
        binaryMoveTimes = r bytesO F.moveTimes,
        clockHistory = for {
          clk <- draughtsGame.clock
          bw <- r bytesO F.whiteClockHistory
          bb <- r bytesO F.blackClockHistory
          history <- BinaryFormat.clockHistory.read(clk.limit, bw, bb, (status == Status.Outoftime).option(decodedSituation.color))
        } yield history,
        mode = Mode(r boolD F.rated),
        next = r strO F.next,
        bookmarks = r intD F.bookmarks,
        createdAt = createdAt,
        movedAt = r.dateD(F.movedAt, createdAt),
        metadata = Metadata(
          source = r intO F.source flatMap Source.apply,
          pdnImport = r.getO[PdnImport](F.pdnImport)(PdnImport.pdnImportBSONHandler),
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
      F.turns -> o.draughts.turns,
      F.startedAtTurn -> w.intO(o.draughts.startedAtTurn),
      F.clock -> (o.draughts.clock map { c => clockBSONWrite(o.createdAt, c) }),
      F.daysPerTurn -> o.daysPerTurn,
      F.moveTimes -> o.binaryMoveTimes,
      F.whiteClockHistory -> clockHistory(White, o.clockHistory, o.draughts.clock, o.flagged),
      F.blackClockHistory -> clockHistory(Black, o.clockHistory, o.draughts.clock, o.flagged),
      F.rated -> w.boolO(o.mode.rated),
      F.variant -> o.board.variant.exotic.option(w int o.board.variant.id),
      F.next -> o.next,
      F.bookmarks -> w.intO(o.bookmarks),
      F.createdAt -> w.date(o.createdAt),
      F.movedAt -> w.date(o.movedAt),
      F.source -> o.metadata.source.map(_.id),
      F.pdnImport -> o.metadata.pdnImport,
      F.tournamentId -> o.metadata.tournamentId,
      F.simulId -> o.metadata.simulId,
      F.tvAt -> o.metadata.tvAt.map(w.date),
      F.analysed -> w.boolO(o.metadata.analysed)
    ) ++ {
        o.pdnStorage match {
          case f @ PdnStorage.OldBin => $doc(
            F.oldPdn -> f.encode(o.pdnMoves take Game.maxPlies),
            F.binaryPieces -> BinaryFormat.piece.write(o.board.pieces),
            F.positionHashes -> o.history.positionHashes,
            F.historyLastMove -> o.history.lastMove.fold(none[String])(_.uci.some),
            // since variants are always OldBin
            F.kingMoves -> o.history.kingMoves.nonEmpty.option(o.history.kingMoves)
          )
          case f @ PdnStorage.Huffman => $doc(
            F.huffmanPdn -> f.encode(o.pdnMoves take Game.maxPlies)
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
