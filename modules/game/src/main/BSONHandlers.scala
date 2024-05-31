package lila.game

import shogi.format.forsyth.Sfen
import shogi.format.usi.Usi
import shogi.variant.Variant
import shogi.{
  Clock,
  Color,
  ConsecutiveAttacks,
  Game => ShogiGame,
  Gote,
  Hands,
  History => ShogiHistory,
  Mode,
  Pos,
  Sente,
  Status
}
import org.joda.time.DateTime
import reactivemongo.api.bson._
import scala.util.{ Success, Try }

import lila.db.BSON
import lila.db.dsl._

object BSONHandlers {

  import lila.db.ByteArray.ByteArrayBSONHandler

  implicit private[game] val consecutiveAttacksWriter = new BSONWriter[ConsecutiveAttacks] {
    def writeTry(ca: ConsecutiveAttacks) = Success(BSONArray(ca.sente, ca.gote))
  }

  implicit val StatusBSONHandler = quickHandler[Status](
    { case BSONInteger(v) => Status(v).getOrElse(Status.UnknownFinish) },
    x => BSONInteger(x.id)
  )

  import Player.playerBSONHandler
  private val emptyPlayerBuilder = playerBSONHandler.read($empty)

  implicit val gameBSONHandler: BSON[Game] = new BSON[Game] {

    import Game.{ BSONFields => F }
    import NotationImport.notationImportBSONHandler

    def reads(r: BSON.Reader): Game = {

      lila.mon.game.fetch.increment()

      val light = lightGameBSONHandler.readsWithPlayerIds(r, r str F.playerIds)

      val initialSfen    = r.getO[Sfen](F.initialSfen)
      val startedAtStep  = initialSfen.flatMap(_.stepNumber) | 1
      val startedAtColor = initialSfen.flatMap(_.color) | Sente
      val startedAtPly   = startedAtStep - (if ((startedAtStep % 2 == 1) == startedAtColor.sente) 1 else 0)

      val gameVariant = Variant(r intD F.variant) | shogi.variant.Standard

      val plies    = r int F.plies atMost Game.maxPlies(gameVariant) // unlimited can cause StackOverflowError
      val plyColor = Color.fromPly(plies)
      val clockColor = if (light.status == Status.Paused) !plyColor else plyColor
      val createdAt  = r date F.createdAt

      val periodEntries = BinaryFormat.periodEntries
        .read(
          r bytesD F.periodsSente,
          r bytesD F.periodsGote
        )
        .getOrElse(PeriodEntries.default)

      val usis   = BinaryFormat.usi.read(r bytesD F.usis, gameVariant)
      val pieces = BinaryFormat.pieces.read(usis, initialSfen, gameVariant)

      val positionHashes = r.getO[shogi.PositionHash](F.positionHashes) | Array.empty
      val hands          = r.strO(F.hands) flatMap { Sfen.makeHandsFromString(_, gameVariant) }

      val lastLionCapture = if (gameVariant.chushogi) r.strO(F.lastLionCapture).flatMap(Pos.fromKey) else None
      val counts          = r.intsD(F.consecutiveAttacks)

      val shogiGame = ShogiGame(
        situation = shogi.Situation(
          shogi.Board(pieces = pieces),
          hands = hands.getOrElse(Hands.empty),
          color = plyColor,
          history = ShogiHistory(
            lastUsi = usis.lastOption,
            lastLionCapture = lastLionCapture,
            consecutiveAttacks = ConsecutiveAttacks(~counts.headOption, ~counts.lastOption),
            positionHashes = positionHashes,
            initialSfen = initialSfen
          ),
          variant = gameVariant
        ),
        usis = usis,
        clock = r.getO[Color => Clock](F.clock) {
          clockBSONReader(createdAt, periodEntries, light.sentePlayer.berserk, light.gotePlayer.berserk)
        } map (_(clockColor)),
        plies = plies,
        startedAtPly = startedAtPly,
        startedAtStep = startedAtStep
      )

      val senteClockHistory = r bytesO F.senteClockHistory
      val goteClockHistory  = r bytesO F.goteClockHistory

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
                .read(clk.limit, bs, bg, periodEntries, (light.status == Status.Outoftime).option(plyColor))
            _ = lila.mon.game.loadClockHistory.increment()
          } yield history,
        status = light.status,
        daysPerTurn = r intO F.daysPerTurn,
        binaryMoveTimes = r bytesO F.moveTimes,
        sealedUsi = r.strO(F.sealedUsi).flatMap(Usi.apply),
        mode = Mode(r boolD F.rated),
        bookmarks = r intD F.bookmarks,
        pausedSeconds = r intO F.pausedSeconds,
        createdAt = createdAt,
        movedAt = r.dateD(F.movedAt, createdAt),
        metadata = Metadata(
          source = r intO F.source flatMap Source.apply,
          notationImport = r.getO[NotationImport](F.notationImport)(NotationImport.notationImportBSONHandler),
          tournamentId = r strO F.tournamentId,
          simulId = r strO F.simulId,
          postGameStudy = r strO F.postGameStudy,
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
        F.status -> o.status,
        F.plies  -> o.shogi.plies,
        F.clock -> (o.shogi.clock flatMap { c =>
          clockBSONWrite(o.createdAt, c).toOption
        }),
        F.daysPerTurn        -> o.daysPerTurn,
        F.moveTimes          -> o.binaryMoveTimes,
        F.sealedUsi          -> o.sealedUsi.map(_.usi),
        F.senteClockHistory  -> clockHistory(Sente, o.clockHistory, o.shogi.clock, o.flagged),
        F.goteClockHistory   -> clockHistory(Gote, o.clockHistory, o.shogi.clock, o.flagged),
        F.periodsSente       -> periodEntries(Sente, o.clockHistory),
        F.periodsGote        -> periodEntries(Gote, o.clockHistory),
        F.rated              -> w.boolO(o.mode.rated),
        F.initialSfen        -> o.initialSfen,
        F.variant            -> (!o.variant.standard).option(w int o.variant.id),
        F.bookmarks          -> w.intO(o.bookmarks),
        F.createdAt          -> w.date(o.createdAt),
        F.movedAt            -> w.date(o.movedAt),
        F.pausedSeconds      -> o.pausedSeconds,
        F.lastLionCapture    -> o.history.lastLionCapture.map(_.key),
        F.consecutiveAttacks -> o.history.consecutiveAttacks,
        F.source             -> o.metadata.source.map(_.id),
        F.notationImport     -> o.metadata.notationImport,
        F.tournamentId       -> o.metadata.tournamentId,
        F.simulId            -> o.metadata.simulId,
        F.analysed           -> w.boolO(o.metadata.analysed),
        F.positionHashes     -> o.history.positionHashes,
        F.hands              -> Sfen.handsToString(o.hands, o.variant),
        F.usis               -> BinaryFormat.usi.write(o.usis, o.variant)
      )
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
      val winC                = r boolO F.winnerColor map Color.fromSente
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

  private[game] def clockBSONReader(
      since: DateTime,
      periodEntries: PeriodEntries,
      senteBerserk: Boolean,
      goteBerserk: Boolean
  ) =
    new BSONReader[Color => Clock] {
      def readTry(bson: BSONValue): Try[Color => Clock] =
        bson match {
          case bin: BSONBinary =>
            ByteArrayBSONHandler readTry bin map { cl =>
              BinaryFormat.clock(since).read(cl, periodEntries, senteBerserk, goteBerserk)
            }
          case b => lila.db.BSON.handlerBadType(b)
        }
    }

  private[game] def clockBSONWrite(since: DateTime, clock: Clock) =
    ByteArrayBSONHandler writeTry {
      BinaryFormat clock since write clock
    }
}
