package lila.game

import shogi.format.{ FEN, Forsyth, Uci }
import shogi.variant.Variant
import shogi.{
  CheckCount,
  Color,
  Clock,
  Sente,
  Gote,
  Status,
  Mode,
  History => ShogiHistory,
  Game => ShogiGame
}
import org.joda.time.DateTime
import reactivemongo.api.bson._
import scala.util.{ Success, Try }

import lila.db.BSON
import lila.db.dsl._

object BSONHandlers {

  import lila.db.ByteArray.ByteArrayBSONHandler

  implicit val FENBSONHandler = stringAnyValHandler[FEN](_.value, FEN.apply)

  implicit private[game] val checkCountWriter = new BSONWriter[CheckCount] {
    def writeTry(cc: CheckCount) = Success(BSONArray(cc.sente, cc.gote))
  }

  implicit val StatusBSONHandler = tryHandler[Status](
    { case BSONInteger(v) => Status(v) toTry s"No such status: $v" },
    x => BSONInteger(x.id)
  )

  import Player.playerBSONHandler
  private val emptyPlayerBuilder = playerBSONHandler.read($empty)

  implicit val gameBSONHandler: BSON[Game] = new BSON[Game] {

    import Game.{ BSONFields => F }
    import PgnImport.pgnImportBSONHandler

    def reads(r: BSON.Reader): Game = {

      lila.mon.game.fetch.increment()

      val light         = lightGameBSONHandler.readsWithPlayerIds(r, r str F.playerIds)
      val startedAtTurn = r intD F.startedAtTurn
      val plies         = r int F.turns atMost Game.maxPlies // unlimited can cause StackOverflowError
      val turnColor     = Color.fromPly(plies)
      val createdAt     = r date F.createdAt

      val playedPlies = plies - startedAtTurn
      val gameVariant = Variant(r intD F.variant) | shogi.variant.Standard

      val decoded = {
        val pgnMoves = PgnStorage.OldBin.decode(r bytesD F.oldPgn, playedPlies)
        PgnStorage.Decoded(
          pgnMoves = pgnMoves,
          pieces = BinaryFormat.piece.read(r bytes F.binaryPieces, gameVariant),
          positionHashes = r.getO[shogi.PositionHash](F.positionHashes) | Array.empty,
          lastMove = r strO F.historyLastMove flatMap Uci.apply,
          checkCount = r.intsD(F.checkCount),
          hands = r strO F.crazyData map Forsyth.readHands
        )
      }
      val shogiGame = ShogiGame(
        situation = shogi.Situation(
          shogi.Board(
            pieces = decoded.pieces,
            history = ShogiHistory(
              lastMove = decoded.lastMove,
              positionHashes = decoded.positionHashes,
              checkCount = CheckCount(~decoded.checkCount.headOption, ~decoded.checkCount.lastOption)
            ),
            variant = gameVariant,
            crazyData = decoded.hands
          ),
          color = turnColor
        ),
        pgnMoves = decoded.pgnMoves,
        clock = r.getO[Color => Clock](F.clock) {
          clockBSONReader(createdAt, light.sentePlayer.berserk, light.gotePlayer.berserk)
        } map (_(turnColor)),
        turns = plies,
        startedAtTurn = startedAtTurn
      )

      val senteClockHistory = r bytesO F.senteClockHistory
      val goteClockHistory  = r bytesO F.goteClockHistory

      val perSente = r bytesD F.periodsSente
      val perGote  = r bytesD F.periodsGote

      val perEnt = BinaryFormat.periodEntries.read(perSente, perGote).getOrElse(PeriodEntries.default)

      Game(
        id = light.id,
        sentePlayer = light.sentePlayer,
        gotePlayer = light.gotePlayer,
        shogi = shogiGame,
        loadClockHistory = clk =>
          for {
            bs <- senteClockHistory
            bg <- goteClockHistory
            history <-
              BinaryFormat.clockHistory
                .read(clk.limit, bs, bg, perEnt, (light.status == Status.Outoftime).option(turnColor))
            _ = lila.mon.game.loadClockHistory.increment()
          } yield history,
        status = light.status,
        daysPerTurn = r intO F.daysPerTurn,
        binaryMoveTimes = r bytesO F.moveTimes,
        mode = Mode(r boolD F.rated),
        bookmarks = r intD F.bookmarks,
        createdAt = createdAt,
        movedAt = r.dateD(F.movedAt, createdAt),
        metadata = Metadata(
          source = r intO F.source flatMap Source.apply,
          pgnImport = r.getO[PgnImport](F.pgnImport)(PgnImport.pgnImportBSONHandler),
          tournamentId = r strO F.tournamentId,
          swissId = r strO F.swissId,
          simulId = r strO F.simulId,
          analysed = r boolD F.analysed
        )
      )
    }

    def writes(w: BSON.Writer, o: Game) =
      BSONDocument(
        F.id         -> o.id,
        F.playerIds  -> (o.sentePlayer.id + o.gotePlayer.id),
        F.playerUids -> w.strListO(List(~o.sentePlayer.userId, ~o.gotePlayer.userId)),
        F.sentePlayer -> w.docO(
          playerBSONHandler write ((_: Color) =>
            (_: Player.ID) => (_: Player.UserId) => (_: Player.Win) => o.sentePlayer
          )
        ),
        F.gotePlayer -> w.docO(
          playerBSONHandler write ((_: Color) =>
            (_: Player.ID) => (_: Player.UserId) => (_: Player.Win) => o.gotePlayer
          )
        ),
        F.status        -> o.status,
        F.turns         -> o.shogi.turns,
        F.startedAtTurn -> w.intO(o.shogi.startedAtTurn),
        F.clock -> (o.shogi.clock flatMap { c =>
          clockBSONWrite(o.createdAt, c).toOption
        }),
        F.daysPerTurn       -> o.daysPerTurn,
        F.moveTimes         -> o.binaryMoveTimes,
        F.senteClockHistory -> clockHistory(Sente, o.clockHistory, o.shogi.clock, o.flagged),
        F.goteClockHistory  -> clockHistory(Gote, o.clockHistory, o.shogi.clock, o.flagged),
        F.periodsSente      -> periodEntries(Sente, o.clockHistory),
        F.periodsGote       -> periodEntries(Gote, o.clockHistory),
        F.rated             -> w.boolO(o.mode.rated),
        F.variant           -> o.board.variant.exotic.option(w int o.board.variant.id),
        F.bookmarks         -> w.intO(o.bookmarks),
        F.createdAt         -> w.date(o.createdAt),
        F.movedAt           -> w.date(o.movedAt),
        F.source            -> o.metadata.source.map(_.id),
        F.pgnImport         -> o.metadata.pgnImport,
        F.tournamentId      -> o.metadata.tournamentId,
        F.swissId           -> o.metadata.swissId,
        F.simulId           -> o.metadata.simulId,
        F.analysed          -> w.boolO(o.metadata.analysed)
      ) ++ {
        // if (false) // one day perhaps
        //   $doc(F.huffmanPgn -> PgnStorage.Huffman.encode(o.pgnMoves take Game.maxPlies))
        // else {
        val f = PgnStorage.OldBin
        $doc(
          F.oldPgn          -> f.encode(o.pgnMoves take Game.maxPlies),
          F.binaryPieces    -> BinaryFormat.piece.write(o.board.pieces),
          F.positionHashes  -> o.history.positionHashes,
          F.historyLastMove -> o.history.lastMove.map(_.uci),
          F.checkCount      -> o.history.checkCount,
          F.crazyData       -> Forsyth.exportCrazyPocket(o.board)
        )
      }
  }

