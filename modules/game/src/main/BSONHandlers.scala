package lila.game

import chess.bitboard.Board as BBoard
import chess.format.Fen
import chess.variant.{ Crazyhouse, Variant }
import chess.{
  ByColor,
  CheckCount,
  Clock,
  Color,
  Game as ChessGame,
  HalfMoveClock,
  History as ChessHistory,
  Mode,
  Ply,
  Status,
  UnmovedRooks
}
import reactivemongo.api.bson.*
import scalalib.model.Days

import scala.util.{ Success, Try }

import lila.core.game.{
  ClockHistory,
  Game,
  GameDrawOffers,
  GameMetadata,
  GameRule,
  LightGame,
  LightPlayer,
  PgnImport,
  Source,
  emptyDrawOffers
}
import lila.db.BSON
import lila.db.dsl.{ *, given }

object BSONHandlers:

  import lila.db.ByteArray.byteArrayHandler
  import lila.game.Game.maxPlies

  private[game] given checkCountWriter: BSONWriter[CheckCount] with
    def writeTry(cc: CheckCount) = Success(BSONArray(cc.white, cc.black))

  given statusHandler: BSONHandler[Status] = tryHandler[Status](
    { case BSONInteger(v) => Status(v).toTry(s"No such status: $v") },
    x => BSONInteger(x.id)
  )

  private[game] given BSONHandler[UnmovedRooks] = tryHandler[UnmovedRooks](
    { case bin: BSONBinary => byteArrayHandler.readTry(bin).map(BinaryFormat.unmovedRooks.read) },
    x => byteArrayHandler.writeTry(BinaryFormat.unmovedRooks.write(x)).get
  )

  given BSONHandler[GameRule] = valueMapHandler[String, GameRule](GameRule.byKey)(_.toString)

  given sourceHandler: BSONHandler[Source] = valueMapHandler[Int, Source](Source.byId)(_.id)

  private[game] given crazyhouseDataHandler: BSON[Crazyhouse.Data] with
    import Crazyhouse.*
    def reads(r: BSON.Reader) =
      val (white, black) = r.str("p").view.flatMap(chess.Piece.fromChar).to(List).partition(_.is(chess.White))
      Crazyhouse.Data(
        pockets = ByColor(white, black).map(pieces => Pocket(pieces.map(_.role))),
        promoted = chess.bitboard.Bitboard(r.str("t").view.flatMap(chess.Square.fromChar(_)))
      )

    def writes(w: BSON.Writer, o: Crazyhouse.Data) =
      def roles = o.pockets.mapWithColor: (color, pocket) =>
        pocket.flatMap((role, nb) => List.fill(nb)(role.forsythBy(color))).mkString

      BSONDocument(
        "p" -> roles.reduce(_ + _),
        "t" -> o.promoted.map(_.asChar).mkString
      )

  private[game] given gameDrawOffersHandler: BSONHandler[GameDrawOffers] = tryHandler[GameDrawOffers](
    { case arr: BSONArray =>
      Success(arr.values.foldLeft(emptyDrawOffers) {
        case (offers, BSONInteger(p)) =>
          if p > 0 then offers.copy(white = offers.white.incl(Ply(p)))
          else offers.copy(black = offers.black.incl(Ply(-p)))
        case (offers, _) => offers
      })
    },
    offers =>
      BSONArray(
        (Ply.raw(offers.white) ++ Ply.raw(offers.black).map(-_)).view.map(BSONInteger.apply).toIndexedSeq
      )
  )

  given BSONDocumentHandler[PgnImport] = Macros.handler

  given gameHandler: BSON[Game] with
    import lila.game.Game.BSONFields as F

    def reads(r: BSON.Reader): Game =

      lila.mon.game.fetch.increment()

      val playerIds = r.str(F.playerIds)
      val light     = lightGameReader.reads(r)

      val startedAtPly = Ply(r.intD(F.startedAtTurn))
      val ply          = r.get[Ply](F.turns).atMost(maxPlies) // unlimited can cause StackOverflowError
      val turnColor    = ply.turn
      val createdAt    = r.date(F.createdAt)

      val playedPlies = ply - startedAtPly

      val whitePlayer = Player.from(light, Color.white, playerIds, r.getD[Bdoc](F.whitePlayer))
      val blackPlayer = Player.from(light, Color.black, playerIds, r.getD[Bdoc](F.blackPlayer))

      val decoded = r.bytesO(F.huffmanPgn) match
        case Some(huffPgn) => PgnStorage.Huffman.decode(huffPgn, playedPlies, light.id)
        case None =>
          val clm  = r.get[CastleLastMove](F.castleLastMove)
          val sans = PgnStorage.OldBin.decode(r.bytesD(F.oldPgn), playedPlies)
          val halfMoveClock = HalfMoveClock
            .from:
              sans.reverse
                .indexWhere(san => san.value.contains("x") || san.value.headOption.exists(_.isLower))
                .some
            .filter(HalfMoveClock.initial <= _)
          PgnStorage.Decoded(
            sans = sans,
            board = BBoard.fromMap(BinaryFormat.piece.read(r.bytes(F.binaryPieces), light.variant)),
            positionHashes =
              r.getO[Array[Byte]](F.positionHashes).map(chess.PositionHash.apply) | chess.PositionHash.empty,
            unmovedRooks = r.getO[UnmovedRooks](F.unmovedRooks) | UnmovedRooks.default,
            lastMove = clm.lastMove,
            castles = clm.castles,
            halfMoveClock = halfMoveClock
              .orElse(r.getO[Fen.Full](F.initialFen).flatMap { fen =>
                Fen.readHalfMoveClockAndFullMoveNumber(fen)._1
              })
              .getOrElse(playedPlies.into(HalfMoveClock))
          )

      val chessGame = ChessGame(
        situation = chess.Situation(
          chess.Board(
            board = decoded.board,
            history = ChessHistory(
              lastMove = decoded.lastMove,
              castles = decoded.castles,
              halfMoveClock = decoded.halfMoveClock,
              positionHashes = decoded.positionHashes,
              unmovedRooks = decoded.unmovedRooks,
              checkCount = if light.variant.threeCheck then
                val counts = r.intsD(F.checkCount)
                CheckCount(~counts.headOption, ~counts.lastOption)
              else emptyCheckCount
            ),
            variant = light.variant,
            crazyData = light.variant.crazyhouse.option(r.get[Crazyhouse.Data](F.crazyData))
          ),
          color = turnColor
        ),
        sans = decoded.sans,
        clock = r
          .getO[Color => Clock](F.clock)(using
            clockBSONReader(createdAt, whitePlayer.berserk, blackPlayer.berserk)
          )
          .map(_(turnColor)),
        ply = ply,
        startedAtPly = startedAtPly
      )

      val whiteClockHistory = r.bytesO(F.whiteClockHistory)
      val blackClockHistory = r.bytesO(F.blackClockHistory)

      Game(
        id = light.id,
        players = ByColor(whitePlayer, blackPlayer),
        chess = chessGame,
        loadClockHistory = clk =>
          for
            bw <- whiteClockHistory
            bb <- blackClockHistory
            history <-
              BinaryFormat.clockHistory
                .read(clk.limit, bw, bb, (light.status == Status.Outoftime).option(turnColor))
            _ = lila.mon.game.loadClockHistory.increment()
          yield history,
        status = light.status,
        daysPerTurn = r.getO[Days](F.daysPerTurn),
        binaryMoveTimes = r.getO[Array[Byte]](F.moveTimes),
        mode = Mode(r.boolD(F.rated)),
        bookmarks = r.intD(F.bookmarks),
        createdAt = createdAt,
        movedAt = r.dateD(F.movedAt, createdAt),
        metadata = GameMetadata(
          source = r.getO[Source](F.source),
          pgnImport = r.getO[PgnImport](F.pgnImport),
          tournamentId = r.getO[TourId](F.tournamentId),
          swissId = r.getO[SwissId](F.swissId),
          simulId = r.getO[SimulId](F.simulId),
          analysed = r.boolD(F.analysed),
          drawOffers = r.getD(F.drawOffers, emptyDrawOffers),
          rules = r.getD(F.rules, Set.empty)
        )
      )

    def writes(w: BSON.Writer, o: Game) =
      BSONDocument(
        F.id        -> o.id,
        F.playerIds -> o.players.reduce(_.id.value + _.id.value),
        F.playerUids -> o.players
          .map(_.userId)
          .toPair
          .match
            case (None, None)    => None
            case (Some(w), None) => Some(List(w.value))
            case (wo, Some(b))   => Some(List(wo.so(_.value), b.value))
        ,
        F.whitePlayer   -> w.docO(Player.playerWrite(o.whitePlayer)),
        F.blackPlayer   -> w.docO(Player.playerWrite(o.blackPlayer)),
        F.status        -> o.status,
        F.turns         -> o.chess.ply,
        F.startedAtTurn -> w.intO(o.chess.startedAtPly.value),
        F.clock -> o.chess.clock.flatMap { c =>
          clockBSONWrite(o.createdAt, c).toOption
        },
        F.daysPerTurn       -> o.daysPerTurn,
        F.moveTimes         -> o.binaryMoveTimes,
        F.whiteClockHistory -> clockHistory(Color.White, o.clockHistory, o.chess.clock, o.flagged),
        F.blackClockHistory -> clockHistory(Color.Black, o.clockHistory, o.chess.clock, o.flagged),
        F.rated             -> w.boolO(o.mode.rated),
        F.variant           -> o.board.variant.exotic.option(w(o.board.variant.id)),
        F.bookmarks         -> w.intO(o.bookmarks),
        F.createdAt         -> w.date(o.createdAt),
        F.movedAt           -> w.date(o.movedAt),
        F.source            -> o.metadata.source,
        F.pgnImport         -> o.metadata.pgnImport,
        F.tournamentId      -> o.metadata.tournamentId,
        F.swissId           -> o.metadata.swissId,
        F.simulId           -> o.metadata.simulId,
        F.analysed          -> w.boolO(o.metadata.analysed),
        F.rules             -> o.metadata.nonEmptyRules
      ) ++ {
        if o.variant.standard then
          $doc(F.huffmanPgn -> PgnStorage.Huffman.encode(o.sans.take(maxPlies.value)))
        else
          val f = PgnStorage.OldBin
          $doc(
            F.oldPgn         -> f.encode(o.sans.take(maxPlies.value)),
            F.binaryPieces   -> BinaryFormat.piece.write(o.board.pieces),
            F.positionHashes -> o.history.positionHashes.value,
            F.unmovedRooks   -> o.history.unmovedRooks,
            F.castleLastMove -> CastleLastMove(castles = o.history.castles, lastMove = o.history.lastMove),
            F.checkCount     -> o.history.checkCount.nonEmpty.option(o.history.checkCount),
            F.crazyData      -> o.board.crazyData
          )
      }
    val emptyCheckCount = CheckCount(0, 0)

  given lightGameReader: lila.db.BSONReadOnly[LightGame] with

    import lila.game.Game.BSONFields as F

    private val emptyPlayerBuilder = lila.game.LightPlayer.builderRead($empty)

    def reads(r: BSON.Reader): LightGame =
      val winC                 = r.boolO(F.winnerColor).map { Color.fromWhite(_) }
      val uids                 = ~r.getO[List[UserId]](F.playerUids)
      val (whiteUid, blackUid) = (uids.headOption.filter(_.value.nonEmpty), uids.lift(1))
      def makePlayer(field: String, color: Color, uid: Option[UserId]): LightPlayer =
        val builder =
          r.getO[lila.game.LightPlayer.Builder](field)(using
            lila.game.LightPlayer.lightPlayerReader
          ) | emptyPlayerBuilder
        builder(color)(uid)
      LightGame(
        id = r.get[GameId](F.id),
        whitePlayer = makePlayer(F.whitePlayer, Color.White, whiteUid),
        blackPlayer = makePlayer(F.blackPlayer, Color.Black, blackUid),
        status = r.get[Status](F.status),
        win = winC,
        variant = Variant.idOrDefault(r.getO[Variant.Id](F.variant))
      )

  private def clockHistory(
      color: Color,
      clockHistory: Option[ClockHistory],
      clock: Option[Clock],
      flagged: Option[Color]
  ) =
    for
      clk     <- clock
      history <- clockHistory
      times = history(color)
    yield BinaryFormat.clockHistory.writeSide(clk.limit, times, flagged.has(color))

  private[game] def clockBSONReader(since: Instant, whiteBerserk: Boolean, blackBerserk: Boolean) =
    new BSONReader[Color => Clock]:
      def readTry(bson: BSONValue): Try[Color => Clock] =
        bson match
          case bin: BSONBinary =>
            byteArrayHandler.readTry(bin).map { cl =>
              BinaryFormat.clock(since).read(cl, whiteBerserk, blackBerserk)
            }
          case b => lila.db.BSON.handlerBadType(b)

  private[game] def clockBSONWrite(since: Instant, clock: Clock) =
    byteArrayHandler.writeTry:
      BinaryFormat.clock(since).write(clock)