  implicit object lightGameBSONHandler extends lila.db.BSONReadOnly[LightGame] {

    import Game.{ BSONFields => F }
    import Player.playerBSONHandler

    def reads(r: BSON.Reader): LightGame = {
      lila.mon.game.fetchLight.increment()
      readsWithPlayerIds(r, "")
    }

    def readsWithPlayerIds(r: BSON.Reader, playerIds: String): LightGame = {
      val (senteId, goteId)   = playerIds splitAt 4
      val winC                = r boolO F.winnerColor map Color.apply
      val uids                = ~r.getO[List[lila.user.User.ID]](F.playerUids)
      val (senteUid, goteUid) = (uids.headOption.filter(_.nonEmpty), uids.lift(1).filter(_.nonEmpty))
      def makePlayer(field: String, color: Color, id: Player.ID, uid: Player.UserId): Player = {
        val builder = r.getO[Player.Builder](field)(playerBSONHandler) | emptyPlayerBuilder
        builder(color)(id)(uid)(winC map (_ == color))
      }
      LightGame(
        id = r str F.id,
        sentePlayer = makePlayer(F.sentePlayer, Sente, senteId, senteUid),
        gotePlayer = makePlayer(F.gotePlayer, Gote, goteId, goteUid),
        status = r.get[Status](F.status)
      )
    }
  }

  private def periodEntries(color: Color, clockHistory: Option[ClockHistory]) =
    for {
      history <- clockHistory
    } yield BinaryFormat.periodEntries.writeSide(history.periodEntries(color))

  private def clockHistory(
      color: Color,
      clockHistory: Option[ClockHistory],
      clock: Option[Clock],
      flagged: Option[Color]
  ) =
    for {
      clk     <- clock
      history <- clockHistory
      times = history(color)
    } yield BinaryFormat.clockHistory.writeSide(clk.limit, times, flagged has color)

  private[game] def clockBSONReader(since: DateTime, senteBerserk: Boolean, goteBerserk: Boolean) =
    new BSONReader[Color => Clock] {
      def readTry(bson: BSONValue): Try[Color => Clock] =
        bson match {
          case bin: BSONBinary =>
            ByteArrayBSONHandler readTry bin map { cl =>
              BinaryFormat.clock(since).read(cl, senteBerserk, goteBerserk)
            }
          case b => lila.db.BSON.handlerBadType(b)
        }
    }

  private[game] def clockBSONWrite(since: DateTime, clock: Clock) =
    ByteArrayBSONHandler writeTry {
      BinaryFormat clock since write clock
    }
}
